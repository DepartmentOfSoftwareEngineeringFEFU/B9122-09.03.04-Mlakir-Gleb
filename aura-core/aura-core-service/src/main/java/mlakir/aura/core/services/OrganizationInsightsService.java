package mlakir.aura.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.dto.OrganizationInsightsResponseDto;
import mlakir.aura.core.dto.OrganizationInsightsReviewItemDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsAnalysisResponseDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsRequestDto;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.exceptions.OrganizationExceptionFactory;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationException;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationService;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.models.OrganizationInsightsEntity;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.OrganizationInsightsRepository;
import mlakir.aura.core.repositories.OrganizationRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import mlakir.aura.core.specifications.ReviewSpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationInsightsService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_REVIEW_TEXT_LENGTH = 1500;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final OrganizationRepository organizationRepository;
    private final OrganizationInsightsRepository organizationInsightsRepository;
    private final ReviewRepository reviewRepository;
    private final AnalysisIntegrationService analysisIntegrationService;
    private final OrganizationExceptionFactory organizationExceptionFactory;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrganizationInsightsResponseDto getOrGenerateInsights(Long organizationId,
                                                                 boolean force,
                                                                 Integer limit,
                                                                 String from,
                                                                 String to) {
        OrganizationEntity organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> organizationExceptionFactory.organizationNotFound(organizationId));

        var cachedInsight = organizationInsightsRepository
                .findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(organizationId);
        if (!force && cachedInsight.isPresent()) {
            return toResponse(cachedInsight.get(), true);
        }

        OffsetDateTime fromBoundary = parseFrom(from);
        OffsetDateTime toBoundary = parseToExclusive(to);
        validateRange(fromBoundary, toBoundary);

        int normalizedLimit = normalizeLimit(limit);
        Specification<ReviewEntity> specification = Specification
                .where(ReviewSpecifications.withFetches())
                .and(ReviewSpecifications.orderedByPublishedAtDesc())
                .and(ReviewSpecifications.organizationIdEquals(organizationId))
                .and(ReviewSpecifications.statusEquals(ReviewStatus.ANALYZED))
                .and(ReviewSpecifications.publishedAtFrom(fromBoundary))
                .and(ReviewSpecifications.publishedAtToExclusive(toBoundary));
        List<ReviewEntity> reviews = reviewRepository.findAll(specification, PageRequest.of(0, normalizedLimit))
                .getContent();

        if (reviews.isEmpty()) {
            throw organizationExceptionFactory.insufficientAnalyzedReviewsForInsights(organizationId);
        }

        OrganizationInsightsRequestDto request = new OrganizationInsightsRequestDto(
                organization.getName(),
                reviews.stream()
                        .map(this::toReviewItem)
                        .toList()
        );

        try {
            OrganizationInsightsAnalysisResponseDto response = analysisIntegrationService.generateInsights(request);
            OrganizationInsightsEntity entity = new OrganizationInsightsEntity();
            entity.setOrganization(organization);
            entity.setSummary(response.summary());
            entity.setStrengths(writeList(response.strengths()));
            entity.setWeaknesses(writeList(response.weaknesses()));
            entity.setRecommendations(writeList(response.recommendations()));
            entity.setReviewsUsed(reviews.size());
            entity.setModelVersion(response.modelVersion());
            entity.setGeneratedAt(OffsetDateTime.now());
            OrganizationInsightsEntity saved = organizationInsightsRepository.save(entity);
            return toResponse(saved, false);
        } catch (AnalysisIntegrationException exception) {
            log.warn("Organization insights generation failed: organizationId={}, force={}, reviewsUsed={}, message={}",
                    organizationId, force, reviews.size(), exception.getMessage());
            throw organizationExceptionFactory.insightsGenerationUnavailable();
        }
    }

    private OrganizationInsightsReviewItemDto toReviewItem(ReviewEntity review) {
        return new OrganizationInsightsReviewItemDto(
                truncateText(review.getText()),
                review.getAnalysis().getSentiment().name(),
                review.getAnalysis().getTopic().name()
        );
    }

    private String truncateText(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.length() <= MAX_REVIEW_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_REVIEW_TEXT_LENGTH);
    }

    private OrganizationInsightsResponseDto toResponse(OrganizationInsightsEntity entity, boolean cached) {
        return new OrganizationInsightsResponseDto(
                entity.getOrganization().getId(),
                entity.getOrganization().getShortName(),
                entity.getSummary(),
                readList(entity.getStrengths()),
                readList(entity.getWeaknesses()),
                readList(entity.getRecommendations()),
                entity.getGeneratedAt(),
                entity.getModelVersion(),
                cached,
                entity.getReviewsUsed()
        );
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize organization insights list", exception);
        }
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize organization insights list", exception);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private OffsetDateTime parseFrom(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(raw).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignoredToo) {
                try {
                    return LocalDate.parse(raw).atStartOfDay().atOffset(ZoneOffset.UTC);
                } catch (DateTimeParseException exception) {
                    throw organizationExceptionFactory.invalidInsightsDate("from", raw);
                }
            }
        }
    }

    private OffsetDateTime parseToExclusive(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(raw).atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException ignoredToo) {
                try {
                    return LocalDate.parse(raw).plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                } catch (DateTimeParseException exception) {
                    throw organizationExceptionFactory.invalidInsightsDate("to", raw);
                }
            }
        }
    }

    private void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw organizationExceptionFactory.invalidInsightsDateRange();
        }
    }
}
