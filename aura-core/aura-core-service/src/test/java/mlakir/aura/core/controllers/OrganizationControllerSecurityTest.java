package mlakir.aura.core.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.dto.OrganizationInsightsResponseDto;
import mlakir.aura.core.dto.OrganizationResponseDto;
import mlakir.aura.core.services.OrganizationInsightsService;
import mlakir.aura.core.services.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrganizationController.class)
@Import(OrganizationControllerSecurityTest.TestSecurityConfig.class)
class OrganizationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;
    @MockBean
    private OrganizationInsightsService organizationInsightsService;

    @Test
    void shouldAllowAdminToCreateOrganization() throws Exception {
        when(organizationService.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(response());

        mockMvc.perform(post("/api/organizations")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Дальневосточный федеральный университет",
                                  "shortName": "ДВФУ",
                                  "website": "https://www.dvfu.ru"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortName").value("ДВФУ"));
    }

    @Test
    void shouldAllowUserToListOrganizations() throws Exception {
        when(organizationService.findAll(null, null)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/organizations")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortName").value("ДВФУ"));
    }

    @Test
    void shouldAllowUserToFilterOrganizations() throws Exception {
        when(organizationService.findAll("dv", true)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/organizations")
                        .param("name", "dv")
                        .param("isActive", "true")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk());

        verify(organizationService).findAll("dv", true);
    }

    @Test
    void shouldAllowUserToGetOrganizationById() throws Exception {
        when(organizationService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/organizations/1")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortName").value("ДВФУ"));
    }

    @Test
    void shouldAllowUserToGenerateOrganizationInsights() throws Exception {
        when(organizationInsightsService.getOrGenerateInsights(1L, false, 50, null, null))
                .thenReturn(insightsResponse(true));

        mockMvc.perform(post("/api/organizations/1/insights")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(1));

        verify(organizationInsightsService).getOrGenerateInsights(1L, false, 50, null, null);
    }

    @Test
    void shouldAllowAdminToForceOrganizationInsightsRegeneration() throws Exception {
        when(organizationInsightsService.getOrGenerateInsights(1L, true, 80, "2026-04-01", "2026-04-28"))
                .thenReturn(insightsResponse(false));

        mockMvc.perform(post("/api/organizations/1/insights")
                        .param("force", "true")
                        .param("limit", "80")
                        .param("from", "2026-04-01")
                        .param("to", "2026-04-28")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(organizationInsightsService).getOrGenerateInsights(1L, true, 80, "2026-04-01", "2026-04-28");
    }

    @Test
    void shouldForbidUserToForceOrganizationInsightsRegeneration() throws Exception {
        mockMvc.perform(post("/api/organizations/1/insights")
                        .param("force", "true")
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidUserToCreateOrganization() throws Exception {
        mockMvc.perform(post("/api/organizations")
                        .with(user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Дальневосточный федеральный университет",
                                  "shortName": "ДВФУ"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToDeleteOrganization() throws Exception {
        mockMvc.perform(delete("/api/organizations/1")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNoContent());
    }

    private OrganizationResponseDto response() {
        return new OrganizationResponseDto(
                1L,
                "Дальневосточный федеральный университет",
                "ДВФУ",
                "Federal university",
                "https://www.dvfu.ru",
                true,
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now()
        );
    }

    private OrganizationInsightsResponseDto insightsResponse(boolean cached) {
        return new OrganizationInsightsResponseDto(
                1L,
                "ДВФУ",
                "Краткий отчёт по отзывам организации",
                List.of("Сильные преподаватели"),
                List.of("Проблемы с общежитием"),
                List.of("Улучшить организацию заселения"),
                OffsetDateTime.parse("2026-04-28T12:00:00Z"),
                "gemini-1.5-flash",
                cached,
                50
        );
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
