package com.msuzuki.configserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.ConfigurableApplicationContext;

@EnableConfigServer
@SpringBootApplication
@Slf4j
public class ConfigServerApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext ctx = SpringApplication.run(ConfigServerApplication.class, args);

        String repoLocation = ctx.getEnvironment().getProperty("spring.cloud.config.server.native.searchLocations");
        log.info("Serving configurations from folder: " + repoLocation);
    }

}
