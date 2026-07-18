package com.medigenius.controller;

import com.medigenius.dto.UserProfileDto;
import com.medigenius.security.UserPrincipal;
import com.medigenius.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NEW CONTROLLER (Feature 2 - User Entity / Feature 12 - Profile).
 * Protected - see SecurityConfig (anything outside /api/v1/**, /api/auth/** requires a
 * valid "Authorization: Bearer <token>").
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users/me - name, email, role, uploaded PDFs, total conversations. */
    @GetMapping("/me")
    public UserProfileDto me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getProfile(principal.getId());
    }
}
