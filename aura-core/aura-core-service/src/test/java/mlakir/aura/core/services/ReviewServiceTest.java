package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mlakir.aura.core.mappers.PageResponseMapper;
import mlakir.aura.core.mappers.ReviewMapper;
import mlakir.aura.core.repositories.ReviewAnalysisRepository;
import mlakir.aura.core.repositories.ReviewRepository;
import mlakir.aura.core.specifications.ReviewSpecifications;
import mlakir.aura.core.exceptions.ReviewExceptionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewAnalysisRepository reviewAnalysisRepository;
    @Mock
    private ReviewMapper reviewMapper;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                reviewRepository,
                reviewAnalysisRepository,
                reviewMapper,
                new PageResponseMapper(),
                new ReviewExceptionFactory()
        );
    }

    @Test
    void shouldKeepOldBehaviorWhenKeywordIsBlank() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(reviewRepository.findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(new PageImpl<>(java.util.List.of(), pageable, 0));

        var response = reviewService.findAll(1L, null, null, null, "  ", null, null, pageable);

        assertEquals(0, response.totalElements());
        verify(reviewRepository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable));
    }

    @Test
    void shouldAcceptKeywordWithOtherFiltersAndPagination() {
        PageRequest pageable = PageRequest.of(1, 10);
        when(reviewRepository.findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable)))
                .thenReturn(new PageImpl<>(java.util.List.of(), pageable, 0));

        var response = reviewService.findAll(1L, 2L, null, null, "Общеж", null, null, pageable);

        assertEquals(1, response.page());
        assertEquals(10, response.size());
        verify(reviewRepository).findAll(any(Specification.class), org.mockito.ArgumentMatchers.eq(pageable));
    }

    @Test
    void keywordSpecificationShouldIgnoreEmptyKeyword() {
        org.junit.jupiter.api.Assertions.assertNotNull(ReviewSpecifications.keywordContains("общеж"));
        org.junit.jupiter.api.Assertions.assertNotNull(ReviewSpecifications.keywordContains(" ОБЩЕЖ "));
    }

    @Test
    void shouldReturnPopularKeywordsWithNormalizedLimit() {
        when(reviewAnalysisRepository.findPopularKeywords(1L, 20)).thenReturn(java.util.List.of(keyword("общежитие", 34)));

        var response = reviewService.findPopularKeywords(1L, null);

        assertEquals(1, response.size());
        assertEquals("общежитие", response.getFirst().keyword());
        assertEquals(34, response.getFirst().count());
        verify(reviewAnalysisRepository).findPopularKeywords(1L, 20);
    }

    private ReviewAnalysisRepository.KeywordCountProjection keyword(String keyword, long count) {
        return new ReviewAnalysisRepository.KeywordCountProjection() {
            @Override
            public String getKeyword() {
                return keyword;
            }

            @Override
            public long getCount() {
                return count;
            }
        };
    }
}
