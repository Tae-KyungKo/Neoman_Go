package com.neomango;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NeomangoApplication {

	public static void main(String[] args) {
		SpringApplication.run(NeomangoApplication.class, args);
	}

}

