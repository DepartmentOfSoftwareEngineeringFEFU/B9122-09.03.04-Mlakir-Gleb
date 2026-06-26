package mlakir.aura.core.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.CategoryStatDto;
import mlakir.aura.core.dto.DashboardResponseDto;
import mlakir.aura.core.dto.DashboardSentimentDto;
import mlakir.aura.core.dto.DashboardSummaryDto;
import mlakir.aura.core.dto.SentimentDistributionItemDto;
import mlakir.aura.core.dto.TimelinePointDto;
import mlakir.aura.core.dto.TopicDistributionItemDto;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.repositories.DashboardFilters;
import mlakir.aura.core.repositories.DashboardRepository;
import mlakir.aura.core.repositories.DashboardRepositoryCustom;
import mlakir.aura.core.repositories.ReviewAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboard(Long organizationId,
                                             LocalDate from,
                                             LocalDate to,
                                             Long sourceId,
                                             SentimentType sentiment) {
        DashboardFilters filters = toFilters(organizationId, from, to, sourceId, sentiment);
        return new DashboardResponseDto(
                countReviews(filters),
                countSources(filters),
                aggregateSentiment(filters),
                topCategories(filters),
                timeline(filters)
        );
    }

    @Transactional(readOnly = true)
    public long countReviews(DashboardFilters filters) {
        return dashboardRepository.countReviews(filters);
    }

    @Transactional(readOnly = true)
    public long countSources(DashboardFilters filters) {
        return dashboardRepository.countSources(filters);
    }

    @Transactional(readOnly = true)
    public DashboardSentimentDto aggregateSentiment(DashboardFilters filters) {
        long positive = 0;
        long neutral = 0;
        long negative = 0;

        for (DashboardRepositoryCustom.SentimentCount item : dashboardRepository.aggregateSentiment(filters)) {
            if (item.sentiment() == SentimentType.POSITIVE) {
                positive = item.count();
            } else if (item.sentiment() == SentimentType.NEUTRAL) {
                neutral = item.count();
            } else if (item.sentiment() == SentimentType.NEGATIVE) {
                negative = item.count();
            }
        }

        return new DashboardSentimentDto(positive, neutral, negative);
    }

    @Transactional(readOnly = true)
    public List<CategoryStatDto> topCategories(DashboardFilters filters) {
        return dashboardRepository.topCategories(filters).stream()
                .map(item -> new CategoryStatDto(item.category(), item.count()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TimelinePointDto> timeline(DashboardFilters filters) {
        return dashboardRepository.timeline(filters).stream()
                .map(item -> new TimelinePointDto(item.month(), item.count()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(Long organizationId) {
        return new DashboardSummaryDto(
                dashboardRepository.countTotalReviews(organizationId),
                dashboardRepository.countBySentiment(organizationId, SentimentType.POSITIVE),
                dashboardRepository.countBySentiment(organizationId, SentimentType.NEUTRAL),
                dashboardRepository.countBySentiment(organizationId, SentimentType.NEGATIVE)
        );
    }

    @Transactional(readOnly = true)
    public List<SentimentDistributionItemDto> getSentimentDistribution(Long organizationId) {
        return reviewAnalysisRepository.countByOrganizationIdAndSentiment(organizationId).stream()
                .map(item -> new SentimentDistributionItemDto(item.getSentiment(), item.getCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TopicDistributionItemDto> getTopicDistribution(Long organizationId) {
        return reviewAnalysisRepository.countByOrganizationIdAndTopic(organizationId).stream()
                .map(item -> new TopicDistributionItemDto(item.getTopic(), item.getCount()))
                .toList();
    }

    private DashboardFilters toFilters(Long organizationId,
                                       LocalDate from,
                                       LocalDate to,
                                       Long sourceId,
                                       SentimentType sentiment) {
        OffsetDateTime fromBoundary = from == null ? null : from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toBoundary = to == null ? null : to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        return new DashboardFilters(organizationId, fromBoundary, toBoundary, sourceId, sentiment);
    }
}
