package com.listifyai.security;

import com.listifyai.entity.User;
import com.listifyai.exception.ForbiddenException;
import com.listifyai.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Static utility for extracting the authenticated {@link User} from the
 * current request's {@link org.springframework.security.core.context.SecurityContext}.
 *
 * <p>The principal is the full {@link User} entity stored by {@link SupabaseJwtFilter}.
 * Call {@code AuthUtils.getCurrentUser()} in service or controller methods that
 * need the acting user without injecting {@code HttpServletRequest}.
 */
public final class AuthUtils {

    private AuthUtils() {
        // utility class — no instantiation
    }

    /**
     * Returns the currently authenticated {@link User}.
     *
     * @throws UnauthorizedException if there is no authenticated principal in
     *                               the security context (e.g. token was missing
     *                               or invalid and the route is still reachable).
     */
    public static User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            throw new UnauthorizedException("No authenticated user found in security context");
        }

        return (User) auth.getPrincipal();
    }

    /**
     * Returns the currently authenticated {@link User}, or {@code null} if
     * the request is unauthenticated. Useful in optional-auth scenarios.
     */
    public static User getCurrentUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    /**
     * Asserts that the given user is not suspended.
     *
     * @throws ForbiddenException if the user's role is "suspended".
     */
    public static void assertNotSuspended(User user) {
        if ("suspended".equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenException("Your account has been suspended.");
        }
    }
}
