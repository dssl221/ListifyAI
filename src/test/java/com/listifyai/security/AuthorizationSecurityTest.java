package com.listifyai.security;

import com.listifyai.entity.User;
import com.listifyai.exception.ForbiddenException;
import com.listifyai.service.BulkJobService;
import com.listifyai.service.ListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class AuthorizationSecurityTest {

    @Mock
    private ListingService listingService;

    @Mock
    private BulkJobService bulkJobService;

    @InjectMocks
    private AdminSecurityAspect adminSecurityAspect;

    private User normalUser;
    private User adminUser;
    private User suspendedUser;

    @BeforeEach
    void setup() {
        normalUser = new User();
        normalUser.setId(UUID.randomUUID());
        normalUser.setRole("user");

        adminUser = new User();
        adminUser.setId(UUID.randomUUID());
        adminUser.setRole("admin");

        suspendedUser = new User();
        suspendedUser.setId(UUID.randomUUID());
        suspendedUser.setRole("suspended");
    }

    @Test
    void assertNotSuspendedThrowsForSuspendedUser() {
        assertThrows(ForbiddenException.class, () -> AuthUtils.assertNotSuspended(suspendedUser));
    }

    @Test
    void assertNotSuspendedPassesForNormalUser() {
        AuthUtils.assertNotSuspended(normalUser); // Should not throw
    }

    @Test
    void adminSecurityAspectRequiresAdminRole() {
        try (MockedStatic<AuthUtils> authUtils = Mockito.mockStatic(AuthUtils.class)) {
            authUtils.when(AuthUtils::getCurrentUserOrNull).thenReturn(normalUser);

            assertThrows(ForbiddenException.class, () -> adminSecurityAspect.requireAdmin());
        }
    }

    @Test
    void adminSecurityAspectPassesForAdminRole() {
        try (MockedStatic<AuthUtils> authUtils = Mockito.mockStatic(AuthUtils.class)) {
            authUtils.when(AuthUtils::getCurrentUserOrNull).thenReturn(adminUser);

            adminSecurityAspect.requireAdmin(); // Should not throw
        }
    }
}
