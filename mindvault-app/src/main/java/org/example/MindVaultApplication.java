package org.example;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "org.example")
@MapperScan("org.example.**.mapper")
@EnableCaching
public class MindVaultApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindVaultApplication.class, args);
    }
}