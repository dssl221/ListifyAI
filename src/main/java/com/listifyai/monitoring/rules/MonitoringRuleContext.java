package com.listifyai.monitoring.rules;

import com.listifyai.entity.ListingSnapshot;
import com.listifyai.entity.MonitoredListing;

import java.time.OffsetDateTime;
import java.util.List;

public record MonitoringRuleContext(
        MonitoredListing listing,
        ListingSnapshot current,
        ListingSnapshot previous,
        List<String> tags,
        OffsetDateTime now
) {}
