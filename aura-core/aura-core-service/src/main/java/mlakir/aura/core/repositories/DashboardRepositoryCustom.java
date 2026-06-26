package mlakir.aura.core.repositories;

import java.util.List;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;

public interface DashboardRepositoryCustom {

    long countReviews(DashboardFilters filters);

    long countSources(DashboardFilters filters);

    List<SentimentCount> aggregateSentiment(DashboardFilters filters);

    List<CategoryCount> topCategories(DashboardFilters filters);

    List<TimelineCount> timeline(DashboardFilters filters);

    record SentimentCount(SentimentType sentiment, long count) {
    }

    record CategoryCount(ReviewTopic category, long count) {
    }

    record TimelineCount(String month, long count) {
    }
}
