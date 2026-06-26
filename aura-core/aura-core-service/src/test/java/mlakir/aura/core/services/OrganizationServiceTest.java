package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.dto.CreateOrganizationRequestDto;
import mlakir.aura.core.dto.OrganizationResponseDto;
import mlakir.aura.core.dto.UpdateOrganizationRequestDto;
import mlakir.aura.core.exceptions.OrganizationExceptionFactory;
import mlakir.aura.core.mappers.OrganizationMapper;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.repositories.OrganizationRepository;
import mlakir.aura.exception.AuraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private OrganizationMapper organizationMapper;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(
                organizationRepository,
                organizationMapper,
                new OrganizationExceptionFactory()
        );
    }

    @Test
    void shouldCreateOrganization() {
        CreateOrganizationRequestDto request = new CreateOrganizationRequestDto(
                "Дальневосточный федеральный университет",
                "ДВФУ",
                "Federal university",
                "https://www.dvfu.ru"
        );
        OrganizationEntity entity = organization();
        OrganizationResponseDto response = response();

        when(organizationRepository.existsByNameIgnoreCase(request.name())).thenReturn(false);
        when(organizationRepository.existsByShortNameIgnoreCase(request.shortName())).thenReturn(false);
        when(organizationMapper.toEntity(request)).thenReturn(entity);
        when(organizationRepository.save(entity)).thenReturn(entity);
        when(organizationMapper.toDto(entity)).thenReturn(response);

        OrganizationResponseDto result = organizationService.create(request);

        assertEquals(1L, result.id());
        assertEquals("ДВФУ", result.shortName());
    }

    @Test
    void shouldListOrganizations() {
        OrganizationEntity entity = organization();
        OrganizationResponseDto response = response();

        when(organizationRepository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(organizationMapper.toDto(entity)).thenReturn(response);

        List<OrganizationResponseDto> result = organizationService.findAll(null, null);

        assertEquals(1, result.size());
        assertEquals("ДВФУ", result.getFirst().shortName());
    }

    @Test
    void shouldFilterOrganizationsByNameAndActiveFlag() {
        OrganizationEntity entity = organization();
        OrganizationResponseDto response = response();

        when(organizationRepository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(organizationMapper.toDto(entity)).thenReturn(response);

        List<OrganizationResponseDto> result = organizationService.findAll("  ДВФУ  ", true);

        assertEquals(1, result.size());
        assertEquals("ДВФУ", result.getFirst().shortName());
    }

    @Test
    void shouldUpdateOrganization() {
        OrganizationEntity entity = organization();
        UpdateOrganizationRequestDto request = new UpdateOrganizationRequestDto(
                "Дальневосточный федеральный университет",
                "DVFU",
                "Updated",
                "https://www.dvfu.ru",
                true
        );
        OrganizationResponseDto response = new OrganizationResponseDto(
                1L,
                "Дальневосточный федеральный университет",
                "DVFU",
                "Updated",
                "https://www.dvfu.ru",
                true,
                entity.getCreatedAt(),
                OffsetDateTime.now()
        );

        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        when(organizationRepository.existsByNameIgnoreCaseAndIdNot(request.name(), 1L)).thenReturn(false);
        when(organizationRepository.existsByShortNameIgnoreCaseAndIdNot(request.shortName(), 1L)).thenReturn(false);
        when(organizationRepository.save(entity)).thenReturn(entity);
        when(organizationMapper.toDto(entity)).thenReturn(response);

        OrganizationResponseDto result = organizationService.update(1L, request);

        assertEquals("DVFU", result.shortName());
    }

    @Test
    void shouldSoftDeleteOrganization() {
        OrganizationEntity entity = organization();
        when(organizationRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        when(organizationRepository.save(entity)).thenReturn(entity);

        organizationService.delete(1L);

        assertFalse(entity.getIsActive());
    }

    @Test
    void shouldFailWhenOrganizationNameAlreadyExists() {
        CreateOrganizationRequestDto request = new CreateOrganizationRequestDto("DVFU", "FEFU", null, null);
        when(organizationRepository.existsByNameIgnoreCase("DVFU")).thenReturn(true);

        assertThrows(AuraException.class, () -> organizationService.create(request));
    }

    private OrganizationEntity organization() {
        OrganizationEntity entity = new OrganizationEntity();
        entity.setId(1L);
        entity.setName("Дальневосточный федеральный университет");
        entity.setShortName("ДВФУ");
        entity.setDescription("Federal university");
        entity.setWebsite("https://www.dvfu.ru");
        entity.setIsActive(true);
        entity.setCreatedAt(OffsetDateTime.now().minusDays(1));
        entity.setUpdatedAt(OffsetDateTime.now().minusHours(1));
        return entity;
    }

    private OrganizationResponseDto response() {
        OrganizationEntity entity = organization();
        return new OrganizationResponseDto(
                entity.getId(),
                entity.getName(),
                entity.getShortName(),
                entity.getDescription(),
                entity.getWebsite(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
