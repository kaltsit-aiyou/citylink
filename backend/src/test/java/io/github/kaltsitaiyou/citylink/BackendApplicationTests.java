package io.github.kaltsitaiyou.citylink;

import io.github.kaltsitaiyou.citylink.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/*
 * 启动完整的 Spring Boot 环境进行测试。
 * 因此，这两个测试也会检查应用配置能否正常加载。
 */
@SpringBootTest
class BackendApplicationTests {

    // 让 Spring 把已经创建好的 UserMapper 对象交给这个测试类。
    @Autowired
    private UserMapper userMapper;

    @Test
    void contextLoads() {
        // 方法体为空是有意的：只要应用启动失败，这个测试就会失败。
    }

    @Test
    void canQueryUserTable() {
        // 执行查询时不应抛出异常；这能检查 MyBatis 和数据库连接是否可用。
        assertDoesNotThrow(() -> {
            userMapper.countUsers();
        });
    }
}