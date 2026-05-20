package com.kamotomo.pos.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SecurityUtil {

    // This method converts a plain text password into a secure SHA-256 hash
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes());

            // Convert bytes to hexadecimal format
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error initializing hashing algorithm", e);
        }
    }
}