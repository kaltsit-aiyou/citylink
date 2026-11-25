package io.github.kaltsitaiyou.citylink.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // GET /api/users/me：令牌里的 subject 就是用户 ID。
    @GetMapping("/me")
    public UserService.UserProfile me(
            @AuthenticationPrincipal Jwt jwt) {
        long id = Long.parseLong(jwt.getSubject());
        return userService.getProfile(id);
    }

    // PUT /api/users/me：只修改当前登录用户的昵称和头像地址。
    @PutMapping("/me")
    public UserService.UserProfile updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        long id = Long.parseLong(jwt.getSubject());
        return userService.updateProfile(
                id, request.nickname(), request.avatarUrl());
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 50) String nickname,
            @Size(max = 512) String avatarUrl
    ) {}
}