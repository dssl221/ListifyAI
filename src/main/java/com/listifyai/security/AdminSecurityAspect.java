package com.listifyai.security;

import com.listifyai.entity.User;
import com.listifyai.exception.ForbiddenException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AdminSecurityAspect {

    @Before("@within(com.listifyai.security.AdminOnly) || @annotation(com.listifyai.security.AdminOnly)")
    public void requireAdmin() {
        User user = AuthUtils.getCurrentUserOrNull();
        if (user == null || !"admin".equals(user.getRole())) {
            throw new ForbiddenException("Admin access required");
        }
    }
}
