package br.com.estoqueti.model.enums;

public enum AuditAction {
    LOGIN("Login"),
    CADASTRO("Cadastro"),
    EDICAO("Edicao"),
    EXCLUSAO("Exclusao"),
    MOVIMENTACAO("Movimentacao");

    private final String displayName;

    AuditAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
