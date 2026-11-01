package com.listifyai.monitoring;

import com.listifyai.entity.User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;

@Service
public class MonitoringPlanService {

    private final MonitoringProperties properties;

    public MonitoringPlanService(MonitoringProperties properties) {
        this.properties = properties;
    }

    public Duration frequency(User user) {
        String plan = user.getPlan() == null ? "free" : user.getPlan().toLowerCase(Locale.ROOT);
        return switch (plan) {
            case "pro" -> properties.getProFrequency();
            case "growth" -> properties.getGrowthFrequency();
            case "starter" -> properties.getStarterFrequency();
            default -> properties.getFreeFrequency();
        };
    }

    public OffsetDateTime nextCheckAt(User user, OffsetDateTime from) {
        return from.plus(frequency(user));
    }

    public int priority(User user) {
        String plan = user.getPlan() == null ? "free" : user.getPlan().toLowerCase(Locale.ROOT);
        return switch (plan) {
            case "pro" -> 10;
            case "growth" -> 25;
            case "starter" -> 50;
            default -> 100;
        };
    }
}
