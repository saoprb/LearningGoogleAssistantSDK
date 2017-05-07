package com.sao.learning.google.assistant;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Created by Sao Paulo BOCO on 5/7/2017.
 */
@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan
public class ApplicationMain {

    public static void main(String[] args) {
        SpringApplication.run(ApplicationMain.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(ApplicationContext context) {
        return args -> System.out.println("Hello world");
    }
}
