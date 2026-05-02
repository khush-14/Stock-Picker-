package com.stockpicker.ingestor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.stockpicker.common.entity")
@EnableJpaRepositories(basePackages = "com.stockpicker.common.repository")
@EnableScheduling
public class IngestorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngestorApplication.class, args);
    }
}
