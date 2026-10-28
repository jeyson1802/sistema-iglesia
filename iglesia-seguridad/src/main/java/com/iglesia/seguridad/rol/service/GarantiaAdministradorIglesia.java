package com.iglesia.seguridad.rol.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.common.error.exception.ConflictoException;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;
import com.iglesia.seguridad.rol.repository.RolRepository;

/**
 * Impide que una iglesia se quede sin quien administre sus usuarios, sea por cambiar un usuario, el estado de
 * un rol o sus permisos. Se verifica el resultado del cambio en lugar de simular cada camino: toda operación que
 * cambie usuarios o roles de una iglesia debe llamarla al final de su transacción.
 */
@Service
public class GarantiaAdministradorIglesia {

	public static final String SIN_ADMINISTRADOR =
			"La iglesia debe conservar al menos un usuario activo que pueda administrar usuarios";

	private final RolRepository roles;

	GarantiaAdministradorIglesia(RolRepository roles) {
		this.roles = roles;
	}

	/**
	 * Escribe los cambios pendientes y cuenta los administradores activos que quedan. El bloqueo por iglesia
	 * serializa la verificación: dos administradores que se desactivan entre sí a la vez no pueden pasar los dos.
	 * Va en su propia sentencia, antes de contar, para que el conteo vea lo que confirmó la transacción que tenía
	 * el bloqueo.
	 * @throws ConflictoException si no queda ninguno; la transacción se revierte
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void verificarQueQuedaAdministrador(Long iglesiaId) {
		roles.flush();
		roles.bloquearAdministracionDeIglesia(iglesiaId);
		if (roles.contarAdministradoresActivos(iglesiaId, Permisos.USUARIOS_ADMINISTRAR) == 0) {
			throw new ConflictoException(SIN_ADMINISTRADOR);
		}
	}

}
