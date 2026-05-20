package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import com.smartcity.issuereporting.exception.BadRequestException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

@Service
public class SupabaseIssueImageStorageService implements IssueImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseIssueImageStorageService.class);
    private static final int SIGNED_URL_EXPIRATION_SECONDS = 86_400;

    private static final List<String> DEFAULT_ALLOWED_IMAGE_TYPES = List.of(
            MediaType.IMAGE_JPEG_VALUE,
            "image/jpg",
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final ApplicationProperties applicationProperties;
    private final RestClient restClient;

    public SupabaseIssueImageStorageService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public StoredIssueImage store(String issueStoragePrefix, MultipartFile file) {
        validateImage(file);
        ApplicationProperties.Storage storageProperties = applicationProperties.getStorage();

        if (!StringUtils.hasText(storageProperties.getSupabaseUrl())
                || !StringUtils.hasText(storageProperties.getSupabaseServiceKey())
                || !StringUtils.hasText(storageProperties.getSupabaseBucket())) {
            throw new IllegalStateException("Supabase storage is not configured for issue image uploads");
        }

        String objectPath = buildObjectPath(issueStoragePrefix, file.getOriginalFilename(), resolveContentType(file));

        try {
            byte[] bytes = file.getBytes();
            URI uploadUri = URI.create(buildUploadUrl(storageProperties, objectPath));

            restClient.post()
                    .uri(uploadUri)
                    .header("apikey", storageProperties.getSupabaseServiceKey())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + storageProperties.getSupabaseServiceKey())
                    .header("x-upsert", "true")
                    .header("cache-control", "3600")
                    .contentType(MediaType.parseMediaType(resolveContentType(file)))
                    .body(bytes)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleUploadFailure)
                    .toBodilessEntity();

            return new StoredIssueImage(
                    objectPath,
                    buildPublicUrl(storageProperties, objectPath),
                    file.getOriginalFilename(),
                    resolveContentType(file),
                    file.getSize()
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read issue image before uploading to Supabase", exception);
        }
    }

    @Override
    public String resolveAccessUrl(String storagePath, String fallbackUrl) {
        ApplicationProperties.Storage storageProperties = applicationProperties.getStorage();

        if (!StringUtils.hasText(storagePath)) {
            return fallbackUrl;
        }

        if (!StringUtils.hasText(storageProperties.getSupabaseUrl())
                || !StringUtils.hasText(storageProperties.getSupabaseServiceKey())
                || !StringUtils.hasText(storageProperties.getSupabaseBucket())) {
            return fallbackUrl;
        }

        try {
            URI signedUrlEndpoint = URI.create(buildSignedUrlRequestUrl(storageProperties, storagePath));
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(signedUrlEndpoint)
                    .header("apikey", storageProperties.getSupabaseServiceKey())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + storageProperties.getSupabaseServiceKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", SIGNED_URL_EXPIRATION_SECONDS))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleSignedUrlFailure)
                    .body(Map.class);

            if (response == null) {
                return fallbackUrl;
            }

            Object signedPathValue = response.get("signedURL");
            if (!(signedPathValue instanceof String signedPath) || signedPath.isBlank()) {
                signedPathValue = response.get("signedUrl");
            }

            if (signedPathValue instanceof String signedPath && !signedPath.isBlank()) {
                return toAbsoluteSupabaseUrl(storageProperties, signedPath);
            }
        } catch (RuntimeException exception) {
            log.warn("Failed to create signed Supabase URL for storagePath={}", storagePath, exception);
        }

        return fallbackUrl;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("At least one non-empty image is required");
        }

        String contentType = resolveContentType(file);
        List<String> allowedTypes = applicationProperties.getStorage().getAllowedImageContentTypes().isEmpty()
                ? DEFAULT_ALLOWED_IMAGE_TYPES
                : applicationProperties.getStorage().getAllowedImageContentTypes();

        if (!allowedTypes.contains(contentType)) {
            throw new BadRequestException("Supported image types are JPEG, PNG, and WEBP");
        }

        long maxFileSizeBytes = applicationProperties.getStorage().getMaxImageFileSizeBytes();
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("Each image must be at most " + maxFileSizeBytes + " bytes");
        }
    }

    private String buildObjectPath(String issueStoragePrefix, String originalFilename, String contentType) {
        String normalizedPrefix = trimSlashes(issueStoragePrefix);
        String extension = resolveExtension(originalFilename, contentType);
        return normalizedPrefix + "/" + UUID.randomUUID() + extension;
    }

    private String buildUploadUrl(ApplicationProperties.Storage storageProperties, String objectPath) {
        String baseUrl = trimTrailingSlash(storageProperties.getSupabaseUrl());
        String encodedPath = UriUtils.encodePath(objectPath, StandardCharsets.UTF_8);
        return baseUrl + "/storage/v1/object/" + storageProperties.getSupabaseBucket() + "/" + encodedPath;
    }

    private String buildPublicUrl(ApplicationProperties.Storage storageProperties, String objectPath) {
        String baseUrl = trimTrailingSlash(storageProperties.getSupabaseUrl());
        String encodedPath = UriUtils.encodePath(objectPath, StandardCharsets.UTF_8);
        return baseUrl + "/storage/v1/object/public/" + storageProperties.getSupabaseBucket() + "/" + encodedPath;
    }

    private String buildSignedUrlRequestUrl(ApplicationProperties.Storage storageProperties, String objectPath) {
        String baseUrl = trimTrailingSlash(storageProperties.getSupabaseUrl());
        String encodedPath = UriUtils.encodePath(objectPath, StandardCharsets.UTF_8);
        return baseUrl + "/storage/v1/object/sign/" + storageProperties.getSupabaseBucket() + "/" + encodedPath;
    }

    private String toAbsoluteSupabaseUrl(ApplicationProperties.Storage storageProperties, String signedPath) {
        String baseUrl = trimTrailingSlash(storageProperties.getSupabaseUrl());
        return signedPath.startsWith("http://") || signedPath.startsWith("https://")
                ? signedPath
                : baseUrl + "/storage/v1" + signedPath;
    }

    private String resolveContentType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    }

    private void handleUploadFailure(org.springframework.http.HttpRequest request, ClientHttpResponse response)
            throws IOException {
        String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        log.error("Supabase upload failed. status={}, body={}", response.getStatusCode().value(), responseBody);

        String message = responseBody.isBlank()
                ? "Supabase upload failed with status " + response.getStatusCode().value()
                : "Supabase upload failed with status " + response.getStatusCode().value() + ": " + responseBody;
        throw new IllegalStateException(message);
    }

    private void handleSignedUrlFailure(org.springframework.http.HttpRequest request, ClientHttpResponse response)
            throws IOException {
        String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        log.error("Supabase signed URL request failed. status={}, body={}", response.getStatusCode().value(), responseBody);

        String message = responseBody.isBlank()
                ? "Supabase signed URL request failed with status " + response.getStatusCode().value()
                : "Supabase signed URL request failed with status " + response.getStatusCode().value() + ": " + responseBody;
        throw new IllegalStateException(message);
    }

    private String resolveExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension != null && !extension.isBlank()) {
            return "." + extension.toLowerCase(Locale.ROOT);
        }

        return "";
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String extension = resolveExtension(originalFilename);
        if (!extension.isBlank()) {
            return extension;
        }

        return switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
            case "image/jpg" -> ".jpg";
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return "issues";
        }

        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
