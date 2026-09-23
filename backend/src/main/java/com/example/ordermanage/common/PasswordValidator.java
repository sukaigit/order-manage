package com.example.ordermanage.common;

public final class PasswordValidator {

    private PasswordValidator() {
    }

    /** 8-20 chars, at least 3 of [A-Z][a-z][0-9][^A-Za-z0-9]. */
    public static boolean isValid(String password) {
        if (password == null || !password.matches(".{8,20}")) {
            return false;
        }
        int types = 0;
        if (password.matches(".*[A-Z].*")) {
            types++;
        }
        if (password.matches(".*[a-z].*")) {
            types++;
        }
        if (password.matches(".*[0-9].*")) {
            types++;
        }
        if (password.matches(".*[^A-Za-z0-9].*")) {
            types++;
        }
        return types >= 3;
    }
}
