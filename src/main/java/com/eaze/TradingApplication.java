package com.eaze;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TradingApplication {

	public static void main(String[] args) {
        SpringApplication.run(TradingApplication.class, args);
//        String[] beanName = ctxs.getBeanDefinitionNames();
//        for (String name : beanName) {
//            System.out.println(name);
//        }
	}

}
