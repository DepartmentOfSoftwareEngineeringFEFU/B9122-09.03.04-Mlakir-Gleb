package mlakir.aura.auth.utils;

import jakarta.servlet.http.*;
import lombok.*;
import mlakir.aura.auth.*;

@AllArgsConstructor
public class JwtClaimProvider {

    private final JwtParser jwtParser;

    public Long getUserId(HttpServletRequest request) {
        String token = JwtTokenExtractor.extractToken(request);
        return jwtParser.extractUserId(token);
    }

    public Long getUserId(String header) {
        String token = JwtTokenExtractor.extractToken(header);
        return jwtParser.extractUserId(token);
    }

}
