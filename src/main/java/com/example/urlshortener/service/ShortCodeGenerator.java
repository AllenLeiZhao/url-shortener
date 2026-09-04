package com.example.urlshortener.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Random Base62 codes (see ADR-002). SecureRandom so the code space is not
 * enumerable; uniqueness is enforced by the DB constraint, not by this class.
 */
@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
