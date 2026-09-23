package com.example.ordermanage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.ordermanage.mapper")
public class OrderManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderManageApplication.class, args);
    }
}
