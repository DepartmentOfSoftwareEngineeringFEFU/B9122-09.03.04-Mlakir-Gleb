package mlakir.aura.core.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import mlakir.aura.core.dto.KeywordStatDto;
import mlakir.aura.core.dto.PageResponseDto;
import mlakir.aura.core.dto.ReviewListItemDto;
import mlakir.aura.core.dto.ReviewReanalysisResponseDto;
import mlakir.aura.core.dto.ReviewSummaryResponseDto;
import mlakir.aura.core.services.ReviewReanalysisService;
import mlakir.aura.core.services.ReviewService;
import mlakir.aura.core.services.ReviewSummaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import(ReviewControllerSecurityTest.TestSecurityConfig.class)
class ReviewControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewService reviewService;
    @MockBean
    private ReviewReanalysisService reviewReanalysisService;
    @MockBean
    private ReviewSummaryService reviewSummaryService;

    @Test
    void shouldAllowUserToFilterReviewsByOrganization() throws Exception {
        when(reviewService.findAll(eq(10L), eq(20L), isNull(), isNull(), eq("общеж"), isNull(), isNull(), any()))
                .thenReturn(new PageResponseDto<>(List.<ReviewListItemDto>of(), 0, 20, 0, 0, true));

        mockMvc.perform(get("/api/reviews")
                        .param("organizationId", "10")
                        .param("sourceId", "20")
                        .param("keyword", "общеж")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());

        verify(reviewService).findAll(eq(10L), eq(20L), isNull(), isNull(), eq("общеж"), isNull(), isNull(), any());
    }

    @Test
    void shouldAllowAdminToReanalyzeFailedReviews() throws Exception {
        when(reviewReanalysisService.reanalyzeFailedReviews(10L, 20L, 50, true))
                .thenReturn(new ReviewReanalysisResponseDto(10L, 20L, 10, 8, 2, 0, null));

        mockMvc.perform(post("/api/reviews/reanalyze")
                        .param("organizationId", "10")
                        .param("sourceId", "20")
                        .param("limit", "50")
                        .param("force", "true")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(reviewReanalysisService).reanalyzeFailedReviews(10L, 20L, 50, true);
    }

    @Test
    void shouldAllowUserToGetPopularKeywords() throws Exception {
        when(reviewService.findPopularKeywords(10L, 5))
                .thenReturn(List.of(new KeywordStatDto("общежитие", 34)));

        mockMvc.perform(get("/api/reviews/keywords/popular")
                        .param("organizationId", "10")
                        .param("limit", "5")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());

        verify(reviewService).findPopularKeywords(10L, 5);
    }

    @Test
    void shouldAllowUserToGetCachedSummary() throws Exception {
        when(reviewSummaryService.getOrGenerateSummary(15L, false))
                .thenReturn(new ReviewSummaryResponseDto(
                        15L,
                        "Краткий конспект отзыва",
                        java.time.OffsetDateTime.parse("2026-04-27T12:00:00Z"),
                        "deepseek-openrouter-0.1.0",
                        true
                ));

        mockMvc.perform(post("/api/reviews/15/summary")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());

        verify(reviewSummaryService).getOrGenerateSummary(15L, false);
    }

    @Test
    void shouldAllowAdminToForceSummaryRegeneration() throws Exception {
        when(reviewSummaryService.getOrGenerateSummary(15L, true))
                .thenReturn(new ReviewSummaryResponseDto(
                        15L,
                        "Новый краткий конспект отзыва",
                        java.time.OffsetDateTime.parse("2026-04-27T12:00:00Z"),
                        "deepseek-openrouter-0.1.0",
                        false
                ));

        mockMvc.perform(post("/api/reviews/15/summary")
                        .param("force", "true")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(reviewSummaryService).getOrGenerateSummary(15L, true);
    }

    @Test
    void shouldForbidUserToForceSummaryRegeneration() throws Exception {
        mockMvc.perform(post("/api/reviews/15/summary")
                        .param("force", "true")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidUserToReanalyzeFailedReviews() throws Exception {
        mockMvc.perform(post("/api/reviews/reanalyze")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }
    }
}
