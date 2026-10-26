package com.iglesia.seguridad.rol.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.common.error.exception.ReglaNegocioException;
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

	/**
	 * Roles que se pueden asignar a un usuario de esa iglesia: roles base o roles personalizados activos de la
	 * misma iglesia. Un rol de otra iglesia se trata como inexistente para no revelar su existencia.
	 * @throws ReglaNegocioException si algún rol no existe, es de otra iglesia o está inactivo
	 */
	@Transactional(readOnly = true)
	public Set<Rol> rolesAsignables(Set<Long> ids, Long iglesiaId) {
		Set<Rol> encontrados = new HashSet<>(roles.findByIdIn(ids));
		if (encontrados.size() != ids.size()
				|| encontrados.stream().anyMatch(rol -> !rol.esBase() && !rol.getIglesiaId().equals(iglesiaId))) {
			throw new ReglaNegocioException("Algún rol no existe o no pertenece a la iglesia del usuario");
		}
		if (encontrados.stream().anyMatch(rol -> !rol.isActivo())) {
			throw new ReglaNegocioException("No se puede asignar un rol inactivo");
		}
		return encontrados;
	}

}
