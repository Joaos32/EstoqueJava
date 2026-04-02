package br.com.estoqueti.util;

import br.com.estoqueti.dto.DownloadedFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public final class ApiFileResponseFactory {

    private ApiFileResponseFactory() {
    }

    public static ResponseEntity<ByteArrayResource> createAttachmentResponse(DownloadedFile downloadedFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(downloadedFile.contentType()));
        headers.setContentLength(downloadedFile.content().length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(downloadedFile.fileName(), StandardCharsets.UTF_8)
                .build());
        downloadedFile.metadata().forEach((key, value) -> headers.add("X-" + toHeaderName(key), value));
        return new ResponseEntity<>(new ByteArrayResource(downloadedFile.content()), headers, HttpStatus.OK);
    }

    private static String toHeaderName(String key) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < key.length(); index++) {
            char character = key.charAt(index);
            if (Character.isUpperCase(character) && index > 0) {
                builder.append('-');
            }
            builder.append(Character.toUpperCase(character));
        }
        return builder.toString();
    }
}
