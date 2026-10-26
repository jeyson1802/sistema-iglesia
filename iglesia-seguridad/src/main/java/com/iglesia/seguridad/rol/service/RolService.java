package com.iglesia.seguridad.rol.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.rol.repository.RolRepository;

/** Consulta de roles de acceso para los demás contextos. */
@Service
public class RolService {

	private final RolRepository roles;

	RolService(RolRepository roles) {
		this.roles = roles;
	}

	/** Roles base con esos códigos. Flyway siembra los 4, así que nunca falta ninguno. */
	@Transactional(readOnly = true)
	public Set<Rol> rolesBase(Set<RolBase> codigos) {
		Set<Rol> encontrados = new HashSet<>(roles.findByCodigoIn(codigos));
		if (encontrados.size() != codigos.size()) {
			throw new IllegalStateException("Faltan roles base sembrados por Flyway: " + codigos);
		}
		return encontrados;
	}

}
