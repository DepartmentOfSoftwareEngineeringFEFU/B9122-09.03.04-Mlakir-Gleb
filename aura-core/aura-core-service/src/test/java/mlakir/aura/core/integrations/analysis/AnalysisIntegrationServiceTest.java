package mlakir.aura.core.integrations.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import mlakir.aura.core.dto.integrations.analysis.AnalyzeResponseDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeRequestDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeItemResponseDto;
import mlakir.aura.core.dto.integrations.analysis.BatchAnalyzeResponseDto;
import mlakir.aura.core.dto.integrations.analysis.HealthResponseDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsAnalysisResponseDto;
import mlakir.aura.core.dto.integrations.analysis.OrganizationInsightsRequestDto;
import mlakir.aura.core.dto.integrations.analysis.SummarizeResponseDto;
import mlakir.aura.core.dto.OrganizationInsightsReviewItemDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalysisIntegrationServiceTest {

    @Mock
    private AnalysisFeignClient analysisFeignClient;
    @Mock
    private AnalysisProperties analysisProperties;

    @InjectMocks
    private AnalysisIntegrationService analysisIntegrationService;

    @Test
    void shouldAnalyzeBatchSuccessfully() {
        when(analysisProperties.getMaxTextLength()).thenReturn(10000);
        BatchAnalyzeResponseDto response = new BatchAnalyzeResponseDto(List.of(
                new BatchAnalyzeItemResponseDto(
                        "POSITIVE",
                        "TEACHERS",
                        List.of("преподаватели"),
                        new BigDecimal("0.93"),
                        "rule-based-v1"
                ),
                new BatchAnalyzeItemResponseDto(
                        "NEGATIVE",
                        "DORMITORY",
                        List.of("общежитие", "грязно"),
                        new BigDecimal("0.91"),
                        "rule-based-v1"
                )
        ));
        when(analysisFeignClient.analyzeBatch(org.mockito.ArgumentMatchers.any())).thenReturn(response);

        List<AnalyzeResponseDto> result = analysisIntegrationService.analyzeBatch(List.of(
                "Очень хорошие преподаватели",
                "В общежитии грязно"
        ));

        assertEquals(2, result.size());
        assertEquals("POSITIVE", result.get(0).sentiment());
        assertEquals("DORMITORY", result.get(1).topic());
    }

    @Test
    void shouldTruncateLongBatchTextBeforeSendingToAnalysisService() {
        when(analysisProperties.getMaxTextLength()).thenReturn(10000);
        when(analysisFeignClient.analyzeBatch(any())).thenReturn(new BatchAnalyzeResponseDto(List.of(
                new BatchAnalyzeItemResponseDto(
                        "POSITIVE",
                        "TEACHERS",
                        List.of("преподаватели"),
                        new BigDecimal("0.93"),
                        "rule-based-v1"
                )
        )));

        String longText = "  " + "x".repeat(10050) + "  ";

        analysisIntegrationService.analyzeBatch(List.of(longText), List.of(17L));

        ArgumentCaptor<BatchAnalyzeRequestDto> captor = ArgumentCaptor.forClass(BatchAnalyzeRequestDto.class);
        verify(analysisFeignClient).analyzeBatch(captor.capture());
        String sentText = captor.getValue().items().getFirst().text();
        assertEquals(10000, sentText.length());
        assertEquals("x".repeat(10000), sentText);
    }

    @Test
    void shouldKeepShortBatchTextUnchangedExceptTrim() {
        when(analysisProperties.getMaxTextLength()).thenReturn(10000);
        when(analysisFeignClient.analyzeBatch(any())).thenReturn(new BatchAnalyzeResponseDto(List.of(
                new BatchAnalyzeItemResponseDto(
                        "POSITIVE",
                        "TEACHERS",
                        List.of("преподаватели"),
                        new BigDecimal("0.93"),
                        "rule-based-v1"
                )
        )));

        analysisIntegrationService.analyzeBatch(List.of("  short text  "), List.of(18L));

        ArgumentCaptor<BatchAnalyzeRequestDto> captor = ArgumentCaptor.forClass(BatchAnalyzeRequestDto.class);
        verify(analysisFeignClient).analyzeBatch(captor.capture());
        assertEquals("short text", captor.getValue().items().getFirst().text());
    }

    @Test
    void shouldThrowMeaningfulErrorWhenAnalysisServiceIsUnavailable() {
        when(analysisProperties.getMaxTextLength()).thenReturn(10000);
        when(analysisFeignClient.analyzeBatch(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("Connection refused"));

        AnalysisIntegrationException exception = assertThrows(
                AnalysisIntegrationException.class,
                () -> analysisIntegrationService.analyzeBatch(List.of("test"))
        );

        assertTrue(exception.getMessage().contains("Unexpected error during batch analyze request to analysis-service"));
    }

    @Test
    void shouldSummarizeSuccessfully() {
        when(analysisFeignClient.summarize(any()))
                .thenReturn(new SummarizeResponseDto("Краткий конспект", "deepseek-openrouter-0.1.0"));

        SummarizeResponseDto response = analysisIntegrationService.summarize("Очень длинный отзыв");

        assertEquals("Краткий конспект", response.summary());
        assertEquals("deepseek-openrouter-0.1.0", response.modelVersion());
    }

    @Test
    void shouldGenerateOrganizationInsightsSuccessfully() {
        when(analysisFeignClient.generateInsights(any()))
                .thenReturn(new OrganizationInsightsAnalysisResponseDto(
                        "Краткий отчёт",
                        List.of("Сильные преподаватели"),
                        List.of("Проблемы с общежитием"),
                        List.of("Улучшить организацию заселения"),
                        "gemini-1.5-flash"
                ));

        OrganizationInsightsAnalysisResponseDto response = analysisIntegrationService.generateInsights(
                new OrganizationInsightsRequestDto(
                        "Дальневосточный федеральный университет",
                        List.of(new OrganizationInsightsReviewItemDto("Отзыв", "POSITIVE", "TEACHERS"))
                )
        );

        assertEquals("Краткий отчёт", response.summary());
        assertEquals("gemini-1.5-flash", response.modelVersion());
    }

    @Test
    void shouldReturnHealthStatus() {
        when(analysisFeignClient.health()).thenReturn(new HealthResponseDto("UP", "local", "rule-based-v1"));

        assertTrue(analysisIntegrationService.isHealthy());
    }

    @Test
    void shouldReturnFalseWhenHealthCheckFails() {
        when(analysisFeignClient.health()).thenThrow(new RuntimeException("timeout"));

        assertFalse(analysisIntegrationService.isHealthy());
    }
}
