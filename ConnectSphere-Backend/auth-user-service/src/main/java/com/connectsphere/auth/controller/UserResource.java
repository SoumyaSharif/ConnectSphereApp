package com.connectsphere.auth.controller;

import com.connectsphere.auth.dto.UserProfileDto;
import com.connectsphere.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Public user profile and search endpoints")
public class UserResource {

    private final AuthService authService;

    @Operation(summary = "Get user public profile by ID")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileDto> getProfile(@PathVariable String userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @Operation(summary = "Search users by username or name")
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDto>> search(@RequestParam String query) {
        return ResponseEntity.ok(authService.searchUsers(query));
    }
}
