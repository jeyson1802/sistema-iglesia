package com.iglesia.api.miembros.service;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.repository.MiembroRepository;

/**
 * Reglas de la jerarquía de liderazgo: nadie es su propio líder, el pastor principal no tiene líder, no hay ciclos
 * (la red de un miembro no puede incluir a su líder) y no se desactiva a quien sostiene parte del árbol. La base de
 * datos respalda las mismas reglas con {@code CHECK} y el índice único parcial del pastor.
 */
@Component
class PoliticaJerarquia {

	static final String CAMPO_LIDER = "liderId";

	static final String PROPIO_LIDER = "Un miembro no puede ser su propio líder";

	static final String PASTOR_SIN_LIDER = "El pastor principal no tiene líder";

	static final String CICLO = "No se puede asignar como líder a alguien de su propia red: se formaría un ciclo";

	static final String DESACTIVAR_PASTOR = "No se puede desactivar al pastor principal: designe antes a otro";

	static final String DESACTIVAR_CON_LIDERADOS = "No se puede desactivar: lidera miembros activos. Asígneles otro "
			+ "líder antes";

	static final String DESACTIVAR_CON_GRUPOS = "No se puede desactivar: lidera un grupo activo. Asigne otro líder "
			+ "al grupo antes";

	private final MiembroRepository miembros;

	private final IglesiaActual iglesiaActual;

	private final ConsultaGrupos grupos;

	PoliticaJerarquia(MiembroRepository miembros, IglesiaActual iglesiaActual, ConsultaGrupos grupos) {
		this.miembros = miembros;
		this.iglesiaActual = iglesiaActual;
		this.grupos = grupos;
	}

	/** Antes de resolver el líder: un miembro existente no puede indicarse a sí mismo (400). */
	void validarNoEsSuPropioLider(Miembro miembro, Long liderId) {
		if (miembro != null && liderId != null && liderId.equals(miembro.getId())) {
			throw new ReglaNegocioException(PROPIO_LIDER, CAMPO_LIDER);
		}
	}

	/** Al cambiar el líder de un miembro existente: ni el pastor tiene líder ni se admiten ciclos (409). */
	void validarNuevoLider(Miembro miembro, Miembro nuevoLider) {
		if (miembro == null || nuevoLider == null) {
			return;
		}
		if (miembro.isPastorPrincipal()) {
			throw new ConflictoException(PASTOR_SIN_LIDER, CAMPO_LIDER);
		}
		if (miembros.esDeLaRed(iglesiaActual.requerida(), miembro.getId(), nuevoLider.getId())) {
			throw new ConflictoException(CICLO, CAMPO_LIDER);
		}
	}

	void validarDesactivacion(Miembro miembro) {
		if (miembro.isPastorPrincipal()) {
			throw new ConflictoException(DESACTIVAR_PASTOR);
		}
		if (miembros.countByLiderIdAndActivoTrue(miembro.getId()) > 0) {
			throw new ConflictoException(DESACTIVAR_CON_LIDERADOS);
		}
		if (grupos.lideraAlgunGrupoActivo(miembro.getId())) {
			throw new ConflictoException(DESACTIVAR_CON_GRUPOS);
		}
	}

}
