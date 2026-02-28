package com.deharri.jlds.util;

import com.deharri.jlds.error.exception.UnauthorizedAccessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class HeaderExtractor {

    public static UUID getUserId(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedAccessException("Missing user identity");
        }
        return UUID.fromString(userId);
    }

    public static String getUsername(HttpServletRequest request) {
        return request.getHeader("X-Username");
    }

    public static String getUserEmail(HttpServletRequest request) {
        return request.getHeader("X-User-Email");
    }

    public static List<String> getUserRoles(HttpServletRequest request) {
        String roles = request.getHeader("X-User-Roles");
        if (roles == null || roles.isBlank()) return List.of();
        return Arrays.asList(roles.split(","));
    }

    public static boolean hasRole(HttpServletRequest request, String role) {
        return getUserRoles(request).contains(role);
    }

    public static boolean isConsumer(HttpServletRequest request) {
        return hasRole(request, "ROLE_CONSUMER");
    }

    public static boolean isWorker(HttpServletRequest request) {
        return hasRole(request, "ROLE_WORKER");
    }
}
