package br.com.estoqueti.model.enums;

public enum Role {
    ADMIN("Administrador"),
    TECNICO("Tecnico"),
    VISUALIZADOR("Visualizador");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canManageUsers() {
        return this == ADMIN;
    }

    public boolean canManageInventory() {
        return this == ADMIN || this == TECNICO;
    }

    @Override
    public String toString() {
        return displayName;
    }
}