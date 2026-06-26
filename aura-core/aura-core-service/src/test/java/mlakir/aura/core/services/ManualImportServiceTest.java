package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.dto.ManualImportResponseDto;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.ManualImportExceptionFactory;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.core.mappers.ReviewCandidateMapper;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.ReviewRepository;
import mlakir.aura.exception.AuraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ManualImportServiceTest {

    @Mock
    private SourceService sourceService;
    @Mock
    private SourceExceptionFactory sourceExceptionFactory;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewCandidateMapper reviewCandidateMapper;
    @Mock
    private ReviewBatchAnalysisService reviewBatchAnalysisService;

    private ManualImportService manualImportService;

    @BeforeEach
    void setUp() {
        manualImportService = new ManualImportService(
                sourceService,
                sourceExceptionFactory,
                new ManualImportExceptionFactory(),
                new CsvReviewImportParser(new ManualImportExceptionFactory()),
                reviewRepository,
                reviewCandidateMapper,
                reviewBatchAnalysisService
        );
    }

    @Test
    void shouldImportCsvAndSkipDuplicatesAndInvalidRows() {
        SourceEntity source = source();
        String csv = """
                externalId,text,authorName,publishedAt,originalUrl,rating
                1,"Great teachers",Ivan,2026-04-01T12:00:00Z,,5
                1,"Duplicate",Ivan,2026-04-01T12:00:00Z,,5
                2," ",Maria,2026-04-02T12:00:00Z,,4
                3,"Average",Alex,2026-04-03T12:00:00Z,,3
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "reviews.csv", "text/csv", csv.getBytes()
        );

        ReviewEntity firstReview = review("1");
        ReviewEntity secondReview = review("3");

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(reviewRepository.existsBySourceIdAndExternalId(1L, "1")).thenReturn(false, true);
        when(reviewRepository.existsBySourceIdAndExternalId(1L, "3")).thenReturn(false);
        when(reviewCandidateMapper.toEntity(any(SourceEntity.class), any(ReviewCandidate.class)))
                .thenReturn(firstReview, secondReview);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(firstReview, secondReview);

        ManualImportResponseDto response = manualImportService.importCsv(1L, file);

        assertEquals(1L, response.sourceId());
        assertEquals("reviews.csv", response.fileName());
        assertEquals(4, response.totalRows());
        assertEquals(2, response.importedCount());
        assertEquals(1, response.duplicateCount());
        assertEquals(1, response.invalidCount());
        verify(reviewBatchAnalysisService).analyzeReviews(List.of(firstReview, secondReview));
    }

    @Test
    void shouldMarkImportedReviewsFailedWhenAnalysisFails() {
        SourceEntity source = source();
        String csv = """
                externalId,text,authorName,publishedAt,originalUrl,rating
                1,"Great teachers",Ivan,2026-04-01T12:00:00Z,,5
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "reviews.csv", "text/csv", csv.getBytes()
        );
        ReviewEntity savedReview = review("1");

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(reviewRepository.existsBySourceIdAndExternalId(1L, "1")).thenReturn(false);
        when(reviewCandidateMapper.toEntity(any(SourceEntity.class), any(ReviewCandidate.class))).thenReturn(savedReview);
        when(reviewRepository.save(savedReview)).thenReturn(savedReview);
        org.mockito.Mockito.doThrow(new RuntimeException("analysis down"))
                .when(reviewBatchAnalysisService).analyzeReviews(List.of(savedReview));

        manualImportService.importCsv(1L, file);

        verify(reviewBatchAnalysisService).markFailedAnalysis(List.of(savedReview), "analysis down");
    }

    @Test
    void shouldFailWhenSourceTypeIsWrong() {
        SourceEntity source = source();
        source.setType(null);
        MockMultipartFile file = new MockMultipartFile(
                "file", "reviews.csv", "text/csv", "externalId,text,publishedAt\n1,test,2026-04-01T12:00:00Z".getBytes()
        );
        AuraException exception = new SourceExceptionFactory().unsupportedSourceType(null);

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(sourceExceptionFactory.unsupportedSourceType(null)).thenReturn(exception);

        assertThrows(AuraException.class, () -> manualImportService.importCsv(1L, file));
    }

    @Test
    void shouldFailWhenFileIsMissing() {
        assertThrows(AuraException.class, () -> manualImportService.importCsv(1L, null));
        verify(sourceService, never()).getSourceOrThrow(any(Long.class));
    }

    private SourceEntity source() {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(10L);
        organization.setName("DVFU");
        organization.setShortName("DVFU");
        organization.setIsActive(true);

        SourceEntity source = new SourceEntity();
        source.setId(1L);
        source.setOrganization(organization);
        source.setName("Manual");
        source.setType(SourceType.MANUAL_IMPORT);
        source.setCollectionMode(CollectionMode.MANUAL);
        source.setBaseUrl("https://example.com/manual");
        source.setIsActive(true);
        return source;
    }

    private ReviewEntity review(String externalId) {
        ReviewEntity review = new ReviewEntity();
        review.setExternalId(externalId);
        review.setPublishedAt(OffsetDateTime.now());
        review.setCollectedAt(OffsetDateTime.now());
        review.setStatus(ReviewStatus.ANALYSIS_PENDING);
        return review;
    }
}
