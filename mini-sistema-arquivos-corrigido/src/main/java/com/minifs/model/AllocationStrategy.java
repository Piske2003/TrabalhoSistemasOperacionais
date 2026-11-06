package com.minifs.model;

public enum AllocationStrategy {
    LINKED("Encadeada"),
    CONTIGUOUS("Contígua"),
    INDEXED("Indexada");

    private final String label;

    AllocationStrategy(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
