package com.NanBan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.NanBan"})
@MapperScan(basePackages = {"com.NanBan.mappers"})
@EnableTransactionManagement
public class NanBanWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(NanBanWebApplication.class, args);
    }
}
