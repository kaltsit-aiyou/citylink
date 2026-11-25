package io.github.kaltsitaiyou.citylink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * 标记这是 Spring Boot 应用的入口。
 * Spring Boot 会从这个类所在的包开始，寻找 Controller、Service、Mapper 等组件。
 */
@SpringBootApplication
public class BackendApplication {

    // Java 程序启动时首先执行 main 方法。
    public static void main(String[] args) {
        // 创建 Spring 容器、加载配置，并启动后端服务。
        SpringApplication.run(BackendApplication.class, args);
    }
}