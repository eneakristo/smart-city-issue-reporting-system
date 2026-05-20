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

    public CityZone resolveZone(BigDecimal longitude) {
        if (longitude == null) {
            return null;
        }

        return longitude.compareTo(applicationProperties.getZoning().getSplitLongitude()) < 0
                ? CityZone.ZONE_1
                : CityZone.ZONE_2;
    }

    public CityZone resolveZone(Issue issue) {
        if (issue.getZone() != null) {
            return issue.getZone();
        }

        return resolveZone(issue.getLongitude());
    }

    public boolean canAdministratorAccessIssue(CityZone assignedZone, Issue issue) {
        CityZone issueZone = resolveZone(issue);
        return assignedZone != null && issueZone != null && assignedZone == issueZone;
    }
}
