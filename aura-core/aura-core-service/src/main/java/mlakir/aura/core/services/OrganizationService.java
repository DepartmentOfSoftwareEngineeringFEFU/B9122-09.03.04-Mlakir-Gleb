package mlakir.aura.core.services;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.CreateOrganizationRequestDto;
import mlakir.aura.core.dto.OrganizationResponseDto;
import mlakir.aura.core.dto.UpdateOrganizationRequestDto;
import mlakir.aura.core.exceptions.OrganizationExceptionFactory;
import mlakir.aura.core.mappers.OrganizationMapper;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.repositories.OrganizationRepository;
import mlakir.aura.core.specifications.OrganizationSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final OrganizationExceptionFactory organizationExceptionFactory;

    @Transactional
    public OrganizationResponseDto create(CreateOrganizationRequestDto requestDto) {
        validateCreate(requestDto.name(), requestDto.shortName());

        OrganizationEntity entity = organizationMapper.toEntity(requestDto);
        return organizationMapper.toDto(organizationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponseDto> findAll(String name, Boolean isActive) {
        Specification<OrganizationEntity> specification = Specification
                .where(OrganizationSpecifications.orderedByName())
                .and(OrganizationSpecifications.nameContains(normalize(name)))
                .and(OrganizationSpecifications.isActiveEquals(isActive));

        return organizationRepository.findAll(specification).stream()
                .map(organizationMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponseDto findById(Long organizationId) {
        return organizationMapper.toDto(getEntityOrThrow(organizationId));
    }

    @Transactional
    public OrganizationResponseDto update(Long organizationId, UpdateOrganizationRequestDto requestDto) {
        OrganizationEntity entity = getEntityOrThrow(organizationId);

        String nextName = requestDto.name() == null ? entity.getName() : requestDto.name();
        String nextShortName = requestDto.shortName() == null ? entity.getShortName() : requestDto.shortName();

        if (organizationRepository.existsByNameIgnoreCaseAndIdNot(nextName, organizationId)) {
            throw organizationExceptionFactory.organizationAlreadyExists(nextName);
        }
        if (organizationRepository.existsByShortNameIgnoreCaseAndIdNot(nextShortName, organizationId)) {
            throw organizationExceptionFactory.organizationShortNameAlreadyExists(nextShortName);
        }

        organizationMapper.updateEntity(requestDto, entity);
        return organizationMapper.toDto(organizationRepository.save(entity));
    }

    @Transactional
    public void delete(Long organizationId) {
        OrganizationEntity entity = getEntityOrThrow(organizationId);
        entity.setIsActive(Boolean.FALSE);
        organizationRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public OrganizationEntity getEntityOrThrow(Long organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> organizationExceptionFactory.organizationNotFound(organizationId));
    }

    private void validateCreate(String name, String shortName) {
        if (organizationRepository.existsByNameIgnoreCase(name)) {
            throw organizationExceptionFactory.organizationAlreadyExists(name);
        }
        if (organizationRepository.existsByShortNameIgnoreCase(shortName)) {
            throw organizationExceptionFactory.organizationShortNameAlreadyExists(shortName);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
