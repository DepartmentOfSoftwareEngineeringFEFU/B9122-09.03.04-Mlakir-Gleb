package mlakir.aura.auth.filter;

import java.io.*;
import java.util.*;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.*;
import lombok.extern.slf4j.*;
import mlakir.aura.auth.*;
import mlakir.aura.auth.handler.*;
import mlakir.aura.auth.utils.*;
import mlakir.aura.auth.validator.*;
import mlakir.aura.exception.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.*;
import org.springframework.security.core.context.*;
import org.springframework.web.filter.*;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtAccessTokenValidator validator;

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    private final JwtParser jwtParser;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(JwtTokenExtractor.AUTH_HEADER);

        if (header == null || !header.startsWith(JwtTokenExtractor.BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(JwtTokenExtractor.BEARER.length());

        try {
            if (!validator.isValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }
        } catch (AuraException ex) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                request,
                response,
                new AuthenticationServiceException(ex.getMessage(), ex)
            );
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        Principal principal = new Principal(jwtParser.extractUserId(token));
        List<SimpleGrantedAuthority> authorities = jwtParser.extractAuthorities(token);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.endsWith("/refresh");
    }

}
