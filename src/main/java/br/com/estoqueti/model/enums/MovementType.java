package br.com.estoqueti.model.enums;

public enum MovementType {
    ENTRADA("Entrada de estoque"),
    SAIDA("Saida de estoque"),
    TRANSFERENCIA("Transferencia entre locais"),
    ENTREGA_FUNCIONARIO("Entrega com protocolo"),
    DEVOLUCAO_FUNCIONARIO("Devolucao com protocolo"),
    ENVIO_MANUTENCAO("Envio para manutencao"),
    RETORNO_MANUTENCAO("Retorno de manutencao"),
    BAIXA_DESCARTE("Baixa ou descarte");

    private final String displayName;

    MovementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requiresSourceLocation() {
        return this != ENTRADA;
    }

    public boolean requiresDestinationLocation() {
        return this == ENTRADA
                || this == TRANSFERENCIA
                || this == ENTREGA_FUNCIONARIO
                || this == DEVOLUCAO_FUNCIONARIO
                || this == ENVIO_MANUTENCAO
                || this == RETORNO_MANUTENCAO;
    }

    public boolean requiresDistinctLocations() {
        return this == TRANSFERENCIA
                || this == ENVIO_MANUTENCAO
                || this == RETORNO_MANUTENCAO;
    }

    public boolean requiresFullBalance() {
        return this == TRANSFERENCIA
                || this == ENTREGA_FUNCIONARIO
                || this == DEVOLUCAO_FUNCIONARIO
                || this == ENVIO_MANUTENCAO
                || this == RETORNO_MANUTENCAO;
    }

    public boolean decreasesQuantity() {
        return this == SAIDA || this == BAIXA_DESCARTE;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
