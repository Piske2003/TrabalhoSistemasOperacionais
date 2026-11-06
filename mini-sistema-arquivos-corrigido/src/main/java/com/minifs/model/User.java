package com.minifs.model;

import java.util.Objects;

/** Identidade simples do sistema de arquivos; não há senha ou outros dados. */
public record User(String username) {
    public User {
        if (username == null || username.isBlank() || username.contains("/") || username.contains("\\")) {
            throw new IllegalArgumentException("Informe um nome de usuário válido, sem barras.");
        }
        username = username.trim();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof User user && username.equalsIgnoreCase(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username.toLowerCase());
    }

    @Override
    public String toString() {
        return username;
    }
}
