package io.github.kaltsitaiyou.citylink.user;

public class UserAccount {
    private Long id;
    private String username;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;

    // MyBatis 查询出一行数据后，通过这些 setter 填入属性。
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    // 业务代码通过这些 getter 读取属性。
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }
}