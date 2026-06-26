package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.dto.OrganizationInsightsResponseDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsAnalysisResponseDto;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.exceptions.OrganizationExceptionFactory;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationException;
import mlakir.aura.core.integrations.analysis.AnalysisIntegrationService;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.models.OrganizationInsightsEntity;
import mlakir.aura.core.models.ReviewAnalysisEntity;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.OrganizationInsightsRepository;
import mlakir.aura.core.repositories.OrganizationRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import mlakir.aura.exception.AuraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OrganizationInsightsServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationInsightsRepository organizationInsightsRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private AnalysisIntegrationService analysisIntegrationService;

    private OrganizationInsightsService organizationInsightsService;

    @BeforeEach
    void setUp() {
        organizationInsightsService = new OrganizationInsightsService(
                organizationRepository,
                organizationInsightsRepository,
                reviewRepository,
                analysisIntegrationService,
                new OrganizationExceptionFactory(),
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnCachedInsightWithoutCallingAnalysis() {
        OrganizationEntity organization = organization();
        OrganizationInsightsEntity cached = cachedInsight(organization);
        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(organization));
        when(organizationInsightsRepository.findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(1L))
                .thenReturn(java.util.Optional.of(cached));

        OrganizationInsightsResponseDto response = organizationInsightsService
                .getOrGenerateInsights(1L, false, 50, null, null);

        assertEquals(true, response.cached());
        assertEquals("ДВФУ", response.organizationName());
        verify(analysisIntegrationService, never()).generateInsights(any());
        verify(reviewRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldForceGenerationEvenWhenCacheExists() {
        OrganizationEntity organization = organization();
        OrganizationInsightsEntity cached = cachedInsight(organization);
        ReviewEntity review = analyzedReview(organization, "Очень хороший отзыв");
        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(organization));
        when(organizationInsightsRepository.findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(1L))
                .thenReturn(java.util.Optional.of(cached));
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(analysisIntegrationService.generateInsights(any())).thenReturn(analysisResponse());
        when(organizationInsightsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationInsightsResponseDto response = organizationInsightsService
                .getOrGenerateInsights(1L, true, 50, null, null);

        assertEquals(false, response.cached());
        verify(analysisIntegrationService).generateInsights(any());
        verify(organizationInsightsRepository).save(any());
    }

    @Test
    void shouldApplyMaxLimitOfOneHundred() {
        OrganizationEntity organization = organization();
        ReviewEntity review = analyzedReview(organization, "Очень хороший отзыв");
        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(organization));
        when(organizationInsightsRepository.findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(1L))
                .thenReturn(java.util.Optional.empty());
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(analysisIntegrationService.generateInsights(any())).thenReturn(analysisResponse());
        when(organizationInsightsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        organizationInsightsService.getOrGenerateInsights(1L, false, 500, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void shouldTruncateReviewTextsToOneThousandFiveHundredCharacters() {
        OrganizationEntity organization = organization();
        String longText = "x".repeat(1600);
        ReviewEntity review = analyzedReview(organization, longText);
        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(organization));
        when(organizationInsightsRepository.findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(1L))
                .thenReturn(java.util.Optional.empty());
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(analysisIntegrationService.generateInsights(any())).thenReturn(analysisResponse());
        when(organizationInsightsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        organizationInsightsService.getOrGenerateInsights(1L, false, 50, null, null);

        ArgumentCaptor<mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsRequestDto> captor =
                ArgumentCaptor.forClass(mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsRequestDto.class);
        verify(analysisIntegrationService).generateInsights(captor.capture());
        assertEquals(1500, captor.getValue().reviews().getFirst().text().length());
    }

    @Test
    void shouldUseOnlyAnalyzedReviewsFromRepositorySelection() {
        OrganizationEntity organization = organization();
        ReviewEntity review = analyzedReview(organization, "Только analyzed review");
        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(organization));
        when(organizationInsightsRepository.findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(1L))
                .thenReturn(java.util.Optional.empty());
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(analysisIntegrationService.generateInsights(any())).thenReturn(analysisResponse());
        when(organizationInsightsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        organizationInsightsService.getOrGenerateInsights(1L, false, 50, null, null);

        verify(reviewRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldThrowMeaningfulErrorWhenNoAnalyzedReviewsExist() {
        OrganizationEntity organization = organization();
        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(organization));
        when(organizationInsightsRepository.findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(1L))
                .thenReturn(java.util.Optional.empty());
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        AuraException exception = assertThrows(
                AuraException.class,
                () -> organizationInsightsService.getOrGenerateInsights(1L, false, 50, null, null)
        );

        assertEquals("Not enough analyzed reviews for insights", exception.getBody().getTitle());
        verify(analysisIntegrationService, never()).generateInsights(any());
    }

    @Test
    void shouldKeepOldCacheWhenForceGenerationFails() {
        OrganizationEntity organization = organization();
        OrganizationInsightsEntity cached = cachedInsight(organization);
        ReviewEntity review = analyzedReview(organization, "Очень хороший отзыв");
        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(organization));
        when(organizationInsightsRepository.findTopByOrganizationIdOrderByGeneratedAtDescIdDesc(1L))
                .thenReturn(java.util.Optional.of(cached));
        when(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(analysisIntegrationService.generateInsights(any()))
                .thenThrow(new AnalysisIntegrationException("analysis-service is unavailable"));

        AuraException exception = assertThrows(
                AuraException.class,
                () -> organizationInsightsService.getOrGenerateInsights(1L, true, 50, null, null)
        );

        assertEquals("Insights generation unavailable", exception.getBody().getTitle());
        assertEquals("Старый summary", cached.getSummary());
        verify(organizationInsightsRepository, never()).save(any());
    }

    private OrganizationEntity organization() {
        OrganizationEntity entity = new OrganizationEntity();
        entity.setId(1L);
        entity.setName("Дальневосточный федеральный университет");
        entity.setShortName("ДВФУ");
        entity.setIsActive(true);
        return entity;
    }

    private ReviewEntity analyzedReview(OrganizationEntity organization, String text) {
        SourceEntity source = new SourceEntity();
        source.setId(10L);
        source.setOrganization(organization);
        source.setName("Tabiturient");

        ReviewEntity review = new ReviewEntity();
        review.setId(100L);
        review.setSource(source);
        review.setText(text);
        review.setPublishedAt(OffsetDateTime.parse("2026-04-28T10:00:00Z"));
        review.setStatus(ReviewStatus.ANALYZED);

        ReviewAnalysisEntity analysis = new ReviewAnalysisEntity();
        analysis.setReview(review);
        analysis.setSentiment(SentimentType.POSITIVE);
        analysis.setTopic(ReviewTopic.TEACHERS);
        analysis.setModelVersion("gemini-1.5-flash");
        review.setAnalysis(analysis);
        return review;
    }

    private OrganizationInsightsEntity cachedInsight(OrganizationEntity organization) {
        OrganizationInsightsEntity entity = new OrganizationInsightsEntity();
        entity.setId(1L);
        entity.setOrganization(organization);
        entity.setSummary("Старый summary");
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            entity.setStrengths(objectMapper.writeValueAsString(List.of("Сильные преподаватели")));
            entity.setWeaknesses(objectMapper.writeValueAsString(List.of("Проблемы с общежитием")));
            entity.setRecommendations(objectMapper.writeValueAsString(List.of("Улучшить заселение")));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to prepare cached organization insights test data", exception);
        }
        entity.setReviewsUsed(50);
        entity.setModelVersion("gemini-1.5-flash");
        entity.setGeneratedAt(OffsetDateTime.parse("2026-04-28T12:00:00Z"));
        return entity;
    }

    private OrganizationInsightsAnalysisResponseDto analysisResponse() {
        return new OrganizationInsightsAnalysisResponseDto(
                "Новый summary",
                List.of("Сильные преподаватели"),
                List.of("Проблемы с общежитием"),
                List.of("Улучшить заселение"),
                "gemini-1.5-flash"
        );
    }
}
