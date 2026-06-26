package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import mlakir.aura.core.dto.CollectionJobResponseDto;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.ReviewStatus;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.CollectionExceptionFactory;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.core.mappers.CollectionJobMapper;
import mlakir.aura.core.mappers.ReviewCandidateMapper;
import mlakir.aura.core.models.CollectionJobEntity;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.CollectionJobRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import mlakir.aura.core.security.CurrentUserProvider;
import mlakir.aura.exception.AuraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private SourceService sourceService;
    @Mock
    private SourceExceptionFactory sourceExceptionFactory;
    @Mock
    private CollectionJobRepository collectionJobRepository;
    @Mock
    private CollectionJobMapper collectionJobMapper;
    @Mock
    private CollectionExceptionFactory collectionExceptionFactory;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private SourceCollectorRegistry sourceCollectorRegistry;
    @Mock
    private SourceCollector sourceCollector;
    @Mock
    private ReviewCandidateMapper reviewCandidateMapper;
    @Mock
    private ReviewBatchAnalysisService reviewBatchAnalysisService;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private SourceCollectionStateService sourceCollectionStateService;

    private CollectionService collectionService;
    private CollectionJobLifecycleService collectionJobLifecycleService;

    @BeforeEach
    void setUp() {
        collectionJobLifecycleService = new CollectionJobLifecycleService(collectionJobRepository);
        collectionService = new CollectionService(
                sourceService,
                sourceExceptionFactory,
                collectionJobRepository,
                collectionJobMapper,
                collectionExceptionFactory,
                collectionJobLifecycleService,
                reviewRepository,
                sourceCollectorRegistry,
                reviewCandidateMapper,
                reviewBatchAnalysisService,
                currentUserProvider,
                sourceCollectionStateService
        );
    }

    @Test
    void shouldCompleteCollectionUsingBatchAnalysis() {
        SourceEntity source = source();
        ReviewCandidate collectedReview = new ReviewCandidate(
                "manual:1:10",
                "Очень хорошие преподаватели",
                "Иван",
                null,
                OffsetDateTime.now(),
                "https://example.com/manual/10"
        );
        ReviewEntity savedReview = new ReviewEntity();
        savedReview.setId(10L);
        savedReview.setSource(source);
        savedReview.setExternalId(collectedReview.externalId());
        savedReview.setText(collectedReview.text());
        savedReview.setStatus(ReviewStatus.ANALYSIS_PENDING);

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(currentUserProvider.getCurrentUsername()).thenReturn("manual-user");
        when(sourceCollectorRegistry.getCollector(SourceType.MANUAL_IMPORT)).thenReturn(sourceCollector);
        when(sourceCollector.collect(source)).thenReturn(List.of(collectedReview));
        when(reviewRepository.existsBySourceIdAndExternalId(source.getId(), collectedReview.externalId())).thenReturn(false);
        when(reviewCandidateMapper.toEntity(source, collectedReview)).thenReturn(savedReview);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(savedReview);
        org.mockito.Mockito.doAnswer(invocation -> {
            savedReview.setStatus(ReviewStatus.ANALYZED);
            return null;
        }).when(reviewBatchAnalysisService).analyzeReviews(List.of(savedReview));
        stubJobMapping(source);

        CollectionJobResponseDto response = collectionService.run(1L);

        assertEquals(CollectionJobStatus.SUCCESS, response.status());
        assertEquals(1, response.collectedCount());
        assertNull(response.errorMessage());
        assertEquals(ReviewStatus.ANALYZED, savedReview.getStatus());
    }

    @Test
    void shouldSkipDuplicatesAndCountOnlyNewReviews() {
        SourceEntity source = source();
        ReviewCandidate duplicate = new ReviewCandidate(
                "manual:1:10",
                "Уже есть",
                "Иван",
                null,
                OffsetDateTime.now(),
                "https://example.com/manual/10"
        );
        ReviewCandidate fresh = new ReviewCandidate(
                "manual:1:11",
                "Новый комментарий",
                "Петр",
                null,
                OffsetDateTime.now(),
                "https://example.com/manual/11"
        );
        ReviewEntity savedReview = new ReviewEntity();
        savedReview.setId(11L);
        savedReview.setSource(source);
        savedReview.setExternalId(fresh.externalId());
        savedReview.setText(fresh.text());
        savedReview.setStatus(ReviewStatus.ANALYSIS_PENDING);

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(currentUserProvider.getCurrentUsername()).thenReturn("manual-user");
        when(sourceCollectorRegistry.getCollector(SourceType.MANUAL_IMPORT)).thenReturn(sourceCollector);
        when(sourceCollector.collect(source)).thenReturn(List.of(duplicate, fresh));
        when(reviewRepository.existsBySourceIdAndExternalId(source.getId(), duplicate.externalId())).thenReturn(true);
        when(reviewRepository.existsBySourceIdAndExternalId(source.getId(), fresh.externalId())).thenReturn(false);
        when(reviewCandidateMapper.toEntity(source, fresh)).thenReturn(savedReview);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(savedReview);
        stubJobMapping(source);

        CollectionJobResponseDto response = collectionService.run(1L);

        assertEquals(CollectionJobStatus.SUCCESS, response.status());
        assertEquals(1, response.collectedCount());
    }

    @Test
    void shouldFailJobAndMarkReviewsWhenBatchAnalysisFails() {
        SourceEntity source = source();
        ReviewCandidate collectedReview = new ReviewCandidate(
                "manual:1:20",
                "В общежитии грязно",
                "Петр",
                null,
                OffsetDateTime.now(),
                "https://example.com/manual/20"
        );
        ReviewEntity savedReview = new ReviewEntity();
        savedReview.setId(11L);
        savedReview.setSource(source);
        savedReview.setExternalId(collectedReview.externalId());
        savedReview.setText(collectedReview.text());
        savedReview.setStatus(ReviewStatus.ANALYSIS_PENDING);

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(currentUserProvider.getCurrentUsername()).thenReturn("manual-user");
        when(sourceCollectorRegistry.getCollector(SourceType.MANUAL_IMPORT)).thenReturn(sourceCollector);
        when(sourceCollector.collect(source)).thenReturn(List.of(collectedReview));
        when(reviewRepository.existsBySourceIdAndExternalId(source.getId(), collectedReview.externalId())).thenReturn(false);
        when(reviewCandidateMapper.toEntity(source, collectedReview)).thenReturn(savedReview);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(savedReview);
        org.mockito.Mockito.doThrow(new RuntimeException("analysis-service is unavailable"))
                .when(reviewBatchAnalysisService).analyzeReviews(List.of(savedReview));
        org.mockito.Mockito.doAnswer(invocation -> {
            savedReview.setStatus(ReviewStatus.FAILED_ANALYSIS);
            return null;
        }).when(reviewBatchAnalysisService)
                .markFailedAnalysis(List.of(savedReview), "analysis-service is unavailable");
        stubJobMapping(source);

        CollectionJobResponseDto response = collectionService.run(1L);

        assertEquals(CollectionJobStatus.FAILED, response.status());
        assertEquals("analysis-service is unavailable", response.errorMessage());
        assertEquals(ReviewStatus.FAILED_ANALYSIS, savedReview.getStatus());
    }

    @Test
    void shouldCollectTabiturientReviewsUsingRegisteredCollector() {
        SourceEntity source = source();
        source.setType(SourceType.TABITURIENT);
        source.setName("Tabiturient source");
        source.setBaseUrl("https://tabiturient.ru/vuzu/dvfu/");

        ReviewCandidate collectedReview = new ReviewCandidate(
                "tabiturient:11568",
                "Очень хорошие преподаватели и полезная программа обучения",
                "Студент этого вуза",
                1,
                OffsetDateTime.now(),
                "https://tabiturient.ru/sliv/n/?11568"
        );
        ReviewEntity savedReview = new ReviewEntity();
        savedReview.setId(12L);
        savedReview.setSource(source);
        savedReview.setExternalId(collectedReview.externalId());
        savedReview.setText(collectedReview.text());
        savedReview.setStatus(ReviewStatus.ANALYSIS_PENDING);

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(currentUserProvider.getCurrentUsername()).thenReturn("manual-user");
        when(sourceCollectorRegistry.getCollector(SourceType.TABITURIENT)).thenReturn(sourceCollector);
        when(sourceCollector.collect(source)).thenReturn(List.of(collectedReview));
        when(reviewRepository.existsBySourceIdAndExternalId(source.getId(), collectedReview.externalId())).thenReturn(false);
        when(reviewCandidateMapper.toEntity(source, collectedReview)).thenReturn(savedReview);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(savedReview);
        stubJobMapping(source);

        CollectionJobResponseDto response = collectionService.run(1L);

        assertEquals(CollectionJobStatus.SUCCESS, response.status());
        assertEquals(1, response.collectedCount());
    }

    @Test
    void shouldAllowManualCollectionWhenScheduleEnabled() {
        SourceEntity source = source();
        source.setScheduleEnabled(true);
        source.setScheduleIntervalMinutes(1440);
        source.setNextCollectionAt(OffsetDateTime.now().plusDays(1));

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(currentUserProvider.getCurrentUsername()).thenReturn("manual-user");
        when(sourceCollectorRegistry.getCollector(SourceType.MANUAL_IMPORT)).thenReturn(sourceCollector);
        when(sourceCollector.collect(source)).thenReturn(List.of());
        stubJobMapping(source);

        CollectionJobResponseDto response = collectionService.run(1L);

        assertEquals(CollectionJobStatus.SUCCESS, response.status());
        verify(sourceCollectionStateService).markManualCollectionFinished(any(), any());
    }

    @Test
    void shouldRejectCollectionWhenJobAlreadyRunning() {
        SourceEntity source = source();

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(currentUserProvider.getCurrentUsername()).thenReturn("manual-user");
        when(collectionJobRepository.existsBySourceIdAndStatus(1L, CollectionJobStatus.RUNNING)).thenReturn(true);
        when(collectionExceptionFactory.collectionJobAlreadyRunning(1L))
                .thenReturn(new CollectionExceptionFactory().collectionJobAlreadyRunning(1L));

        assertThrows(AuraException.class, () -> collectionService.run(1L));
    }

    @Test
    void shouldRunScheduledCollectionAsSystemScheduler() {
        SourceEntity source = source();
        source.setScheduleEnabled(true);
        source.setScheduleIntervalMinutes(60);
        source.setNextCollectionAt(OffsetDateTime.now().minusMinutes(1));

        when(sourceService.getSourceOrThrow(1L)).thenReturn(source);
        when(sourceCollectorRegistry.getCollector(SourceType.MANUAL_IMPORT)).thenReturn(sourceCollector);
        when(sourceCollector.collect(source)).thenReturn(List.of());
        stubJobMapping(source);

        CollectionJobResponseDto response = collectionService.runScheduled(1L);

        assertEquals("SYSTEM_SCHEDULER", response.triggeredBy());
        verify(sourceCollectionStateService).markScheduledCollectionFinished(any(), any());
    }

    private void stubJobMapping(SourceEntity source) {
        AtomicReference<CollectionJobEntity> savedJob = new AtomicReference<>();
        when(collectionJobRepository.save(any(CollectionJobEntity.class))).thenAnswer(invocation -> {
            CollectionJobEntity job = invocation.getArgument(0);
            if (job.getId() == null) {
                job.setId(100L);
            }
            savedJob.set(job);
            return job;
        });
        when(collectionJobRepository.findDetailedById(100L)).thenAnswer(invocation -> Optional.of(savedJob.get()));
        when(collectionJobMapper.toDto(any(CollectionJobEntity.class))).thenAnswer(invocation -> {
            CollectionJobEntity job = invocation.getArgument(0);
            return new CollectionJobResponseDto(
                    job.getId(),
                    source.getId(),
                    source.getName(),
                    job.getStatus(),
                    job.getStartedAt(),
                    job.getFinishedAt(),
                    job.getCollectedCount(),
                    job.getErrorMessage(),
                    job.getTriggeredBy()
            );
        });
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
        source.setName("Manual import source");
        source.setType(SourceType.MANUAL_IMPORT);
        source.setCollectionMode(CollectionMode.MANUAL);
        source.setScheduleEnabled(false);
        source.setBaseUrl("https://example.com/manual");
        source.setIsActive(true);
        return source;
    }
}
