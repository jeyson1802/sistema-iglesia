package com.iglesia.api.asistencia.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.iglesia.api.asistencia.entity.Reunion;
import com.iglesia.api.asistencia.model.AlcanceAsistencia;
import com.iglesia.api.asistencia.model.TipoReunion;

/** Filtros del historial de reuniones. Un filtro nulo no restringe. */
public final class EspecificacionesReunion {

	private static final String FECHA = "fecha";

	private static final String TIPO = "tipo";

	private static final String GRUPO = "grupoId";

	private EspecificacionesReunion() {
	}

	/** Todas si el alcance es completo; si no, solo las reuniones de sus grupos. */
	public static Specification<Reunion> dentroDe(AlcanceAsistencia alcance) {
		if (alcance.todas()) {
			return Specification.unrestricted();
		}
		if (alcance.grupoIds().isEmpty()) {
			return (raiz, consulta, cb) -> cb.disjunction();
		}
		return (raiz, consulta, cb) -> cb.and(cb.equal(raiz.get(TIPO), TipoReunion.GRUPO),
				raiz.get(GRUPO).in(alcance.grupoIds()));
	}

	public static Specification<Reunion> desde(LocalDate desde) {
		return desde == null ? Specification.unrestricted()
				: (raiz, consulta, cb) -> cb.greaterThanOrEqualTo(raiz.get(FECHA), desde);
	}

	public static Specification<Reunion> hasta(LocalDate hasta) {
		return hasta == null ? Specification.unrestricted()
				: (raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get(FECHA), hasta);
	}

	public static Specification<Reunion> deTipo(TipoReunion tipo) {
		return tipo == null ? Specification.unrestricted() : (raiz, consulta, cb) -> cb.equal(raiz.get(TIPO), tipo);
	}

	public static Specification<Reunion> deGrupo(Long grupoId) {
		return grupoId == null ? Specification.unrestricted()
				: (raiz, consulta, cb) -> cb.equal(raiz.get(GRUPO), grupoId);
	}

}
