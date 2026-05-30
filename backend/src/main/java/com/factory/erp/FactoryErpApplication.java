package com.factory.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ShakeERP 金东进销存系统启动入口。
 */
@SpringBootApplication
@EnableScheduling
public class FactoryErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(FactoryErpApplication.class, args);
    }
}

