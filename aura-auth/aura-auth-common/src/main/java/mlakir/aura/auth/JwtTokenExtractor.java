package mlakir.aura.auth;

import jakarta.servlet.http.*;
import lombok.experimental.*;
import lombok.extern.slf4j.*;

@UtilityClass
public class JwtTokenExtractor {

    public static final String BEARER = "Bearer ";

    public static final String AUTH_HEADER = "Authorization";

    public static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);

        return extractToken(header);
    }

    public static String extractToken(String header) {
        if (header == null || header.isBlank()) {
            throw AuthExceptionFabric.createTokenNotFound();
        }

        if (header.startsWith(BEARER)) {
            if (header.length() == BEARER.length()) {
                throw AuthExceptionFabric.createTokenNotFound();
            }
            return header.substring(BEARER.length());
        }

        return header;
    }

}
