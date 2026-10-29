package com.iglesia.api.miembros.service;

import java.util.Collection;
import java.util.Map;

import com.iglesia.api.common.api.dto.ReferenciaResponse;

/**
 * Lo que el registro de miembros necesita saber de los grupos. Lo implementa el contexto de grupos, que depende de
 * miembros; así la dependencia entre contextos va en un solo sentido.
 */
public interface ConsultaGrupos {

	/**
	 * Valida el grupo que se asigna a un miembro: si no cambia se acepta aunque esté inactivo; si cambia, debe existir
	 * en la iglesia y estar activo (409 sobre {@code grupoId}).
	 */
	void validarAsignable(Long nuevoId, Long actualId);

	/**
	 * Referencias de los grupos indicados, por id. Un grupo no tiene nombre: el {@code nombre} de la referencia es el
	 * de su líder. Los ids nulos o inexistentes no aparecen.
	 */
	Map<Long, ReferenciaResponse> referencias(Collection<Long> ids);

	/** Si el miembro lidera algún grupo activo (impide desactivarlo). */
	boolean lideraAlgunGrupoActivo(Long miembroId);

}
