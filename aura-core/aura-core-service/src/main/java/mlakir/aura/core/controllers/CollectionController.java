package mlakir.aura.core.controllers;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.CollectionJobResponseDto;
import mlakir.aura.core.services.CollectionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collection")
@SecurityRequirement(name = "Bearer Authorization")
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping("/run/{sourceId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Run review collection for supported source types")
    public CollectionJobResponseDto run(@PathVariable Long sourceId) {
        return collectionService.run(sourceId);
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CollectionJobResponseDto> findAllJobs() {
        return collectionService.findAllJobs();
    }

    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CollectionJobResponseDto findJobById(@PathVariable Long id) {
        return collectionService.findJobById(id);
    }
}
