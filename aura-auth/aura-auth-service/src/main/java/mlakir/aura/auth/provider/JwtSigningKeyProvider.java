package mlakir.aura.auth.provider;

import java.io.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.*;

import lombok.*;
import mlakir.aura.auth.config.*;
import org.springframework.stereotype.*;

@Getter
@Component
public class JwtSigningKeyProvider {

    private final RSAPrivateKey privateKey;

    public JwtSigningKeyProvider(JwtSigningConfig config) {
        this.privateKey = loadPrivateKey(config.getPrivateKey());
    }

    private static RSAPrivateKey loadPrivateKey(String key) {
        try {
            EncodedKeySpec spec = getPrivateKeySpec(key);
            return generatePrivateKey(spec);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Can`t generate RSA private key", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA private key", ex);
        }
    }

    private static KeyFactory getKeyFactory() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (Exception ex) {
            throw new IllegalStateException("RSA algorithm not available", ex);
        }
    }

    private static EncodedKeySpec getPrivateKeySpec(String keyValue) {
        var keyBytes = Base64.getDecoder().decode(keyValue);
        return new PKCS8EncodedKeySpec(keyBytes);
    }

    private static RSAPrivateKey generatePrivateKey(EncodedKeySpec keySpec) {
        try {
            return (RSAPrivateKey) getKeyFactory().generatePrivate(keySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("Can't generate private key", ex);
        }
    }

}
