package mlakir.aura.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mlakir.aura.auth.config.AdminBootstrapProperties;
import mlakir.aura.auth.entity.CredentialEntity;
import mlakir.aura.auth.entity.RoleEntity;
import mlakir.aura.auth.entity.UserEntity;
import mlakir.aura.auth.repository.CredentialRepository;
import mlakir.aura.auth.repository.RoleRepository;
import mlakir.aura.auth.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapService implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialRepository credentialRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        String login = trim(properties.getLogin());
        String password = trim(properties.getPassword());
        if (login == null || password == null) {
            log.warn("Admin bootstrap is enabled, but login or password is empty. Skipping seed.");
            return;
        }

        RoleEntity adminRole = roleRepository.findByCode("ROLE_ADMIN")
            .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN was not found"));

        UserEntity existingUser = userRepository.findByLogin(login).orElse(null);
        if (existingUser != null) {
            ensureAdminRole(existingUser, adminRole);
            return;
        }

        UserEntity user = new UserEntity();
        user.setLogin(login);
        user.getRoles().add(adminRole);
        UserEntity savedUser = userRepository.save(user);

        String salt = BCrypt.gensalt();
        CredentialEntity credential = new CredentialEntity();
        credential.setUser(savedUser);
        credential.setSalt(salt);
        credential.setHash(BCrypt.hashpw(password, salt));
        credentialRepository.save(credential);

        log.info("Bootstrap admin '{}' has been created.", login);
    }

    private void ensureAdminRole(UserEntity user, RoleEntity adminRole) {
        if (user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getCode()))) {
            log.info("Admin bootstrap skipped: user '{}' already has ROLE_ADMIN.", user.getLogin());
            return;
        }

        user.getRoles().add(adminRole);
        userRepository.save(user);
        log.info("Admin bootstrap granted ROLE_ADMIN to existing user '{}'.", user.getLogin());
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
