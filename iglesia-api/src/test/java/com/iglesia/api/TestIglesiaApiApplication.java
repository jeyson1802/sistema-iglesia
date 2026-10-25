package com.iglesia.api;

import org.springframework.boot.SpringApplication;

public class TestIglesiaApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(IglesiaApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
