package com.hbys.survey.application.service.impl;

import com.hbys.survey.application.service.TokenHashService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class TokenHashServiceImpl implements TokenHashService {

    @Override
    public String sha256(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Token value cannot be blank.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);

            for (byte hashByte : hash) {
                result.append(String.format("%02x", hashByte));
            }

            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
        }
    }
}
