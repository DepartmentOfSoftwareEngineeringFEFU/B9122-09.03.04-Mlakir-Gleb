package mlakir.aura.core.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.DashboardResponseDto;
import mlakir.aura.core.dto.DashboardSummaryDto;
import mlakir.aura.core.dto.SentimentDistributionItemDto;
import mlakir.aura.core.dto.TopicDistributionItemDto;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.services.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "Bearer Authorization")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DashboardResponseDto getDashboard(
            @RequestParam Long organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long sourceId,
            @RequestParam(required = false) SentimentType sentiment
    ) {
        return dashboardService.getDashboard(organizationId, from, to, sourceId, sentiment);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public DashboardSummaryDto getSummary(@RequestParam Long organizationId) {
        return dashboardService.getSummary(organizationId);
    }

    @GetMapping("/sentiment-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<SentimentDistributionItemDto> getSentimentDistribution(@RequestParam Long organizationId) {
        return dashboardService.getSentimentDistribution(organizationId);
    }

    @GetMapping("/topic-distribution")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<TopicDistributionItemDto> getTopicDistribution(@RequestParam Long organizationId) {
        return dashboardService.getTopicDistribution(organizationId);
    }
}
