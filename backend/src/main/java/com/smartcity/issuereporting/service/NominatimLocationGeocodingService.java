package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class NominatimLocationGeocodingService implements LocationGeocodingService {

    private static final ParameterizedTypeReference<List<NominatimSearchResult>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final ApplicationProperties applicationProperties;

    public NominatimLocationGeocodingService(ApplicationProperties applicationProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Optional<ResolvedCoordinates> resolveCoordinates(String locationText) {
        if (!applicationProperties.getGeocoding().isEnabled()) {
            return Optional.empty();
        }

        String query = buildQuery(locationText);
        if (query == null) {
            return Optional.empty();
        }

        try {
            List<NominatimSearchResult> results = restClient.get()
                    .uri(UriComponentsBuilder
                            .fromHttpUrl(applicationProperties.getGeocoding().getBaseUrl())
                            .path(applicationProperties.getGeocoding().getSearchPath())
                            .queryParam("q", query)
                            .queryParam("format", "jsonv2")
                            .queryParam("limit", applicationProperties.getGeocoding().getResultLimit())
                            .queryParam("countrycodes", applicationProperties.getGeocoding().getCountryCode())
                            .build(true)
                            .toUri())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("User-Agent", applicationProperties.getGeocoding().getUserAgent())
                    .retrieve()
                    .body(RESPONSE_TYPE);

            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }

            NominatimSearchResult firstResult = results.get(0);
            if (firstResult.lat() == null || firstResult.lon() == null) {
                return Optional.empty();
            }

            return Optional.of(new ResolvedCoordinates(
                    new BigDecimal(firstResult.lat()),
                    new BigDecimal(firstResult.lon()),
                    query
            ));
        } catch (Exception exception) {
            log.warn("Failed to geocode location text '{}': {}", locationText, exception.getMessage());
            return Optional.empty();
        }
    }

    private String buildQuery(String locationText) {
        if (locationText == null || locationText.isBlank()) {
            return null;
        }

        String normalizedLocation = locationText.trim();
        String querySuffix = applicationProperties.getGeocoding().getDefaultQuerySuffix();
        if (querySuffix == null || querySuffix.isBlank()) {
            return normalizedLocation;
        }

        String normalizedSuffix = querySuffix.trim();
        if (normalizedLocation.toLowerCase().contains(normalizedSuffix.toLowerCase())) {
            return normalizedLocation;
        }

        return normalizedLocation + ", " + normalizedSuffix;
    }

    private record NominatimSearchResult(String lat, String lon) {
    }
}
