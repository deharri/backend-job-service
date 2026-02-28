package com.deharri.jlds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class JldsApplication {

	public static void main(String[] args) {
		SpringApplication.run(JldsApplication.class, args);
	}

}
