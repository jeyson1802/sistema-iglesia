package com.iglesia.seguridad.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TiempoConfig {

	/** Reloj inyectable para que la lógica dependiente del tiempo sea testeable. */
	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}

}
