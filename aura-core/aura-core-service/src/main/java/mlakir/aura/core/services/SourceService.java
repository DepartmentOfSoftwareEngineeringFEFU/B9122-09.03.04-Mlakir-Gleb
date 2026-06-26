package mlakir.aura.core.services;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mlakir.aura.core.dto.CreateSourceRequestDto;
import mlakir.aura.core.dto.SourceResponseDto;
import mlakir.aura.core.dto.UpdateSourceRequestDto;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.OrganizationExceptionFactory;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.core.mappers.SourceMapper;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.models.SourceEntity;
import mlakir.aura.core.repositories.SourceRepository;
import mlakir.aura.core.specifications.SourceSpecifications;
import mlakir.aura.core.services.tabiturient.SourceBaseUrlNormalizer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceService {

    private static final int MIN_SCHEDULE_INTERVAL_MINUTES = 15;
    private static final int MAX_SCHEDULE_INTERVAL_MINUTES = 43200;

    private final SourceRepository sourceRepository;
    private final SourceMapper sourceMapper;
    private final SourceExceptionFactory sourceExceptionFactory;
    private final OrganizationService organizationService;
    private final OrganizationExceptionFactory organizationExceptionFactory;
    private final SourceBaseUrlNormalizer sourceBaseUrlNormalizer;

    @Transactional
    public SourceResponseDto create(CreateSourceRequestDto requestDto) {
        OrganizationEntity organization = organizationService.getEntityOrThrow(requestDto.organizationId());
        validateOrganizationActive(organization);

        if (sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(
                organization.getId(), requestDto.name(), requestDto.type())) {
            throw sourceExceptionFactory.duplicateSource(requestDto.name(), requestDto.type());
        }

        SourceEntity entity = sourceMapper.toEntity(requestDto);
        entity.setOrganization(organization);
        entity.setBaseUrl(sourceBaseUrlNormalizer.normalize(requestDto.type(), requestDto.baseUrl()));
        applyCreateSchedule(requestDto, entity);
        return sourceMapper.toDto(sourceRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<SourceResponseDto> findAll(Long organizationId,
                                           String name,
                                           SourceType type,
                                           Boolean isActive,
                                           Boolean scheduleEnabled) {
        Specification<SourceEntity> specification = Specification
                .where(SourceSpecifications.withOrganizationFetch())
                .and(SourceSpecifications.organizationIdEquals(organizationId))
                .and(SourceSpecifications.nameContains(normalize(name)))
                .and(SourceSpecifications.typeEquals(type))
                .and(SourceSpecifications.isActiveEquals(isActive))
                .and(SourceSpecifications.scheduleEnabledEquals(scheduleEnabled));

        return sourceRepository.findAll(specification).stream()
                .map(sourceMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SourceResponseDto findById(Long sourceId) {
        return sourceMapper.toDto(getSourceOrThrow(sourceId));
    }

    @Transactional
    public SourceResponseDto update(Long sourceId, UpdateSourceRequestDto requestDto) {
        SourceEntity entity = getSourceOrThrow(sourceId);
        Long nextOrganizationId = requestDto.organizationId() == null
                ? entity.getOrganization().getId()
                : requestDto.organizationId();
        OrganizationEntity organization = organizationService.getEntityOrThrow(nextOrganizationId);
        validateOrganizationActive(organization);
        String nextBaseUrl = requestDto.baseUrl() == null ? entity.getBaseUrl() : requestDto.baseUrl();
        String normalizedBaseUrl = sourceBaseUrlNormalizer.normalize(entity.getType(), nextBaseUrl);

        String nextName = requestDto.name() == null ? entity.getName() : requestDto.name();
        boolean organizationChanged = !entity.getOrganization().getId().equals(nextOrganizationId);
        boolean nameChanged = !entity.getName().equalsIgnoreCase(nextName);
        if ((organizationChanged || nameChanged)
                && sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(
                nextOrganizationId, nextName, entity.getType())) {
            throw sourceExceptionFactory.duplicateSource(nextName, entity.getType());
        }

        boolean scheduleWasEnabled = Boolean.TRUE.equals(entity.getScheduleEnabled());
        Integer previousIntervalMinutes = entity.getScheduleIntervalMinutes();
        sourceMapper.updateEntity(requestDto, entity);
        entity.setOrganization(organization);
        entity.setBaseUrl(normalizedBaseUrl);
        applyUpdateSchedule(requestDto, entity, scheduleWasEnabled, previousIntervalMinutes);
        return sourceMapper.toDto(sourceRepository.save(entity));
    }

    @Transactional
    public void delete(Long sourceId) {
        SourceEntity entity = getSourceOrThrow(sourceId);
        entity.setIsActive(Boolean.FALSE);
        sourceRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public SourceEntity getSourceOrThrow(Long sourceId) {
        return sourceRepository.findById(sourceId)
                .orElseThrow(() -> sourceExceptionFactory.sourceNotFound(sourceId));
    }

    private void validateOrganizationActive(OrganizationEntity organization) {
        if (!Boolean.TRUE.equals(organization.getIsActive())) {
            throw organizationExceptionFactory.organizationInactive(organization.getId());
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void applyCreateSchedule(CreateSourceRequestDto requestDto, SourceEntity entity) {
        boolean scheduleEnabled = resolveCreateScheduleEnabled(requestDto);
        entity.setScheduleEnabled(scheduleEnabled);
        entity.setCollectionMode(resolveLegacyCollectionMode(
                requestDto.collectionMode(), scheduleEnabled, requestDto.scheduleEnabled() != null
        ));

        if (!scheduleEnabled) {
            disableSchedule(entity);
            return;
        }

        Integer intervalMinutes = resolveCreateInterval(requestDto);
        validateScheduleInterval(intervalMinutes);
        entity.setScheduleIntervalMinutes(intervalMinutes);
        entity.setNextCollectionAt(OffsetDateTime.now().plusMinutes(intervalMinutes));
    }

    private void applyUpdateSchedule(UpdateSourceRequestDto requestDto,
                                     SourceEntity entity,
                                     boolean scheduleWasEnabled,
                                     Integer previousIntervalMinutes) {
        boolean scheduleEnabled = resolveUpdateScheduleEnabled(requestDto, scheduleWasEnabled);
        entity.setScheduleEnabled(scheduleEnabled);
        entity.setCollectionMode(resolveLegacyCollectionMode(
                requestDto.collectionMode(), scheduleEnabled, requestDto.scheduleEnabled() != null
        ));

        if (!scheduleEnabled) {
            disableSchedule(entity);
            return;
        }

        Integer intervalMinutes = resolveUpdateInterval(requestDto, entity.getScheduleIntervalMinutes());
        validateScheduleInterval(intervalMinutes);

        boolean intervalChanged = requestDto.scheduleIntervalMinutes() != null
                && !requestDto.scheduleIntervalMinutes().equals(previousIntervalMinutes);
        boolean scheduleJustEnabled = !scheduleWasEnabled;
        entity.setScheduleIntervalMinutes(intervalMinutes);
        if (scheduleJustEnabled || intervalChanged || entity.getNextCollectionAt() == null) {
            entity.setNextCollectionAt(OffsetDateTime.now().plusMinutes(intervalMinutes));
        }
    }

    private boolean resolveCreateScheduleEnabled(CreateSourceRequestDto requestDto) {
        if (requestDto.scheduleEnabled() != null) {
            return Boolean.TRUE.equals(requestDto.scheduleEnabled());
        }
        return requestDto.collectionMode() == CollectionMode.SCHEDULED;
    }

    private Integer resolveCreateInterval(CreateSourceRequestDto requestDto) {
        if (requestDto.scheduleIntervalMinutes() != null) {
            return requestDto.scheduleIntervalMinutes();
        }
        if (requestDto.scheduleEnabled() == null && requestDto.collectionMode() == CollectionMode.SCHEDULED) {
            return 1440;
        }
        return null;
    }

    private boolean resolveUpdateScheduleEnabled(UpdateSourceRequestDto requestDto, boolean scheduleWasEnabled) {
        if (requestDto.scheduleEnabled() != null) {
            return Boolean.TRUE.equals(requestDto.scheduleEnabled());
        }
        if (requestDto.collectionMode() == CollectionMode.SCHEDULED) {
            return true;
        }
        if (requestDto.collectionMode() == CollectionMode.MANUAL) {
            return false;
        }
        return scheduleWasEnabled;
    }

    private Integer resolveUpdateInterval(UpdateSourceRequestDto requestDto, Integer currentIntervalMinutes) {
        if (requestDto.scheduleIntervalMinutes() != null) {
            return requestDto.scheduleIntervalMinutes();
        }
        if (requestDto.scheduleEnabled() == null
                && requestDto.collectionMode() == CollectionMode.SCHEDULED
                && currentIntervalMinutes == null) {
            return 1440;
        }
        return currentIntervalMinutes;
    }

    private CollectionMode resolveLegacyCollectionMode(CollectionMode requestedMode,
                                                       boolean scheduleEnabled,
                                                       boolean scheduleExplicitlySet) {
        if (scheduleExplicitlySet) {
            return scheduleEnabled ? CollectionMode.SCHEDULED : CollectionMode.MANUAL;
        }
        if (requestedMode != null) {
            return requestedMode;
        }
        return scheduleEnabled ? CollectionMode.SCHEDULED : CollectionMode.MANUAL;
    }

    private void disableSchedule(SourceEntity entity) {
        entity.setScheduleEnabled(Boolean.FALSE);
        entity.setScheduleIntervalMinutes(null);
        entity.setNextCollectionAt(null);
    }

    private void validateScheduleInterval(Integer intervalMinutes) {
        if (intervalMinutes == null
                || intervalMinutes < MIN_SCHEDULE_INTERVAL_MINUTES
                || intervalMinutes > MAX_SCHEDULE_INTERVAL_MINUTES) {
            throw sourceExceptionFactory.invalidScheduleInterval();
        }
    }
}
