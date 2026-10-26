package com.iglesia.seguridad.usuario.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.usuario.entity.Usuario;
import com.iglesia.seguridad.usuario.model.Correo;
import com.iglesia.seguridad.usuario.repository.UsuarioRepository;

/** Carga las credenciales para el login. El nombre de usuario es el correo. */
@Service
class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarios;

	UsuarioDetailsService(UsuarioRepository usuarios) {
		this.usuarios = usuarios;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String correo) {
		Usuario usuario = usuarios.findByCorreo(Correo.normalizar(correo))
			.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
		return User.withUsername(usuario.getCorreo())
			.password(usuario.getHashContrasena())
			.disabled(!usuario.isActivo())
			.roles(usuario.getRoles().stream().map(Enum::name).toArray(String[]::new))
			.build();
	}

}
