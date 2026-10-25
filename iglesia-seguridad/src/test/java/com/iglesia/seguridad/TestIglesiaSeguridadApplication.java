package com.iglesia.seguridad;

import org.springframework.boot.SpringApplication;

public class TestIglesiaSeguridadApplication {

	public static void main(String[] args) {
		SpringApplication.from(IglesiaSeguridadApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
