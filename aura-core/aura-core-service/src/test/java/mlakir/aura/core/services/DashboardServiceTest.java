package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import mlakir.aura.core.dto.DashboardResponseDto;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.repositories.DashboardFilters;
import mlakir.aura.core.repositories.DashboardRepository;
import mlakir.aura.core.repositories.DashboardRepositoryCustom;
import mlakir.aura.core.repositories.ReviewAnalysisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;
    @Mock
    private ReviewAnalysisRepository reviewAnalysisRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(dashboardRepository, reviewAnalysisRepository);
    }

    @Test
    void shouldBuildAggregatedDashboardResponseWithFilters() {
        OffsetDateTime from = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        DashboardFilters filters = new DashboardFilters(1L, from, to, 2L, SentimentType.POSITIVE);

        when(dashboardRepository.countReviews(eq(filters))).thenReturn(15L);
        when(dashboardRepository.countSources(eq(filters))).thenReturn(3L);
        when(dashboardRepository.aggregateSentiment(eq(filters))).thenReturn(List.of(
                sentiment(SentimentType.POSITIVE, 9),
                sentiment(SentimentType.NEUTRAL, 4),
                sentiment(SentimentType.NEGATIVE, 2)
        ));
        when(dashboardRepository.topCategories(eq(filters))).thenReturn(List.of(
                category(ReviewTopic.TEACHERS, 7),
                category(ReviewTopic.DORMITORY, 5)
        ));
        when(dashboardRepository.timeline(eq(filters))).thenReturn(List.of(
                timeline("2026-01", 5),
                timeline("2026-02", 10)
        ));

        DashboardResponseDto response = dashboardService.getDashboard(
                1L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                2L,
                SentimentType.POSITIVE
        );

        assertEquals(15L, response.totalReviews());
        assertEquals(3L, response.sourcesCount());
        assertEquals(9L, response.sentiment().positive());
        assertEquals(4L, response.sentiment().neutral());
        assertEquals(2L, response.sentiment().negative());
        assertEquals(2, response.topCategories().size());
        assertEquals(ReviewTopic.TEACHERS, response.topCategories().getFirst().category());
        assertEquals("2026-01", response.timeline().getFirst().month());
        assertEquals(5L, response.timeline().getFirst().count());
    }

    @Test
    void shouldReturnZeroSentimentBucketsWhenRepositoryHasNoRows() {
        OffsetDateTime from = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        DashboardFilters filters = new DashboardFilters(1L, from, to, null, null);

        when(dashboardRepository.countReviews(eq(filters))).thenReturn(0L);
        when(dashboardRepository.countSources(eq(filters))).thenReturn(0L);
        when(dashboardRepository.aggregateSentiment(eq(filters))).thenReturn(List.of());
        when(dashboardRepository.topCategories(eq(filters))).thenReturn(List.of());
        when(dashboardRepository.timeline(eq(filters))).thenReturn(List.of());

        DashboardResponseDto response = dashboardService.getDashboard(
                1L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                null,
                null
        );

        assertEquals(0L, response.sentiment().positive());
        assertEquals(0L, response.sentiment().neutral());
        assertEquals(0L, response.sentiment().negative());
    }

    @Test
    void shouldReturnOrganizationScopedSummary() {
        when(dashboardRepository.countTotalReviews(1L)).thenReturn(12L);
        when(dashboardRepository.countBySentiment(1L, SentimentType.POSITIVE)).thenReturn(7L);
        when(dashboardRepository.countBySentiment(1L, SentimentType.NEUTRAL)).thenReturn(3L);
        when(dashboardRepository.countBySentiment(1L, SentimentType.NEGATIVE)).thenReturn(2L);

        var response = dashboardService.getSummary(1L);

        assertEquals(12L, response.totalReviews());
        assertEquals(7L, response.positiveCount());
        assertEquals(3L, response.neutralCount());
        assertEquals(2L, response.negativeCount());
    }

    private DashboardRepositoryCustom.SentimentCount sentiment(SentimentType sentiment, long count) {
        return new DashboardRepositoryCustom.SentimentCount(sentiment, count);
    }

    private DashboardRepositoryCustom.CategoryCount category(ReviewTopic topic, long count) {
        return new DashboardRepositoryCustom.CategoryCount(topic, count);
    }

    private DashboardRepositoryCustom.TimelineCount timeline(String month, long count) {
        return new DashboardRepositoryCustom.TimelineCount(month, count);
    }
}
