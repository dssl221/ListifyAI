package com.listifyai.service;

import com.listifyai.entity.User;
import com.listifyai.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyReportScheduler {

    private final UserRepository userRepository;
    private final WeeklyReportService weeklyReportService;

    public WeeklyReportScheduler(UserRepository userRepository, WeeklyReportService weeklyReportService) {
        this.userRepository = userRepository;
        this.weeklyReportService = weeklyReportService;
    }

    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyReports() {
        int page = 0;
        Page<User> users;
        do {
            users = userRepository.findByStripeSubscriptionIdIsNotNull(PageRequest.of(page, 100));
            users.forEach(weeklyReportService::sendWeeklyReport);
            page++;
        } while (users.hasNext());
    }
}
