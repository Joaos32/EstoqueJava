package br.com.estoqueti.model.enums;

public enum ReportType {
    EQUIPAMENTOS_CADASTRADOS(
            "Equipamentos cadastrados",
            "Lista completa dos equipamentos ativos cadastrados no estoque.",
            false,
            "equipamentos-cadastrados"
    ),
    ESTOQUE_BAIXO(
            "Estoque baixo",
            "Itens com saldo atual abaixo do estoque minimo configurado.",
            false,
            "estoque-baixo"
    ),
    EQUIPAMENTOS_POR_CATEGORIA(
            "Equipamentos por categoria",
            "Resumo consolidado de registros e quantidades por categoria.",
            false,
            "equipamentos-por-categoria"
    ),
    EQUIPAMENTOS_POR_LOCALIZACAO(
            "Equipamentos por localizacao",
            "Resumo consolidado de registros e quantidades por localizacao.",
            false,
            "equipamentos-por-localizacao"
    ),
    MOVIMENTACOES_POR_PERIODO(
            "Movimentacoes por periodo",
            "Historico das movimentacoes registradas dentro do intervalo informado.",
            true,
            "movimentacoes-por-periodo"
    ),
    EQUIPAMENTOS_EM_MANUTENCAO(
            "Equipamentos em manutencao",
            "Lista de equipamentos com status atual em manutencao.",
            false,
            "equipamentos-em-manutencao"
    );

    private final String displayName;
    private final String description;
    private final boolean requiresPeriod;
    private final String fileSlug;

    ReportType(String displayName, String description, boolean requiresPeriod, String fileSlug) {
        this.displayName = displayName;
        this.description = description;
        this.requiresPeriod = requiresPeriod;
        this.fileSlug = fileSlug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresPeriod() {
        return requiresPeriod;
    }

    public String getFileSlug() {
        return fileSlug;
    }

    @Override
    public String toString() {
        return displayName;
    }
}