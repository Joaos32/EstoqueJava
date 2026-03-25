package br.com.estoqueti.model.enums;

public enum ReturnProtocolReason {
    DESLIGAMENTO_EMPRESA("Desligamento da empresa"),
    ALTERACAO_CARGO_FUNCAO("Alteracao de cargo/funcao"),
    SUBSTITUICAO_EQUIPAMENTO("Substituicao do equipamento"),
    OUTROS("Outros");

    private final String displayName;

    ReturnProtocolReason(String displayName) {
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
