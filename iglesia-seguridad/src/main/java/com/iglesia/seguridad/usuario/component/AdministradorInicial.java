package com.iglesia.seguridad.usuario.component;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.service.RolService;
import com.iglesia.seguridad.usuario.config.AdministradorInicialProperties;
import com.iglesia.seguridad.usuario.entity.Usuario;
import com.iglesia.seguridad.usuario.repository.UsuarioRepository;

/**
 * Crea el primer administrador del sistema al arrancar, si no existe ninguno y se configuraron sus
 * credenciales. Evita tener usuarios o contraseñas por defecto en el código o en las migraciones.
 */
@Component
class AdministradorInicial implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(AdministradorInicial.class);

	private final AdministradorInicialProperties propiedades;

	private final UsuarioRepository usuarios;

	private final PasswordEncoder codificador;

	private final RolService roles;

	AdministradorInicial(AdministradorInicialProperties propiedades, UsuarioRepository usuarios,
			PasswordEncoder codificador, RolService roles) {
		this.propiedades = propiedades;
		this.usuarios = usuarios;
		this.codificador = codificador;
		this.roles = roles;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments argumentos) {
		if (!propiedades.estaConfigurado() || usuarios.existeConRol(RolBase.ADMIN_SISTEMA)) {
			return;
		}
		usuarios.save(Usuario.nuevo(propiedades.correo(), codificador.encode(propiedades.contrasena()), null, null,
				roles.rolesBase(Set.of(RolBase.ADMIN_SISTEMA))));
		LOG.info("Administrador del sistema inicial creado");
	}

}
