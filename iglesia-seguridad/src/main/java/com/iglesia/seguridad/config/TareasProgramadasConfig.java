package com.iglesia.seguridad.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas (p. ej. la limpieza de la actividad antigua). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class TareasProgramadasConfig {
}
