package com.smartcity.issuereporting.service;

import com.smartcity.issuereporting.config.ApplicationProperties;
import com.smartcity.issuereporting.model.entity.CityZone;
import com.smartcity.issuereporting.model.entity.Issue;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class CityZoneService {

    private final ApplicationProperties applicationProperties;

    public CityZoneService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public CityZone resolveZone(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        boolean north = latitude.compareTo(applicationProperties.getZoning().getSplitLatitude()) >= 0;
        boolean west = longitude.compareTo(applicationProperties.getZoning().getSplitLongitude()) < 0;

        if (north && west) {
            return CityZone.ZONE_1;
        }

        if (north) {
            return CityZone.ZONE_2;
        }

        return west ? CityZone.ZONE_3 : CityZone.ZONE_4;
    }

    public CityZone resolveZone(Issue issue) {
        if (issue.getZone() != null) {
            return issue.getZone();
        }

        return resolveZone(issue.getLatitude(), issue.getLongitude());
    }

    public boolean canAdministratorAccessIssue(CityZone assignedZone, Issue issue) {
        CityZone issueZone = resolveZone(issue);
        return assignedZone != null && issueZone != null && assignedZone == issueZone;
    }
}
