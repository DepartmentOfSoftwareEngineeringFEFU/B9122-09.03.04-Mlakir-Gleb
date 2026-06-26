package mlakir.aura.core.integrations.analysis;

import mlakir.aura.core.dto.integrations.analysis.AnalyzeRequestDto;
import mlakir.aura.core.dto.integrations.analysis.AnalyzeResponseDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeRequestDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeResponseDto;
import mlakir.aura.core.dto.integrations.analysis.HealthResponseDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsAnalysisResponseDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsRequestDto;
import mlakir.aura.core.dto.integrations.analysis.SummarizeRequestDto;
import mlakir.aura.core.dto.integrations.analysis.SummarizeResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "analysisFeignClient",
        url = "${analysis.service.url}"
)
public interface AnalysisFeignClient {

    @PostMapping("/analyze")
    AnalyzeResponseDto analyze(@RequestBody AnalyzeRequestDto request);

    @PostMapping("/analyze/batch")
    BatchAnalyzeResponseDto analyzeBatch(@RequestBody BatchAnalyzeRequestDto request);

    @PostMapping("/summarize")
    SummarizeResponseDto summarize(@RequestBody SummarizeRequestDto request);

    @PostMapping("/insights")
    OrganizationInsightsAnalysisResponseDto generateInsights(@RequestBody OrganizationInsightsRequestDto request);

    @GetMapping("/health")
    HealthResponseDto health();
}
