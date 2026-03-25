package br.com.estoqueti.dto.common;

public record LookupOptionDto(
        Long id,
        String label
) {

    @Override
    public String toString() {
        return label;
    }
}