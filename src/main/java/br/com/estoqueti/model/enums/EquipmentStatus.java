package br.com.estoqueti.model.enums;

public enum EquipmentStatus {
    DISPONIVEL("Disponivel"),
    EM_USO("Em uso"),
    EM_MANUTENCAO("Em manutencao"),
    DEFEITUOSO("Defeituoso"),
    DESCARTADO("Descartado");

    private final String displayName;

    EquipmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}