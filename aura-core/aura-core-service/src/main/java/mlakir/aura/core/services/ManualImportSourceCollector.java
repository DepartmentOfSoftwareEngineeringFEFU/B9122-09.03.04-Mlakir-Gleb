package mlakir.aura.core.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import mlakir.aura.core.enums.SourceType;
import mlakir.aura.core.models.SourceEntity;
import org.springframework.stereotype.Component;

@Component
public class ManualImportSourceCollector implements SourceCollector {

    @Override
    public boolean supports(SourceType sourceType) {
        return sourceType == SourceType.MANUAL_IMPORT;
    }

    @Override
    public List<ReviewCandidate> collect(SourceEntity source) {
        List<String> texts = extractTexts(source);
        List<ReviewCandidate> candidates = new ArrayList<>(texts.size());
        for (int index = 0; index < texts.size(); index++) {
            String text = texts.get(index);
            candidates.add(new ReviewCandidate(
                    "manual:" + source.getId() + ":" + hash(text) + ":" + index,
                    text,
                    "manual-import",
                    null,
                    OffsetDateTime.now(),
                    source.getBaseUrl()
            ));
        }
        return candidates;
    }

    private List<String> extractTexts(SourceEntity source) {
        if (source.getDescription() == null || source.getDescription().isBlank()) {
            return List.of("Manual import review for source " + source.getName());
        }
        return source.getDescription().lines()
                .map(String::trim)
                .filter(line -> line.length() >= 3)
                .distinct()
                .toList();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                builder.append(String.format("%02x", bytes[i]));
            }
            return builder.toString();
        } catch (Exception exception) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
