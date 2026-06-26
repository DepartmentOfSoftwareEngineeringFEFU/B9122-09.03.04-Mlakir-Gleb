package mlakir.aura.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import mlakir.aura.auth.config.AdminBootstrapProperties;
import mlakir.aura.auth.entity.CredentialEntity;
import mlakir.aura.auth.entity.RoleEntity;
import mlakir.aura.auth.entity.UserEntity;
import mlakir.aura.auth.repository.CredentialRepository;
import mlakir.aura.auth.repository.RoleRepository;
import mlakir.aura.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private AdminBootstrapProperties properties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @InjectMocks
    private AdminBootstrapService adminBootstrapService;

    @Test
    void shouldCreateAdminWhenBootstrapEnabled() throws Exception {
        RoleEntity adminRole = new RoleEntity();
        adminRole.setCode("ROLE_ADMIN");

        when(properties.isEnabled()).thenReturn(true);
        when(properties.getLogin()).thenReturn(" demo-admin ");
        when(properties.getPassword()).thenReturn(" demo123 ");
        when(userRepository.existsByLogin("demo-admin")).thenReturn(false);
        when(roleRepository.findByCode("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("demo-admin", userCaptor.getValue().getLogin());
        assertEquals(1, userCaptor.getValue().getRoles().size());

        ArgumentCaptor<CredentialEntity> credentialCaptor = ArgumentCaptor.forClass(CredentialEntity.class);
        verify(credentialRepository).save(credentialCaptor.capture());
        CredentialEntity savedCredential = credentialCaptor.getValue();
        assertEquals(1L, savedCredential.getUser().getId());
        assertNotNull(savedCredential.getSalt());
        assertNotNull(savedCredential.getHash());
        assertNotEquals("demo123", savedCredential.getHash());
    }

    @Test
    void shouldSkipBootstrapWhenAdminAlreadyExists() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getLogin()).thenReturn("demo-admin");
        when(properties.getPassword()).thenReturn("demo123");
        when(userRepository.existsByLogin("demo-admin")).thenReturn(true);

        adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).save(any(UserEntity.class));
        verify(credentialRepository, never()).save(any(CredentialEntity.class));
        verify(roleRepository, never()).findByCode(anyString());
    }

    @Test
    void shouldSkipBootstrapWhenCredentialsAreBlank() throws Exception {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getLogin()).thenReturn("   ");
        when(properties.getPassword()).thenReturn("");

        adminBootstrapService.run(new DefaultApplicationArguments(new String[0]));

        verify(userRepository, never()).existsByLogin(any());
        verify(userRepository, never()).save(any(UserEntity.class));
        verify(credentialRepository, never()).save(any(CredentialEntity.class));
    }
}
