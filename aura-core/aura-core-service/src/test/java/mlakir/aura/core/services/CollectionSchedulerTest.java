package mlakir.aura.core.services;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.enums.CollectionJobStatus;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.CollectionJobRepository;
import mlakir.aura.core.repositories.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionSchedulerTest {

    @Mock
    private SourceRepository sourceRepository;
    @Mock
    private CollectionJobRepository collectionJobRepository;
    @Mock
    private CollectionService collectionService;

    private CollectionSchedulerProperties properties;
    private CollectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new CollectionSchedulerProperties();
        scheduler = new CollectionScheduler(properties, sourceRepository, collectionJobRepository, collectionService);
    }

    @Test
    void shouldRunDueScheduledSource() {
        SourceEntity source = source(true, OffsetDateTime.now().minusMinutes(1));
        when(sourceRepository.findByScheduleEnabledTrueAndNextCollectionAtLessThanEqual(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(source));

        scheduler.runDueCollections();

        verify(collectionService).runScheduled(1L);
    }

    @Test
    void shouldNotRunWhenSchedulerDisabled() {
        properties.setEnabled(false);

        scheduler.runDueCollections();

        verify(sourceRepository, never())
                .findByScheduleEnabledTrueAndNextCollectionAtLessThanEqual(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSkipSourceWithoutNextCollectionAt() {
        SourceEntity source = source(true, null);
        when(sourceRepository.findByScheduleEnabledTrueAndNextCollectionAtLessThanEqual(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(source));

        scheduler.runDueCollections();

        verify(collectionService, never()).runScheduled(1L);
    }

    @Test
    void shouldSkipSourceWithScheduleDisabled() {
        SourceEntity source = source(false, OffsetDateTime.now().minusMinutes(1));
        when(sourceRepository.findByScheduleEnabledTrueAndNextCollectionAtLessThanEqual(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(source));

        scheduler.runDueCollections();

        verify(collectionService, never()).runScheduled(1L);
    }

    @Test
    void shouldSkipSourceWithActiveJob() {
        SourceEntity source = source(true, OffsetDateTime.now().minusMinutes(1));
        when(sourceRepository.findByScheduleEnabledTrueAndNextCollectionAtLessThanEqual(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(source));
        when(collectionJobRepository.existsBySourceIdAndStatus(1L, CollectionJobStatus.RUNNING)).thenReturn(true);

        scheduler.runDueCollections();

        verify(collectionService, never()).runScheduled(1L);
    }

    private SourceEntity source(boolean scheduleEnabled, OffsetDateTime nextCollectionAt) {
        SourceEntity source = new SourceEntity();
        source.setId(1L);
        source.setScheduleEnabled(scheduleEnabled);
        source.setNextCollectionAt(nextCollectionAt);
        return source;
    }
}
