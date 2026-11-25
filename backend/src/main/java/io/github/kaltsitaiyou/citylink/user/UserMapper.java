package io.github.kaltsitaiyou.citylink.user;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/*
 * MyBatis 根据这里的注解生成数据库访问代码。
 * SQL 中的 #{参数名} 会绑定方法参数，不要用字符串拼接输入 SQL。
 */
@Mapper
public interface UserMapper {

    // 原有的数据库连通测试会调用它。
    @Select("SELECT COUNT(*) FROM app_user")
    long countUsers();

    // 注册前检查用户名是否已被占用。
    @Select("SELECT COUNT(*) FROM app_user WHERE username = #{username}")
    long countByUsername(@Param("username") String username);

    // 保存密码摘要；初始昵称暂时设为用户名。
    @Insert("""
        INSERT INTO app_user (username, password_hash, nickname)
        VALUES (#{username}, #{passwordHash}, #{username})
        """)
    int insertUser(@Param("username") String username,
                   @Param("passwordHash") String passwordHash);

    // 登录时按用户名读取账号；AS 把数据库列名对应到 Java 属性名。
    @Select("""
        SELECT id, username, password_hash AS passwordHash,
               nickname, avatar_url AS avatarUrl
        FROM app_user
        WHERE username = #{username}
        """)
    UserAccount findByUsername(@Param("username") String username);

    // 根据登录令牌中的用户 ID 查询自己的资料。
    @Select("""
        SELECT id, username, password_hash AS passwordHash,
               nickname, avatar_url AS avatarUrl
        FROM app_user
        WHERE id = #{id}
        """)
    UserAccount findById(@Param("id") long id);

    // 返回值表示成功修改的行数；0 表示没有找到这个用户。
    @Update("""
        UPDATE app_user
        SET nickname = #{nickname}, avatar_url = #{avatarUrl}
        WHERE id = #{id}
        """)
    int updateProfile(@Param("id") long id,
                      @Param("nickname") String nickname,
                      @Param("avatarUrl") String avatarUrl);
}