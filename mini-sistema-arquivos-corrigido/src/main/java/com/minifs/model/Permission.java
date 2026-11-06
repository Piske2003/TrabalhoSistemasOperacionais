package com.minifs.model;

public enum Permission {
    READ("Leitura", "r"),
    WRITE("Escrita", "w"),
    EXECUTE("Execução", "x"),
    DELETE("Excluir", "d");

    private final String label;
    private final String symbol;

    Permission(String label, String symbol) {
        this.label = label;
        this.symbol = symbol;
    }

    public String getLabel() { return label; }
    public String getSymbol() { return symbol; }
}
