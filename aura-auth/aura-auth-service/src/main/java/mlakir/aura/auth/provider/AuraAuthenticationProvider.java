package mlakir.aura.auth.provider;

import java.security.*;

import lombok.*;
import lombok.extern.slf4j.*;
import mlakir.aura.auth.*;
import mlakir.aura.auth.Principal;
import mlakir.aura.auth.entity.*;
import mlakir.aura.auth.repository.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.stereotype.*;

@Component
@RequiredArgsConstructor
public class AuraAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;

    private final CredentialRepository credentialRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String login = authentication.getName();
        String password = (String) authentication.getCredentials();

        UserEntity user = userRepository.findByLogin(login)
            .orElseThrow(AuthExceptionFabric::createInvalidCredentials);

        String salt = credentialRepository.findSaltByUserId(user.getId())
            .orElseThrow(AuthExceptionFabric::createInvalidCredentials);

        if (!credentialRepository.existsByUserIdAndHash(
            user.getId(), BCrypt.hashpw(password, salt))) {
            throw AuthExceptionFabric.createInvalidCredentials();
        }

        Principal principal = new Principal(user.getId());

        return new UsernamePasswordAuthenticationToken(principal, null, user.getRoles());

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}
