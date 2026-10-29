package com.iglesia.api.aportes.repository;

import org.springframework.data.jpa.domain.Specification;

import com.iglesia.api.aportes.entity.Aporte;
import com.iglesia.api.aportes.model.CriterioAportes;

/**
 * Criterios del listado de aportes. Un valor nulo no restringe; los valores viajan siempre como parámetros enlazados.
 */
public final class EspecificacionesAporte {

	private static final String FECHA = "fecha";

	private static final String CATEGORIA = "categoria";

	private static final String ID = "id";

	private static final String MEDIO = "medio";

	private static final String MIEMBRO_ID = "miembroId";

	private static final String ANULADO = "anulado";

	private EspecificacionesAporte() {
	}

	public static Specification<Aporte> segun(CriterioAportes criterio, boolean incluirAnulados) {
		return Specification.allOf(entre(criterio), deCategoria(criterio.categoriaId()), conMedio(criterio),
				deMiembro(criterio.miembroId()), incluirAnulados ? Specification.unrestricted() : vigentes());
	}

	/** Ningún aporte: para quien no tiene ámbito de lectura. */
	public static Specification<Aporte> ninguno() {
		return (aporte, consulta, criterios) -> criterios.disjunction();
	}

	private static Specification<Aporte> entre(CriterioAportes criterio) {
		return (aporte, consulta, criterios) -> criterios.between(aporte.get(FECHA), criterio.desde(),
				criterio.hasta());
	}

	private static Specification<Aporte> deCategoria(Long categoriaId) {
		return categoriaId == null ? Specification.unrestricted()
				: (aporte, consulta, criterios) -> criterios.equal(aporte.get(CATEGORIA).get(ID), categoriaId);
	}

	private static Specification<Aporte> conMedio(CriterioAportes criterio) {
		return criterio.medio() == null ? Specification.unrestricted()
				: (aporte, consulta, criterios) -> criterios.equal(aporte.get(MEDIO), criterio.medio());
	}

	private static Specification<Aporte> deMiembro(Long miembroId) {
		return miembroId == null ? Specification.unrestricted()
				: (aporte, consulta, criterios) -> criterios.equal(aporte.get(MIEMBRO_ID), miembroId);
	}

	private static Specification<Aporte> vigentes() {
		return (aporte, consulta, criterios) -> criterios.isFalse(aporte.get(ANULADO));
	}

}
