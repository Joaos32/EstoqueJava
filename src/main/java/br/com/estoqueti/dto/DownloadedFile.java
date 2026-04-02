package br.com.estoqueti.dto;

import java.util.Map;

public record DownloadedFile(
        String fileName,
        String contentType,
        byte[] content,
        Map<String, String> metadata
) {
}
