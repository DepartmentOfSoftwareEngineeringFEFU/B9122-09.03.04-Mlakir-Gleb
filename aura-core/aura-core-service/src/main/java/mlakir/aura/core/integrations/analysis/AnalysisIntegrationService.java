package mlakir.aura.core.integrations.analysis;

import feign.FeignException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.core.dto.integrations.analysis.AnalyzeRequestDto;
import mlakir.aura.core.dto.integrations.analysis.AnalyzeResponseDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeItemRequestDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeItemResponseDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeRequestDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeResponseDto;
import mlakir.aura.core.dto.integrations.analysis.HealthResponseDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsAnalysisResponseDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsRequestDto;
import mlakir.aura.core.dto.integrations.analysis.SummarizeRequestDto;
import mlakir.aura.core.dto.integrations.analysis.SummarizeResponseDto;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisIntegrationService {

    private final AnalysisFeignClient analysisFeignClient;
    private final AnalysisProperties analysisProperties;

    public AnalyzeResponseDto analyze(String text) {
        try {
            log.info("Sending single review to analysis-service");
            AnalyzeResponseDto response = analysisFeignClient.analyze(new AnalyzeRequestDto(prepareTextForAnalysis(null, text)));
            validateAnalyzeResponse(response);
            return response;
        } catch (FeignException exception) {
            throw handleFeignException("Single analyze request to analysis-service failed", exception);
        } catch (AnalysisIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AnalysisIntegrationException("Unexpected error during single analyze request to analysis-service",
                    exception);
        }
    }

    public List<AnalyzeResponseDto> analyzeBatch(List<String> texts) {
        return analyzeBatch(texts, null);
    }

    public List<AnalyzeResponseDto> analyzeBatch(List<String> texts, List<Long> reviewIds) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        BatchAnalyzeRequestDto request = new BatchAnalyzeRequestDto(buildBatchItems(texts, reviewIds).stream()
                .toList());

        try {
            log.info("Sending {} reviews to analysis-service batch endpoint", texts.size());
            BatchAnalyzeResponseDto response = analysisFeignClient.analyzeBatch(request);
            List<AnalyzeResponseDto> items = validateBatchResponse(response, texts.size());
            log.info("Received {} analysis results from analysis-service", items.size());
            return items;
        } catch (FeignException exception) {
            throw handleFeignException("Batch analyze request to analysis-service failed", exception);
        } catch (AnalysisIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AnalysisIntegrationException("Unexpected error during batch analyze request to analysis-service",
                    exception);
        }
    }

    public SummarizeResponseDto summarize(String text) {
        try {
            log.info("Sending review summary request to analysis-service");
            SummarizeResponseDto response = analysisFeignClient.summarize(new SummarizeRequestDto(text));
            validateSummarizeResponse(response);
            return response;
        } catch (FeignException exception) {
            throw handleFeignException("Summarize request to analysis-service failed", exception);
        } catch (AnalysisIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AnalysisIntegrationException("Unexpected error during summarize request to analysis-service",
                    exception);
        }
    }

    public OrganizationInsightsAnalysisResponseDto generateInsights(OrganizationInsightsRequestDto request) {
        try {
            int reviewCount = request == null || request.reviews() == null ? 0 : request.reviews().size();
            log.info("Sending organization insights request to analysis-service: organizationName={}, reviews={}",
                    request == null ? null : request.organizationName(), reviewCount);
            OrganizationInsightsAnalysisResponseDto response = analysisFeignClient.generateInsights(request);
            validateInsightsResponse(response);
            return response;
        } catch (FeignException exception) {
            throw handleFeignException("Insights request to analysis-service failed", exception);
        } catch (AnalysisIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AnalysisIntegrationException("Unexpected error during insights request to analysis-service",
                    exception);
        }
    }

    public boolean isHealthy() {
        try {
            HealthResponseDto response = analysisFeignClient.health();
            return response != null
                    && response.status() != null
                    && !"DOWN".equalsIgnoreCase(response.status());
        } catch (Exception exception) {
            log.warn("Analysis-service health check failed: {}", exception.getMessage());
            return false;
        }
    }

    private List<AnalyzeResponseDto> validateBatchResponse(BatchAnalyzeResponseDto response, int expectedSize) {
        if (response == null || response.items() == null) {
            throw new AnalysisIntegrationException("analysis-service returned an empty batch response");
        }
        if (response.items().size() != expectedSize) {
            throw new AnalysisIntegrationException("analysis-service returned "
                    + response.items().size() + " results for " + expectedSize + " reviews");
        }
        return response.items().stream()
                .map(this::toAnalyzeResponse)
                .peek(this::validateAnalyzeResponse)
                .toList();
    }

    private AnalyzeResponseDto toAnalyzeResponse(BatchAnalyzeItemResponseDto item) {
        if (item == null) {
            throw new AnalysisIntegrationException("analysis-service returned a null item in batch response");
        }
        return new AnalyzeResponseDto(
                item.sentiment(),
                item.topic(),
                item.keywords() == null ? List.of() : item.keywords(),
                item.confidence(),
                item.modelVersion()
        );
    }

    private void validateAnalyzeResponse(AnalyzeResponseDto response) {
        if (response == null) {
            throw new AnalysisIntegrationException("analysis-service returned an empty response");
        }
        if (isBlank(response.sentiment()) || isBlank(response.topic()) || isBlank(response.modelVersion())
                || response.confidence() == null) {
            throw new AnalysisIntegrationException("analysis-service returned an incomplete response");
        }
    }

    private void validateSummarizeResponse(SummarizeResponseDto response) {
        if (response == null) {
            throw new AnalysisIntegrationException("analysis-service returned an empty summary response");
        }
        if (isBlank(response.summary()) || isBlank(response.modelVersion())) {
            throw new AnalysisIntegrationException("analysis-service returned an incomplete summary response");
        }
    }

    private void validateInsightsResponse(OrganizationInsightsAnalysisResponseDto response) {
        if (response == null) {
            throw new AnalysisIntegrationException("analysis-service returned an empty insights response");
        }
        if (isBlank(response.summary()) || isBlank(response.modelVersion())
                || response.strengths() == null
                || response.weaknesses() == null
                || response.recommendations() == null) {
            throw new AnalysisIntegrationException("analysis-service returned an incomplete insights response");
        }
    }

    private AnalysisIntegrationException handleFeignException(String message, FeignException exception) {
        String details = exception.status() == -1
                ? "analysis-service is unavailable"
                : "analysis-service responded with HTTP " + exception.status();
        log.error("{}: {}", message, details, exception);
        return new AnalysisIntegrationException(message + ": " + details, exception);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<BatchAnalyzeItemRequestDto> buildBatchItems(List<String> texts, List<Long> reviewIds) {
        return java.util.stream.IntStream.range(0, texts.size())
                .mapToObj(index -> new BatchAnalyzeItemRequestDto(
                        prepareTextForAnalysis(reviewIdAt(reviewIds, index), texts.get(index))
                ))
                .toList();
    }

    private Long reviewIdAt(List<Long> reviewIds, int index) {
        if (reviewIds == null || index >= reviewIds.size()) {
            return null;
        }
        return reviewIds.get(index);
    }

    private String prepareTextForAnalysis(Long reviewId, String text) {
        String prepared = text == null ? "" : text.trim();
        int maxTextLength = Math.max(analysisProperties.getMaxTextLength(), 1);
        if (prepared.length() <= maxTextLength) {
            return prepared;
        }

        log.info("Review text truncated for analysis: reviewId={}, originalLength={}, maxLength={}",
                reviewId, prepared.length(), maxTextLength);
        return prepared.substring(0, maxTextLength);
    }
}
