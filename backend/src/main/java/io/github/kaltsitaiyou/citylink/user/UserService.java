package io.github.kaltsitaiyou.citylink.user;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/*
 * 业务层：决定注册、登录和修改资料时应该做什么。
 * UserMapper 负责执行 SQL；Controller 负责接收 HTTP 请求。
 */
@Service
public class UserService {
    // 令牌有效期为 7200 秒，即两小时。
    private static final long TOKEN_LIFETIME_SECONDS = 7200;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String dummyHash;

    // Spring 会把已创建的 Mapper、密码编码器和令牌编码器传进来。
    public UserService(UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.dummyHash = passwordEncoder.encode("unused-password");
    }

    public UserProfile register(String username, String password) {
        // BCrypt 只处理一定长度的输入，因此按 UTF-8 字节数检查。
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "密码不能超过 72 个 UTF-8 字节");
        }

        // 提前检查重名，给用户明确提示。
        if (userMapper.countByUsername(username) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "用户名已存在");
        }

        // 数据库只保存 BCrypt 摘要，不保存明文密码。
        String passwordHash = passwordEncoder.encode(password);
        try {
            userMapper.insertUser(username, passwordHash);
        } catch (DuplicateKeyException ex) {
            // 两人同时注册同名时，数据库唯一约束仍会挡住重复记录。
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "用户名已存在", ex);
        }

        return toProfile(userMapper.findByUsername(username));
    }

    public String login(String username, String password) {
        UserAccount account = userMapper.findByUsername(username);

        // 用户名不存在时也做一次密码核对，减少通过响应时间
        // 猜测“某个用户名是否存在”的可能。
        String hash = account == null
                ? dummyHash
                : account.getPasswordHash();

        boolean passwordCorrect = passwordEncoder.matches(password, hash);
        if (account == null || !passwordCorrect) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        // subject 保存用户 ID；issuedAt 和 expiresAt 表示签发及到期时间。
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("citylink")
                .subject(account.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(TOKEN_LIFETIME_SECONDS))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        // 用密钥签名，返回可以放进 Authorization 请求头的令牌。
        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public UserProfile getProfile(long id) {
        UserAccount account = userMapper.findById(id);
        if (account == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "用户不存在");
        }
        return toProfile(account);
    }

    public UserProfile updateProfile(long id,
                                     String nickname,
                                     String avatarUrl) {
        // 空头像地址表示清除头像。
        String normalizedAvatar = avatarUrl == null || avatarUrl.isBlank()
                ? null
                : avatarUrl;

        int updated = userMapper.updateProfile(
                id, nickname, normalizedAvatar);
        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "用户不存在");
        }
        return getProfile(id);
    }

    private UserProfile toProfile(UserAccount account) {
        // 接口返回的资料不包含 passwordHash。
        return new UserProfile(
                account.getId(),
                account.getUsername(),
                account.getNickname(),
                account.getAvatarUrl()
        );
    }

    // record 用来简洁地定义一组返回数据。
    public record UserProfile(
            Long id,
            String username,
            String nickname,
            String avatarUrl
    ) {}
}