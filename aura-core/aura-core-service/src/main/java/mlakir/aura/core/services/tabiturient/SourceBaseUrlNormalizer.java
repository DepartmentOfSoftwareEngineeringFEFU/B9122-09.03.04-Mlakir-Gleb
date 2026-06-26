package mlakir.aura.core.services.tabiturient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.exceptions.SourceExceptionFactory;
import org.springframework.stereotype.Component;

@Component
public class SourceBaseUrlNormalizer {

    private static final Pattern TABITURIENT_PATH = Pattern.compile("^/vuzu/[A-Za-z0-9_-]+/?$");
    private static final Pattern TABITURIENT_VUZ_ID = Pattern.compile("^/vuzu/([A-Za-z0-9_-]+)/?$");
    private static final Pattern VUZOPEDIA_PATH = Pattern.compile("^/vuz/(\\d+)/otziv/?$");
    private static final String OTZOVIK_HOST = "otzovik.com";
    private static final String VUZOPEDIA_HOST = "vuzopedia.ru";

    private final SourceExceptionFactory sourceExceptionFactory;

    public SourceBaseUrlNormalizer(SourceExceptionFactory sourceExceptionFactory) {
        this.sourceExceptionFactory = sourceExceptionFactory;
    }

    public String normalize(SourceType sourceType, String baseUrl) {
        if (sourceType == SourceType.TABITURIENT) {
            return normalizeTabiturientUrl(baseUrl);
        }
        if (sourceType == SourceType.OTZOVIK) {
            return normalizeOtzovikUrl(baseUrl);
        }
        if (sourceType == SourceType.VUZOPEDIA) {
            return normalizeVuzopediaUrl(baseUrl);
        }
        return baseUrl;
    }

    public String normalizeTabiturientUrl(String baseUrl) {
        try {
            URI uri = new URI(baseUrl == null ? "" : baseUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();

            if (scheme == null || host == null || path == null) {
                throw sourceExceptionFactory.invalidTabiturientUrl(baseUrl);
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw sourceExceptionFactory.invalidTabiturientUrl(baseUrl);
            }
            if (!"tabiturient.ru".equalsIgnoreCase(host)) {
                throw sourceExceptionFactory.invalidTabiturientUrl(baseUrl);
            }
            if (!TABITURIENT_PATH.matcher(path).matches()) {
                throw sourceExceptionFactory.invalidTabiturientUrl(baseUrl);
            }

            String normalizedPath = path.endsWith("/") ? path : path + "/";
            return "https://tabiturient.ru" + normalizedPath;
        } catch (IllegalArgumentException | URISyntaxException exception) {
            throw sourceExceptionFactory.invalidTabiturientUrl(baseUrl);
        }
    }

    public String extractTabiturientVuzId(String baseUrl) {
        String normalizedUrl = normalizeTabiturientUrl(baseUrl);
        try {
            URI uri = new URI(normalizedUrl);
            Matcher matcher = TABITURIENT_VUZ_ID.matcher(uri.getPath());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        } catch (URISyntaxException ignored) {
            // normalizeTabiturientUrl already validated the URL, so this should never happen.
        }
        throw sourceExceptionFactory.invalidTabiturientUrl(baseUrl);
    }

    public String normalizeOtzovikUrl(String baseUrl) {
        try {
            URI uri = new URI(baseUrl == null ? "" : baseUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();

            if (scheme == null || host == null || path == null) {
                throw sourceExceptionFactory.invalidOtzovikUrl(baseUrl);
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw sourceExceptionFactory.invalidOtzovikUrl(baseUrl);
            }
            String normalizedHost = host.toLowerCase();
            if (!normalizedHost.equals(OTZOVIK_HOST) && !normalizedHost.endsWith("." + OTZOVIK_HOST)) {
                throw sourceExceptionFactory.invalidOtzovikUrl(baseUrl);
            }
            if (!path.startsWith("/reviews/")) {
                throw sourceExceptionFactory.invalidOtzovikUrl(baseUrl);
            }

            String normalizedPath = path.endsWith("/") ? path : path + "/";
            return "https://" + OTZOVIK_HOST + normalizedPath;
        } catch (IllegalArgumentException | URISyntaxException exception) {
            throw sourceExceptionFactory.invalidOtzovikUrl(baseUrl);
        }
    }

    public String normalizeVuzopediaUrl(String baseUrl) {
        try {
            URI uri = new URI(baseUrl == null ? "" : baseUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            String path = uri.getPath();

            if (scheme == null || host == null || path == null) {
                throw sourceExceptionFactory.invalidVuzopediaUrl(baseUrl);
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw sourceExceptionFactory.invalidVuzopediaUrl(baseUrl);
            }
            String normalizedHost = host.toLowerCase();
            if (!normalizedHost.equals(VUZOPEDIA_HOST) && !normalizedHost.endsWith("." + VUZOPEDIA_HOST)) {
                throw sourceExceptionFactory.invalidVuzopediaUrl(baseUrl);
            }
            Matcher matcher = VUZOPEDIA_PATH.matcher(path);
            if (!matcher.matches()) {
                throw sourceExceptionFactory.invalidVuzopediaUrl(baseUrl);
            }

            return "https://" + VUZOPEDIA_HOST + path.replaceAll("/$", "");
        } catch (IllegalArgumentException | URISyntaxException exception) {
            throw sourceExceptionFactory.invalidVuzopediaUrl(baseUrl);
        }
    }

    public String extractVuzopediaVuzId(String baseUrl) {
        String normalizedUrl = normalizeVuzopediaUrl(baseUrl);
        try {
            URI uri = new URI(normalizedUrl);
            Matcher matcher = VUZOPEDIA_PATH.matcher(uri.getPath());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        } catch (URISyntaxException ignored) {
            // normalizeVuzopediaUrl already validated the URL.
        }
        throw sourceExceptionFactory.invalidVuzopediaUrl(baseUrl);
    }
}
