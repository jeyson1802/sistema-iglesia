package com.iglesia.api.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj de la aplicación (zona del sistema), inyectable para que los cálculos con fechas se puedan probar. */
@Configuration(proxyBeanMethods = false)
class RelojConfig {

	@Bean
	Clock reloj() {
		return Clock.systemDefaultZone();
	}

}
