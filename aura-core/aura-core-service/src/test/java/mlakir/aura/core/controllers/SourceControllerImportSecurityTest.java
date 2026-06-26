package mlakir.aura.core.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.dto.ManualImportResponseDto;
import mlakir.aura.core.dto.OrganizationShortResponseDto;
import mlakir.aura.core.dto.SourceResponseDto;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.services.ManualImportService;
import mlakir.aura.core.services.SourceService;
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
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(SourceController.class)
@Import(SourceControllerImportSecurityTest.TestSecurityConfig.class)
class SourceControllerImportSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SourceService sourceService;
    @MockBean
    private ManualImportService manualImportService;

    @Test
    void shouldAllowAdminToImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "reviews.csv", "text/csv", "a,b\n".getBytes());
        when(manualImportService.importCsv(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ManualImportResponseDto(1L, "reviews.csv", 1, 1, 0, 0));

        mockMvc.perform(multipart("/api/sources/1/import")
                        .file(file)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceId").value(1))
                .andExpect(jsonPath("$.fileName").value("reviews.csv"));
    }

    @Test
    void shouldForbidUserImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "reviews.csv", "text/csv", "a,b\n".getBytes());

        mockMvc.perform(multipart("/api/sources/1/import")
                        .file(file)
                        .with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowUserToFilterSources() throws Exception {
        when(sourceService.findAll(1L, "dvfu", SourceType.MANUAL_IMPORT, true, true))
                .thenReturn(List.of(sourceResponse()));

        mockMvc.perform(get("/api/sources")
                        .param("organizationId", "1")
                        .param("name", "dvfu")
                        .param("type", "MANUAL_IMPORT")
                        .param("isActive", "true")
                        .param("scheduleEnabled", "true")
                        .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("MANUAL_IMPORT"));

        verify(sourceService).findAll(1L, "dvfu", SourceType.MANUAL_IMPORT, true, true);
    }

    private SourceResponseDto sourceResponse() {
        return new SourceResponseDto(
                1L,
                new OrganizationShortResponseDto(1L, "Дальневосточный федеральный университет", "ДВФУ"),
                "Импорт отзывов ДВФУ",
                SourceType.MANUAL_IMPORT,
                "https://example.com/manual",
                true,
                CollectionMode.MANUAL,
                false,
                null,
                null,
                null,
                "CSV import",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now()
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
