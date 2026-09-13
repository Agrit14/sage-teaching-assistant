package com.sage.teachingassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SageTeachingAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SageTeachingAssistantApplication.class, args);
    }
}
