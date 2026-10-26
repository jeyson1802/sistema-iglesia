package com.iglesia.seguridad.usuario.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Credenciales del primer administrador del sistema (solo por variables de entorno). */
@ConfigurationProperties("seguridad.admin-inicial")
public record AdministradorInicialProperties(String correo, String contrasena) {

	public boolean estaConfigurado() {
		return StringUtils.hasText(correo) && StringUtils.hasText(contrasena);
	}

	@Override
	public String toString() {
		return "AdministradorInicialProperties[***]";
	}

}
