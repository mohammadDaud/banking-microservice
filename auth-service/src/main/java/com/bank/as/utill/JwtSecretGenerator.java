package com.bank.as.utill;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Scanner;

public class JwtSecretGenerator {

    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 512; // For HS512
    private static final int SALT_LENGTH_BYTES = 16;

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter a strong passphrase: ");
        String passphrase = scanner.nextLine();

        if (passphrase == null || passphrase.trim().length() < 16) {
            System.out.println(
                    "Passphrase must contain at least 16 characters."
            );
            return;
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(
                passphrase.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH_BITS
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA512"
        );

        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        String jwtSecret = Base64.getEncoder()
                .encodeToString(keyBytes);

        String saltBase64 = Base64.getEncoder()
                .encodeToString(salt);

        System.out.println("\nJWT Secret:");
        System.out.println(jwtSecret);

        System.out.println("\nJWT Salt:");
        System.out.println(saltBase64);

        System.out.println("\napplication.yml:");
        System.out.println("jwt:");
        System.out.println("  secret: ${JWT_SECRET}");
        System.out.println("  expiration: 900000");
        System.out.println("  refresh-expiration: 604800000");

        System.out.println("\nEnvironment variables:");
        System.out.println("JWT_SECRET=" + jwtSecret);
        System.out.println("JWT_SALT=" + saltBase64);

        spec.clearPassword();
        scanner.close();
    }
}