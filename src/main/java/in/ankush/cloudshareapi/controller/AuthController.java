package in.ankush.cloudshareapi.controller;

import in.ankush.cloudshareapi.dto.AuthResponse;
import in.ankush.cloudshareapi.dto.LoginRequest;
import in.ankush.cloudshareapi.dto.RefreshTokenRequest;
import in.ankush.cloudshareapi.dto.RegisterRequest;
import in.ankush.cloudshareapi.dto.UpdateProfileRequest;
import in.ankush.cloudshareapi.dto.UserProfileDTO;
import in.ankush.cloudshareapi.service.AuthService;
import in.ankush.cloudshareapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Fully replaces Clerk sign-in / sign-up / webhooks / user management.
 * Public: /auth/register, /auth/login, /auth/refresh
 * Protected: /auth/me (get/update), /auth/me/photo (upload)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /** Protected: requires a valid access token. Used by the frontend to restore session on page reload. */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> me() {
        return ResponseEntity.ok(userService.getCurrentUserProfile());
    }

    /** Protected: update first name / last name / email for the logged-in user. */
    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    /** Protected: upload/replace the logged-in user's profile photo. Field name must be "photo". */
    @PostMapping(value = "/me/photo", consumes = "multipart/form-data")
    public ResponseEntity<UserProfileDTO> updateMyPhoto(@RequestParam("photo") MultipartFile photo) throws IOException {
        return ResponseEntity.ok(userService.updatePhoto(photo));
    }
}
