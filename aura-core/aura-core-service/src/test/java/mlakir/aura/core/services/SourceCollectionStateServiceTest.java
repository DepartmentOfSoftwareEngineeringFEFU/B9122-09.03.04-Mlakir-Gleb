package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SourceCollectionStateServiceTest {

    @Mock
    private SourceRepository sourceRepository;

    private SourceCollectionStateService service;

    @BeforeEach
    void setUp() {
        service = new SourceCollectionStateService(sourceRepository);
    }

    @Test
    void shouldNotShiftNextCollectionAtAfterManualCollection() {
        SourceEntity source = new SourceEntity();
        source.setId(1L);
        OffsetDateTime nextCollectionAt = OffsetDateTime.now().plusDays(1);
        OffsetDateTime finishedAt = OffsetDateTime.now();
        source.setNextCollectionAt(nextCollectionAt);
        when(sourceRepository.findById(1L)).thenReturn(Optional.of(source));

        service.markManualCollectionFinished(1L, finishedAt);

        assertEquals(finishedAt, source.getLastCollectedAt());
        assertEquals(nextCollectionAt, source.getNextCollectionAt());
    }

    @Test
    void shouldShiftNextCollectionAtAfterScheduledCollection() {
        SourceEntity source = new SourceEntity();
        source.setId(1L);
        source.setScheduleEnabled(true);
        source.setScheduleIntervalMinutes(60);
        OffsetDateTime finishedAt = OffsetDateTime.now();
        when(sourceRepository.findById(1L)).thenReturn(Optional.of(source));

        service.markScheduledCollectionFinished(1L, finishedAt);

        assertEquals(finishedAt, source.getLastCollectedAt());
        assertEquals(finishedAt.plusMinutes(60), source.getNextCollectionAt());
    }
}
