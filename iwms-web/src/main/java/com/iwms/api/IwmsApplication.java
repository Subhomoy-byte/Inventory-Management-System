package com.iwms.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the IWMS Spring Boot host application.
 * Bootstraps embedded Tomcat server, registers REST controllers, and serves static React PWA assets.
 */
@SpringBootApplication
public class IwmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(IwmsApplication.class, args);
    }
}
