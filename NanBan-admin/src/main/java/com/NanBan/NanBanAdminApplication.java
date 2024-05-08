package com.NanBan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan(basePackages = {"com.NanBan.mappers"})
@EnableTransactionManagement     // 防止事物失效
public class NanBanAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(NanBanAdminApplication.class, args);
    }
}
