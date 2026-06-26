package mlakir.aura.core.services.tabiturient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import mlakir.aura.core.exceptions.SourceExceptionFactory;
import mlakir.aura.exception.AuraException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourceBaseUrlNormalizerTest {

    private SourceBaseUrlNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new SourceBaseUrlNormalizer(new SourceExceptionFactory());
    }

    @Test
    void shouldExtractVuzIdFromTabiturientUrl() {
        assertEquals("dvfu", normalizer.extractTabiturientVuzId("https://tabiturient.ru/vuzu/dvfu/"));
        assertEquals("spbgu", normalizer.extractTabiturientVuzId("https://tabiturient.ru/vuzu/spbgu/"));
    }

    @Test
    void shouldRejectInvalidUrlWhenExtractingVuzId() {
        assertThrows(AuraException.class, () -> normalizer.extractTabiturientVuzId("https://example.com/vuzu/dvfu/"));
    }

    @Test
    void shouldNormalizeOtzovikReviewsUrl() {
        assertEquals(
                "https://otzovik.com/reviews/dvfu/",
                normalizer.normalizeOtzovikUrl("http://otzovik.com/reviews/dvfu")
        );
    }

    @Test
    void shouldRejectInvalidOtzovikUrl() {
        assertThrows(AuraException.class, () -> normalizer.normalizeOtzovikUrl("https://example.com/reviews/dvfu/"));
        assertThrows(AuraException.class, () -> normalizer.normalizeOtzovikUrl("https://otzovik.com/company/dvfu/"));
    }

    @Test
    void shouldNormalizeVuzopediaUrlAndExtractVuzId() {
        assertEquals(
                "https://vuzopedia.ru/vuz/3281/otziv",
                normalizer.normalizeVuzopediaUrl("http://vuzopedia.ru/vuz/3281/otziv/")
        );
        assertEquals("3281", normalizer.extractVuzopediaVuzId("https://vuzopedia.ru/vuz/3281/otziv"));
    }

    @Test
    void shouldRejectInvalidVuzopediaUrl() {
        assertThrows(AuraException.class, () -> normalizer.normalizeVuzopediaUrl("https://example.com/vuz/3281/otziv"));
        assertThrows(AuraException.class, () -> normalizer.normalizeVuzopediaUrl("https://vuzopedia.ru/vuz/abc/otziv"));
    }
}
