package mlakir.aura.core.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.CreateOrganizationRequestDto;
import mlakir.aura.core.dto.OrganizationInsightsResponseDto;
import mlakir.aura.core.dto.OrganizationResponseDto;
import mlakir.aura.core.dto.UpdateOrganizationRequestDto;
import mlakir.aura.core.services.OrganizationInsightsService;
import mlakir.aura.core.services.OrganizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/organizations")
@SecurityRequirement(name = "Bearer Authorization")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationInsightsService organizationInsightsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public OrganizationResponseDto create(@Valid @RequestBody CreateOrganizationRequestDto requestDto) {
        return organizationService.create(requestDto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<OrganizationResponseDto> findAll(@RequestParam(required = false) String name,
                                                 @RequestParam(required = false) Boolean isActive) {
        return organizationService.findAll(name, isActive);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public OrganizationResponseDto findById(@PathVariable Long id) {
        return organizationService.findById(id);
    }

    @PostMapping("/{organizationId}/insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER') and (!#force or hasRole('ADMIN'))")
    public OrganizationInsightsResponseDto generateInsights(
            @PathVariable Long organizationId,
            @RequestParam(required = false, defaultValue = "false") boolean force,
            @RequestParam(required = false, defaultValue = "50") Integer limit,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return organizationInsightsService.getOrGenerateInsights(organizationId, force, limit, from, to);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public OrganizationResponseDto update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateOrganizationRequestDto requestDto) {
        return organizationService.update(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        organizationService.delete(id);
    }
}
