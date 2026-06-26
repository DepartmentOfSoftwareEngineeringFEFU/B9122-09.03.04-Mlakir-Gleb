package mlakir.aura.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import mlakir.aura.core.enums.CollectionMode;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.models.OrganizationEntity;
import mlakir.aura.core.models.SourceEntity;
import org.junit.jupiter.api.Test;

class ManualImportSourceCollectorTest {

    private final ManualImportSourceCollector collector = new ManualImportSourceCollector();

    @Test
    void shouldBuildCandidatesFromSourceDescriptionLines() {
        OrganizationEntity organization = new OrganizationEntity();
        organization.setId(7L);
        organization.setName("DVFU");
        organization.setShortName("DVFU");
        organization.setIsActive(true);

        SourceEntity source = new SourceEntity();
        source.setId(7L);
        source.setOrganization(organization);
        source.setName("Manual");
        source.setType(SourceType.MANUAL_IMPORT);
        source.setCollectionMode(CollectionMode.MANUAL);
        source.setBaseUrl("https://example.com/manual");
        source.setDescription("First review\nSecond review\nok");

        List<ReviewCandidate> candidates = collector.collect(source);

        assertEquals(2, candidates.size());
        assertFalse(candidates.get(0).externalId().isBlank());
        assertEquals("https://example.com/manual", candidates.get(0).originalUrl());
    }
}
