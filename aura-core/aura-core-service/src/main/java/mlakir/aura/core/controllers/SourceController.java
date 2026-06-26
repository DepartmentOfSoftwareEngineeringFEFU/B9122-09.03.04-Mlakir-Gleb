package mlakir.aura.core.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.CreateSourceRequestDto;
import mlakir.aura.core.dto.SourceResponseDto;
import mlakir.aura.core.dto.UpdateSourceRequestDto;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.services.SourceService;
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
@RequestMapping("/api/sources")
@SecurityRequirement(name = "Bearer Authorization")
public class SourceController {

    private final SourceService sourceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SourceResponseDto create(@Valid @RequestBody CreateSourceRequestDto requestDto) {
        return sourceService.create(requestDto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public List<SourceResponseDto> findAll(@RequestParam(required = false) Long organizationId,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) SourceType type,
                                           @RequestParam(required = false) Boolean isActive,
                                           @RequestParam(required = false) Boolean scheduleEnabled) {
        return sourceService.findAll(organizationId, name, type, isActive, scheduleEnabled);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public SourceResponseDto findById(@PathVariable Long id) {
        return sourceService.findById(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SourceResponseDto update(@PathVariable Long id, @Valid @RequestBody UpdateSourceRequestDto requestDto) {
        return sourceService.update(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        sourceService.delete(id);
    }
}
