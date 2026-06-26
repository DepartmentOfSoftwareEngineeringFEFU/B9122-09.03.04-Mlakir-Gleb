package mlakir.aura.auth.utils;

import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.*;

import mlakir.aura.auth.config.*;

public record JwtVerificationKeyProvider(RSAPublicKey publicKey) {

    public JwtVerificationKeyProvider(JwtVerificationConfig publicKey) {
        this(loadPublicKey(publicKey.getPublicKey()));
    }

    private static RSAPublicKey loadPublicKey(String key) {
        try {
            EncodedKeySpec spec = getPublicKeySpec(key);
            return generatePublicKey(spec);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Can`t generate RSA public key", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA public key", ex);
        }
    }

    private static KeyFactory getKeyFactory() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (Exception ex) {
            throw new IllegalStateException("RSA algorithm not available", ex);
        }
    }

    private static EncodedKeySpec getPublicKeySpec(String keyValue) {
        var keyBytes = Base64.getDecoder().decode(keyValue);
        return new X509EncodedKeySpec(keyBytes);
    }

    private static RSAPublicKey generatePublicKey(EncodedKeySpec keySpec) {
        try {
            return (RSAPublicKey) getKeyFactory().generatePublic(keySpec);
        } catch (Exception ex) {
            throw new IllegalStateException("Can't generate public key", ex);
        }
    }

}
