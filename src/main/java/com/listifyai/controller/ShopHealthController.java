package com.listifyai.controller;

import com.listifyai.dto.ShopHealthDTO;
import com.listifyai.entity.User;
import com.listifyai.exception.PlanUpgradeRequiredException;
import com.listifyai.security.AuthUtils;
import com.listifyai.service.ShopHealthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api")
public class ShopHealthController {

    private static final Set<String> ALLOWED_PLANS = Set.of("growth", "pro");

    private final ShopHealthService shopHealthService;

    public ShopHealthController(ShopHealthService shopHealthService) {
        this.shopHealthService = shopHealthService;
    }

    @GetMapping("/shop-health")
    public ResponseEntity<ShopHealthDTO> getShopHealth() {
        User user = AuthUtils.getCurrentUser();
        guardPlan(user);
        return shopHealthService.getCached(user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/shop-health/compute")
    public ResponseEntity<ShopHealthDTO> computeShopHealth() {
        User user = AuthUtils.getCurrentUser();
        guardPlan(user);
        return ResponseEntity.ok(shopHealthService.compute(user));
    }

    private void guardPlan(User user) {
        String plan = user.getPlan() != null ? user.getPlan().toLowerCase() : "free";
        if (!ALLOWED_PLANS.contains(plan)) {
            throw new PlanUpgradeRequiredException(
                    "Shop Health Score is available on Growth plan and above.");
        }
    }
}
