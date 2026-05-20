package com.smartcity.issuereporting.service;

import java.math.BigDecimal;
import java.util.Optional;

public interface LocationGeocodingService {

    Optional<ResolvedCoordinates> resolveCoordinates(String locationText);

    record ResolvedCoordinates(BigDecimal latitude, BigDecimal longitude, String resolvedQuery) {
    }
}
