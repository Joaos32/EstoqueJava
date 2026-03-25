package br.com.estoqueti.model.enums;

public enum ReportFormat {
    CSV("CSV", "csv"),
    PDF("PDF", "pdf");

    private final String displayName;
    private final String extension;

    ReportFormat(String displayName, String extension) {
        this.displayName = displayName;
        this.extension = extension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtension() {
        return extension;
    }
}