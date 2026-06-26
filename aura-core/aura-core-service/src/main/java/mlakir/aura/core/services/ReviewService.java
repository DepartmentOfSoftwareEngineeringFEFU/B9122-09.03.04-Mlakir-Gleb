package mlakir.aura.core.services;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.KeywordStatDto;
import mlakir.aura.core.dto.PageResponseDto;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.mappers.PageResponseMapper;
import mlakir.aura.core.dto.ReviewListItemDto;
import mlakir.aura.core.dto.ReviewResponseDto;
import mlakir.aura.core.exceptions.ReviewExceptionFactory;
import mlakir.aura.core.mappers.ReviewMapper;
import mlakir.aura.core.models.ReviewEntity;
import mlakir.aura.core.repositories.ReviewRepository;
import mlakir.aura.core.repositories.ReviewAnalysisRepository;
import mlakir.aura.core.specifications.ReviewSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;
    private final ReviewMapper reviewMapper;
    private final PageResponseMapper pageResponseMapper;
    private final ReviewExceptionFactory reviewExceptionFactory;

    @Transactional(readOnly = true)
    public PageResponseDto<ReviewListItemDto> findAll(
            Long organizationId,
            Long sourceId,
            SentimentType sentiment,
            ReviewTopic topic,
            String keyword,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        Specification<ReviewEntity> specification = Specification
                .where(ReviewSpecifications.withFetches())
                .and(ReviewSpecifications.organizationIdEquals(organizationId))
                .and(ReviewSpecifications.sourceIdEquals(sourceId))
                .and(ReviewSpecifications.hasSentiment(sentiment))
                .and(ReviewSpecifications.hasTopic(topic))
                .and(ReviewSpecifications.keywordContains(keyword))
                .and(ReviewSpecifications.publishedAtFrom(dateFrom))
                .and(ReviewSpecifications.publishedAtTo(dateTo));

        var page = reviewRepository.findAll(specification, pageable)
                .map(reviewMapper::toListItemDto);
        return pageResponseMapper.toDto(page);
    }

    @Transactional(readOnly = true)
    public ReviewResponseDto findById(Long reviewId) {
        ReviewEntity entity = reviewRepository.findDetailedById(reviewId)
                .orElseThrow(() -> reviewExceptionFactory.reviewNotFound(reviewId));
        return reviewMapper.toResponseDto(entity);
    }

    @Transactional(readOnly = true)
    public List<KeywordStatDto> findPopularKeywords(Long organizationId, Integer limit) {
        int normalizedLimit = normalizePopularKeywordsLimit(limit);
        return reviewAnalysisRepository.findPopularKeywords(organizationId, normalizedLimit).stream()
                .map(item -> new KeywordStatDto(item.getKeyword(), item.getCount()))
                .toList();
    }

    private int normalizePopularKeywordsLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 100);
    }
}
