package com.bookaura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookAuraApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookAuraApplication.class, args);
    }
}
