package mlakir.aura.auth.service;

import java.time.*;
import java.util.*;

import lombok.*;
import mlakir.aura.auth.*;
import mlakir.aura.auth.config.*;
import mlakir.aura.auth.dto.*;
import mlakir.aura.auth.entity.*;
import mlakir.aura.auth.mapper.*;
import mlakir.aura.auth.provider.*;
import mlakir.aura.auth.repository.*;
import mlakir.aura.auth.utils.*;
import mlakir.aura.auth.validator.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtSigningConfig config;

    private final AuthenticationManager authenticationManager;

    private final JwtProvider jwtProvider;

    private final AuthMapper authMapper;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final CredentialRepository credentialRepository;

    private final TokenRepository tokenRepository;

    private final JwtParser jwtParser;

    private final JwtRefreshTokenValidator refreshTokenValidator;

    private final JwtAccessTokenValidator accessTokenValidator;

    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        UserEntity user = userRepository.findByLogin(request.getLogin())
            .orElseThrow(AuthExceptionFabric::createInvalidCredentials);

        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getLogin(),
                request.getPassword()
            )
        );

        return issueTokens(user);
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw AuthExceptionFabric.createRegistrationDuplicate();
        }

        RoleEntity defaultRole = roleRepository.findByCode(config.getDefaultRole())
            .orElseThrow(AuthExceptionFabric::createDefaultRoleNotFound);

        UserEntity user = authMapper.toUserEntity(request, defaultRole);
        UserEntity savedUser = userRepository.save(user);

        String salt = BCrypt.gensalt();
        CredentialEntity credential = authMapper.toCredentialEntity(
            savedUser,
            salt,
            BCrypt.hashpw(request.getPassword(), salt)
        );
        credentialRepository.save(credential);

        return issueTokens(savedUser);
    }

    @Transactional
    public AuthResponseDto refresh(TokenRequestDto request) {
        String token = JwtTokenExtractor.extractToken(request.getToken());
        Long userId = jwtParser.extractUserId(token);

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(AuthExceptionFabric::createInvalidToken);

        if (!refreshTokenValidator.isValid(token, user)) {
            throw AuthExceptionFabric.createInvalidToken();
        }

        tokenRepository.softDeleteByRefreshJti(jwtParser.extractJti(token), Instant.now());

        return issueTokens(user);
    }

    @Transactional
    public void logout(String authorizationHeader) {
        String token = JwtTokenExtractor.extractToken(authorizationHeader);

        if (!accessTokenValidator.isValid(token)) {
            throw AuthExceptionFabric.createInvalidToken();
        }

        Long userId = currentUserId();
        if (!Objects.equals(jwtParser.extractUserId(token), userId)) {
            throw AuthExceptionFabric.createAccessDenied();
        }

        UUID accessJti = jwtParser.extractJti(token);
        tokenRepository.softDeleteByAccessJti(accessJti, Instant.now());
    }

    @Transactional
    public void logoutAll() {
        tokenRepository.softDeleteByUserId(currentUserId(), Instant.now());
    }

    @Transactional
    public void deleteExpiredTokens() {
        tokenRepository.deleteAllExpired(Instant.now());
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Principal principal)) {
            throw AuthExceptionFabric.createAccessDenied();
        }

        return principal.getId();
    }

    private AuthResponseDto issueTokens(UserEntity user) {
        UUID accessJti = UUID.randomUUID();
        UUID refreshJti = UUID.randomUUID();
        String accessToken = jwtProvider.generateAccessToken(user, accessJti);
        String refreshToken = jwtProvider.generateRefreshToken(user, refreshJti);

        saveUserToken(accessJti, refreshJti, user);

        return authMapper.toAuthResponseDto(accessToken, refreshToken);
    }

    private void saveUserToken(UUID accessJti, UUID refreshJti, UserEntity user) {
        Instant now = Instant.now();
        TokenEntity token = authMapper.toTokenEntity(
            accessJti,
            refreshJti,
            user,
            now,
            now.plusSeconds(config.getRefreshTokenExpirationSeconds())
        );

        tokenRepository.save(token);
    }

}
