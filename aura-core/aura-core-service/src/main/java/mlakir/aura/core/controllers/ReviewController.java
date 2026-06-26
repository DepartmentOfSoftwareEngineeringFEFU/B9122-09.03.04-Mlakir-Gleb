package mlakir.aura.core.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.KeywordStatDto;
import mlakir.aura.core.dto.PageResponseDto;
import mlakir.aura.core.dto.ReviewReanalysisResponseDto;
import mlakir.aura.core.dto.ReviewSummaryResponseDto;
import mlakir.aura.core.enums.ReviewTopic;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.dto.ReviewListItemDto;
import mlakir.aura.core.dto.ReviewResponseDto;
import mlakir.aura.core.services.ReviewReanalysisService;
import mlakir.aura.core.services.ReviewService;
import mlakir.aura.core.services.ReviewSummaryService;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
@SecurityRequirement(name = "Bearer Authorization")
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewReanalysisService reviewReanalysisService;
    private final ReviewSummaryService reviewSummaryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public PageResponseDto<ReviewListItemDto> findAll(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) SentimentType sentiment,
            @RequestParam(required = false) ReviewTopic topic,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable
    ) {
        return reviewService.findAll(organizationId, sourceId, sentiment, topic, keyword, dateFrom, dateTo, pageable);
    }

    @GetMapping("/keywords/popular")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<KeywordStatDto> findPopularKeywords(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        return reviewService.findPopularKeywords(organizationId, limit);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ReviewResponseDto findById(@PathVariable Long id) {
        return reviewService.findById(id);
    }

    @PostMapping("/{reviewId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER') and (!#force or hasRole('ADMIN'))")
    public ReviewSummaryResponseDto summarizeReview(
            @PathVariable Long reviewId,
            @RequestParam(required = false, defaultValue = "false") boolean force
    ) {
        return reviewSummaryService.getOrGenerateSummary(reviewId, force);
    }

    @PostMapping("/reanalyze")
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewReanalysisResponseDto reanalyze(
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false, defaultValue = "false") boolean force
    ) {
        return reviewReanalysisService.reanalyzeFailedReviews(organizationId, sourceId, limit, force);
    }
}
