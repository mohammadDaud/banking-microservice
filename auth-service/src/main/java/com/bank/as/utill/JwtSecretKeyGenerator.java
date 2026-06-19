package com.bank.as.utill;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;

public class JwtSecretKeyGenerator {
    public static void main(String[] args) {

        SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);

        String base64Key = Base64.getEncoder()
                .encodeToString(secretKey.getEncoded());

        System.out.println("JWT Secret Key:");
        System.out.println(base64Key);

        System.out.println("\napplication.yml");
        System.out.println("app:");
        System.out.println("  jwtSecret: " + base64Key);
    }
}
