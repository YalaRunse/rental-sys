package com.rental.auth;

import org.springframework.stereotype.Component;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Base64;

/** 使用 JDK 提供的 PBKDF2-HMAC-SHA256，随机盐与迭代次数一起存储以便升级。 */
@Component
public class PasswordHasher {
    private static final int ITERATIONS = 600000;
    private static final SecureRandom RANDOM = new SecureRandom();
    public String hash(String password) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }
    public boolean matches(String password, String stored) {
        if (stored == null || !stored.startsWith("pbkdf2$")) return false;
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < ITERATIONS || iterations > 2000000) return false;
            return MessageDigest.isEqual(derive(password, Base64.getDecoder().decode(parts[2]), iterations),
                    Base64.getDecoder().decode(parts[3]));
        } catch (IllegalArgumentException error) { return false; }
    }
    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException error) { throw new IllegalStateException("密码散列算法不可用", error); }
        finally { spec.clearPassword(); }
    }
}
