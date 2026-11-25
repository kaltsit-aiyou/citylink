package io.github.kaltsitaiyou.citylink.auth;

import io.github.kaltsitaiyou.citylink.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // POST /api/auth/register：成功时返回 201 和新用户资料。
    @PostMapping("/register")
    public ResponseEntity<UserService.UserProfile> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.register(
                        request.username(), request.password()));
    }

    // POST /api/auth/login：成功时返回 JWT 及有效时长。
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request) {
        String token = userService.login(
                request.username(), request.password());
        return new LoginResponse(token, "Bearer", 7200);
    }

    public record RegisterRequest(
            @NotBlank
            @Size(min = 3, max = 50)
            @Pattern(regexp = "[A-Za-z0-9_]+")
            String username,

            @NotBlank
            @Size(min = 8, max = 64)
            String password
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn
    ) {}
}