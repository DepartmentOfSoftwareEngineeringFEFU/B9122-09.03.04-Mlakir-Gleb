package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import mlakir.aura.core.dto.CreateSourceRequestDto;
import mlakir.aura.core.dto.OrganizationShortResponseDto;
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
import mlakir.aura.core.services.tabiturient.SourceBaseUrlNormalizer;
import mlakir.aura.exception.AuraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SourceServiceTest {

    @Mock
    private SourceRepository sourceRepository;
    @Mock
    private SourceMapper sourceMapper;
    @Mock
    private OrganizationService organizationService;

    private SourceService sourceService;

    @BeforeEach
    void setUp() {
        sourceService = new SourceService(
                sourceRepository,
                sourceMapper,
                new SourceExceptionFactory(),
                organizationService,
                new OrganizationExceptionFactory(),
                new SourceBaseUrlNormalizer(new SourceExceptionFactory())
        );
    }

    @Test
    void shouldCreateSourceWithValidOrganization() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Импорт отзывов ДВФУ",
                SourceType.MANUAL_IMPORT,
                "https://example.com/manual",
                CollectionMode.MANUAL,
                false,
                null,
                "CSV import"
        );
        OrganizationEntity organization = organization(true);
        SourceEntity entity = source(organization);
        SourceResponseDto response = response();

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization);
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(entity);
        when(sourceRepository.save(entity)).thenReturn(entity);
        when(sourceMapper.toDto(entity)).thenReturn(response);

        SourceResponseDto result = sourceService.create(request);

        assertEquals(1L, result.organization().id());
        assertEquals("ДВФУ", result.organization().shortName());
    }

    @Test
    void shouldFilterSources() {
        OrganizationEntity organization = organization(true);
        SourceEntity entity = source(organization);
        SourceResponseDto response = response();

        when(sourceRepository.findAll(any(Specification.class))).thenReturn(List.of(entity));
        when(sourceMapper.toDto(entity)).thenReturn(response);

        List<SourceResponseDto> result = sourceService.findAll(
                1L,
                "  ДВФУ  ",
                SourceType.MANUAL_IMPORT,
                true,
                false
        );

        assertEquals(1, result.size());
        assertEquals(SourceType.MANUAL_IMPORT, result.getFirst().type());
    }

    @Test
    void shouldNormalizeTabiturientUrlToHttps() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Tabiturient о ДВФУ",
                SourceType.TABITURIENT,
                "http://tabiturient.ru/vuzu/dvfu",
                CollectionMode.MANUAL,
                false,
                null,
                "Tabiturient scraping"
        );
        OrganizationEntity organization = organization(true);
        SourceEntity entity = source(organization);
        entity.setType(SourceType.TABITURIENT);
        entity.setBaseUrl("http://tabiturient.ru/vuzu/dvfu");
        SourceResponseDto response = new SourceResponseDto(
                1L,
                new OrganizationShortResponseDto(1L, "Дальневосточный федеральный университет", "ДВФУ"),
                "Отзывы Tabiturient о ДВФУ",
                SourceType.TABITURIENT,
                "https://tabiturient.ru/vuzu/dvfu/",
                true,
                CollectionMode.MANUAL,
                false,
                null,
                null,
                null,
                "Tabiturient scraping",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now()
        );

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization);
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(entity);
        when(sourceRepository.save(entity)).thenReturn(entity);
        when(sourceMapper.toDto(entity)).thenReturn(response);

        SourceResponseDto result = sourceService.create(request);

        assertEquals("https://tabiturient.ru/vuzu/dvfu/", entity.getBaseUrl());
        assertEquals("https://tabiturient.ru/vuzu/dvfu/", result.baseUrl());
    }

    @Test
    void shouldFailWhenTabiturientUrlIsInvalid() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Tabiturient о ДВФУ",
                SourceType.TABITURIENT,
                "https://example.com/vuzu/dvfu/",
                CollectionMode.MANUAL,
                false,
                null,
                "Tabiturient scraping"
        );

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization(true));
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(new SourceEntity());

        assertThrows(AuraException.class, () -> sourceService.create(request));
    }

    @Test
    void shouldNormalizeOtzovikUrl() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Otzovik о ДВФУ",
                SourceType.OTZOVIK,
                "http://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu",
                CollectionMode.MANUAL,
                false,
                null,
                "Otzovik scraping"
        );
        OrganizationEntity organization = organization(true);
        SourceEntity entity = source(organization);
        entity.setType(SourceType.OTZOVIK);
        entity.setBaseUrl(request.baseUrl());

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization);
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(entity);
        when(sourceRepository.save(entity)).thenReturn(entity);
        when(sourceMapper.toDto(entity)).thenReturn(response());

        sourceService.create(request);

        assertEquals("https://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/", entity.getBaseUrl());
    }

    @Test
    void shouldFailWhenOtzovikUrlIsInvalid() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Otzovik о ДВФУ",
                SourceType.OTZOVIK,
                "https://example.com/reviews/dvfu/",
                CollectionMode.MANUAL,
                false,
                null,
                "Otzovik scraping"
        );

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization(true));
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(new SourceEntity());

        assertThrows(AuraException.class, () -> sourceService.create(request));
    }

    @Test
    void shouldNormalizeVuzopediaUrl() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Vuzopedia о ДВФУ",
                SourceType.VUZOPEDIA,
                "http://vuzopedia.ru/vuz/3281/otziv/",
                CollectionMode.MANUAL,
                false,
                null,
                "Vuzopedia scraping"
        );
        OrganizationEntity organization = organization(true);
        SourceEntity entity = source(organization);
        entity.setType(SourceType.VUZOPEDIA);
        entity.setBaseUrl(request.baseUrl());

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization);
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(entity);
        when(sourceRepository.save(entity)).thenReturn(entity);
        when(sourceMapper.toDto(entity)).thenReturn(response());

        sourceService.create(request);

        assertEquals("https://vuzopedia.ru/vuz/3281/otziv", entity.getBaseUrl());
    }

    @Test
    void shouldFailWhenVuzopediaUrlIsInvalid() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Vuzopedia о ДВФУ",
                SourceType.VUZOPEDIA,
                "https://vuzopedia.ru/vuz/dvfu/otziv",
                CollectionMode.MANUAL,
                false,
                null,
                "Vuzopedia scraping"
        );

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization(true));
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(new SourceEntity());

        assertThrows(AuraException.class, () -> sourceService.create(request));
    }

    @Test
    void shouldFailWhenOrganizationIsInactive() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Импорт отзывов ДВФУ",
                SourceType.MANUAL_IMPORT,
                "https://example.com/manual",
                CollectionMode.MANUAL,
                false,
                null,
                "CSV import"
        );
        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization(false));

        assertThrows(AuraException.class, () -> sourceService.create(request));
    }

    @Test
    void shouldFailWhenOrganizationDoesNotExist() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                99L,
                "Импорт отзывов ДВФУ",
                SourceType.MANUAL_IMPORT,
                "https://example.com/manual",
                CollectionMode.MANUAL,
                false,
                null,
                "CSV import"
        );
        when(organizationService.getEntityOrThrow(99L))
                .thenThrow(new OrganizationExceptionFactory().organizationNotFound(99L));

        assertThrows(AuraException.class, () -> sourceService.create(request));
    }

    @Test
    void shouldCreateScheduledSourceWithNextCollectionAt() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Tabiturient о ДВФУ",
                SourceType.TABITURIENT,
                "https://tabiturient.ru/vuzu/dvfu/",
                CollectionMode.MANUAL,
                true,
                1440,
                "Tabiturient scraping"
        );
        OrganizationEntity organization = organization(true);
        SourceEntity entity = source(organization);
        entity.setType(SourceType.TABITURIENT);

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization);
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(entity);
        when(sourceRepository.save(entity)).thenReturn(entity);
        when(sourceMapper.toDto(entity)).thenReturn(response());

        sourceService.create(request);

        assertEquals(true, entity.getScheduleEnabled());
        assertEquals(1440, entity.getScheduleIntervalMinutes());
        org.junit.jupiter.api.Assertions.assertNotNull(entity.getNextCollectionAt());
    }

    @Test
    void shouldRejectScheduledSourceWithoutInterval() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Tabiturient о ДВФУ",
                SourceType.TABITURIENT,
                "https://tabiturient.ru/vuzu/dvfu/",
                CollectionMode.MANUAL,
                true,
                null,
                "Tabiturient scraping"
        );

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization(true));
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(source(organization(true)));

        assertThrows(AuraException.class, () -> sourceService.create(request));
    }

    @Test
    void shouldRejectScheduledSourceWithIntervalBelowMinimum() {
        CreateSourceRequestDto request = new CreateSourceRequestDto(
                1L,
                "Отзывы Tabiturient о ДВФУ",
                SourceType.TABITURIENT,
                "https://tabiturient.ru/vuzu/dvfu/",
                CollectionMode.MANUAL,
                true,
                14,
                "Tabiturient scraping"
        );

        when(organizationService.getEntityOrThrow(1L)).thenReturn(organization(true));
        when(sourceRepository.existsByOrganizationIdAndNameIgnoreCaseAndType(1L, request.name(), request.type()))
                .thenReturn(false);
        when(sourceMapper.toEntity(request)).thenReturn(source(organization(true)));

        assertThrows(AuraException.class, () -> sourceService.create(request));
    }

    @Test
    void shouldClearScheduleWhenDisabled() {
        OrganizationEntity organization = organization(true);
        SourceEntity entity = source(organization);
        entity.setScheduleEnabled(true);
        entity.setScheduleIntervalMinutes(1440);
        entity.setNextCollectionAt(OffsetDateTime.now().plusDays(1));
        UpdateSourceRequestDto request = new UpdateSourceRequestDto(
                null, null, null, null, null, false, null, null
        );

        when(sourceRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        when(organizationService.getEntityOrThrow(organization.getId())).thenReturn(organization);
        when(sourceRepository.save(entity)).thenReturn(entity);
        when(sourceMapper.toDto(entity)).thenReturn(response());

        sourceService.update(1L, request);

        assertEquals(false, entity.getScheduleEnabled());
        assertEquals(null, entity.getScheduleIntervalMinutes());
        assertEquals(null, entity.getNextCollectionAt());
    }

    @Test
    void shouldSetNextCollectionAtWhenScheduleEnabled() {
        OrganizationEntity organization = organization(true);
        SourceEntity entity = source(organization);
        UpdateSourceRequestDto request = new UpdateSourceRequestDto(
                null, null, null, null, null, true, 60, null
        );

        when(sourceRepository.findById(1L)).thenReturn(java.util.Optional.of(entity));
        when(organizationService.getEntityOrThrow(organization.getId())).thenReturn(organization);
        when(sourceRepository.save(entity)).thenReturn(entity);
        when(sourceMapper.toDto(entity)).thenReturn(response());

        sourceService.update(1L, request);

        assertEquals(true, entity.getScheduleEnabled());
        assertEquals(60, entity.getScheduleIntervalMinutes());
        org.junit.jupiter.api.Assertions.assertNotNull(entity.getNextCollectionAt());
    }

    private SourceEntity source(OrganizationEntity organization) {
        SourceEntity entity = new SourceEntity();
        entity.setId(1L);
        entity.setOrganization(organization);
        entity.setName("Импорт отзывов ДВФУ");
        entity.setType(SourceType.MANUAL_IMPORT);
        entity.setBaseUrl("https://example.com/manual");
        entity.setIsActive(true);
        entity.setCollectionMode(CollectionMode.MANUAL);
        entity.setScheduleEnabled(false);
        entity.setDescription("CSV import");
        entity.setCreatedAt(OffsetDateTime.now().minusDays(1));
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }

    private SourceResponseDto response() {
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

    private OrganizationEntity organization(boolean active) {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(1L);
        organization.setName("Дальневосточный федеральный университет");
        organization.setShortName("ДВФУ");
        organization.setIsActive(active);
        return organization;
    }
}
