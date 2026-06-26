package mlakir.aura.core.services;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.dto.ManualImportResponseDto;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.ManualImportExceptionFactory;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.core.mappers.*;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.ReviewRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualImportService {

    private final SourceService sourceService;
    private final SourceExceptionFactory sourceExceptionFactory;
    private final ManualImportExceptionFactory manualImportExceptionFactory;
    private final CsvReviewImportParser csvReviewImportParser;
    private final ReviewRepository reviewRepository;
    private final ReviewCandidateMapper reviewCandidateMapper;
    private final ReviewBatchAnalysisService reviewBatchAnalysisService;

    @Transactional
    public ManualImportResponseDto importCsv(Long sourceId, MultipartFile file) {
        validateFile(file);

        SourceEntity source = sourceService.getSourceOrThrow(sourceId);
        validateSource(source);

        CsvReviewImportParseResult parseResult = parseFile(file);
        List<ReviewEntity> importedReviews = new ArrayList<>();
        int duplicateCount = 0;

        for (ParsedCsvReviewRow row : parseResult.rows()) {
            if (reviewRepository.existsBySourceIdAndExternalId(sourceId, row.externalId())) {
                duplicateCount++;
                continue;
            }

            ReviewCandidate candidate = new ReviewCandidate(
                    row.externalId(),
                    row.text(),
                    row.authorName(),
                    row.rating(),
                    row.publishedAt(),
                    row.originalUrl()
            );

            try {
                importedReviews.add(reviewRepository.save(reviewCandidateMapper.toEntity(source, candidate)));
            } catch (DataIntegrityViolationException exception) {
                duplicateCount++;
            }
        }

        if (!importedReviews.isEmpty()) {
            try {
                reviewBatchAnalysisService.analyzeReviews(importedReviews);
            } catch (Exception exception) {
                log.warn("CSV import analysis failed for sourceId={}: {}", sourceId, exception.getMessage());
                reviewBatchAnalysisService.markFailedAnalysis(importedReviews, exception.getMessage());
            }
        }

        return new ManualImportResponseDto(
                sourceId,
                file.getOriginalFilename(),
                parseResult.totalRows(),
                importedReviews.size(),
                duplicateCount,
                parseResult.invalidCount()
        );
    }

    private CsvReviewImportParseResult parseFile(MultipartFile file) {
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return csvReviewImportParser.parse(reader);
        } catch (Exception exception) {
            if (exception instanceof mlakir.aura.exception.AuraException auraException) {
                throw auraException;
            }
            throw manualImportExceptionFactory.invalidCsvFormat("Failed to read CSV file: " + exception.getMessage());
        }
    }

    private void validateSource(SourceEntity source) {
        if (!Boolean.TRUE.equals(source.getIsActive())) {
            throw sourceExceptionFactory.sourceInactive(source.getId());
        }
        if (source.getType() != SourceType.MANUAL_IMPORT) {
            throw sourceExceptionFactory.unsupportedSourceType(source.getType());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null) {
            throw manualImportExceptionFactory.fileMissing();
        }
        if (file.isEmpty()) {
            throw manualImportExceptionFactory.emptyFile();
        }
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        boolean csvName = fileName != null && fileName.toLowerCase().endsWith(".csv");
        boolean csvType = contentType != null && (contentType.contains("csv") || contentType.startsWith("text/"));
        if (!csvName && !csvType) {
            throw manualImportExceptionFactory.invalidFileType(fileName);
        }
    }
}
