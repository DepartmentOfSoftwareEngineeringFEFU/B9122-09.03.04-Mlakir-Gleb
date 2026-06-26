package mlakir.aura.core.controllers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import mlakir.aura.core.dto.DashboardResponseDto;
import mlakir.aura.core.dto.DashboardSentimentDto;
import mlakir.aura.core.dto.DashboardSummaryDto;
import mlakir.aura.core.enums.SentimentType;
import mlakir.aura.core.services.DashboardService;
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

@WebMvcTest(DashboardController.class)
@Import(DashboardControllerSecurityTest.TestSecurityConfig.class)
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @Test
    void shouldAllowUserToRequestDashboardWithFilters() throws Exception {
        when(dashboardService.getDashboard(
                eq(1L),
                eq(LocalDate.of(2026, 1, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                eq(2L),
                eq(SentimentType.POSITIVE)
        )).thenReturn(new DashboardResponseDto(0, 0, new DashboardSentimentDto(0, 0, 0), List.of(), List.of()));

        mockMvc.perform(get("/api/dashboard")
                        .param("organizationId", "1")
                        .param("from", "2026-01-01")
                        .param("to", "2026-03-31")
                        .param("sourceId", "2")
                        .param("sentiment", "POSITIVE")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());

        verify(dashboardService).getDashboard(
                1L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31),
                2L,
                SentimentType.POSITIVE
        );
    }

    @Test
    void shouldRequireOrganizationIdForDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .with(user("user").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldPassOrganizationIdToDashboardSummary() throws Exception {
        when(dashboardService.getSummary(eq(1L))).thenReturn(new DashboardSummaryDto(0, 0, 0, 0));

        mockMvc.perform(get("/api/dashboard/summary")
                        .param("organizationId", "1")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());

        verify(dashboardService).getSummary(1L);
    }

    @Test
    void shouldRequireOrganizationIdForDashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .with(user("user").roles("USER")))
                .andExpect(status().isBadRequest());
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
