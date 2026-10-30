package com.iglesia.api.consolidacion.repository;

import java.time.LocalDate;
import java.util.Collection;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.entity.RondaConsolidacion;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.EstadoRonda;
import com.iglesia.api.consolidacion.model.ResultadoContacto;
import com.iglesia.api.consolidacion.model.SeccionMisCasos;

/** Filtros de rondas y casos. Un filtro nulo no restringe. */
public final class EspecificacionesConsolidacion {

	private static final String ID = "id";

	private static final String NOMBRE = "nombre";

	private static final String ESTADO = "estado";

	private static final String RONDA = "rondaId";

	private static final String MIEMBRO = "miembroId";

	private static final String CONSOLIDADOR = "consolidadorId";

	private static final String PROXIMO = "proximoContacto";

	private static final String CASO = "casoId";

	private static final String RESULTADO = "resultado";

	private static final String COMODIN = "%";

	private EspecificacionesConsolidacion() {
	}

	public static Specification<RondaConsolidacion> rondaConEstado(EstadoRonda estado) {
		return estado == null ? Specification.unrestricted()
				: (raiz, consulta, cb) -> cb.equal(raiz.get(ESTADO), estado);
	}

	public static Specification<RondaConsolidacion> rondaConNombre(String texto) {
		if (texto == null || texto.isBlank()) {
			return Specification.unrestricted();
		}
		String patron = COMODIN + texto.trim().toLowerCase() + COMODIN;
		return (raiz, consulta, cb) -> cb.like(cb.lower(raiz.get(NOMBRE)), patron);
	}

	public static Specification<CasoConsolidacion> deRonda(Long rondaId) {
		return (raiz, consulta, cb) -> cb.equal(raiz.get(RONDA), rondaId);
	}

	public static Specification<CasoConsolidacion> conEstado(EstadoCaso estado) {
		return estado == null ? Specification.unrestricted()
				: (raiz, consulta, cb) -> cb.equal(raiz.get(ESTADO), estado);
	}

	public static Specification<CasoConsolidacion> deConsolidador(Long consolidadorId) {
		return consolidadorId == null ? Specification.unrestricted()
				: (raiz, consulta, cb) -> cb.equal(raiz.get(CONSOLIDADOR), consolidadorId);
	}

	public static Specification<CasoConsolidacion> sinAsignar(boolean soloSinAsignar) {
		return soloSinAsignar ? (raiz, consulta, cb) -> cb.isNull(raiz.get(CONSOLIDADOR))
				: Specification.unrestricted();
	}

	/** Solo los de estas personas; {@code null} no restringe y una colección vacía no deja ninguno. */
	public static Specification<CasoConsolidacion> dePersonas(Collection<Long> miembroIds) {
		if (miembroIds == null) {
			return Specification.unrestricted();
		}
		if (miembroIds.isEmpty()) {
			return (raiz, consulta, cb) -> cb.disjunction();
		}
		return (raiz, consulta, cb) -> raiz.get(MIEMBRO).in(miembroIds);
	}

	/** Casos abiertos que el consolidador ya puede trabajar: individuales o de rondas en curso. */
	public static Specification<CasoConsolidacion> abiertosVisibles() {
		return (raiz, consulta, cb) -> {
			Subquery<Long> enCurso = consulta.subquery(Long.class);
			var ronda = enCurso.from(RondaConsolidacion.class);
			enCurso.select(ronda.get(ID))
				.where(cb.equal(ronda.get(ID), raiz.get(RONDA)), cb.equal(ronda.get(ESTADO), EstadoRonda.EN_CURSO));
			return cb.and(cb.notEqual(raiz.get(ESTADO), EstadoCaso.CERRADO),
					cb.or(cb.isNull(raiz.get(RONDA)), cb.exists(enCurso)));
		};
	}

	/**
	 * Casos que cuentan para el avance del consolidador: los abiertos que ya puede trabajar y los cerrados de rondas
	 * en curso.
	 */
	public static Specification<CasoConsolidacion> vigentes() {
		return (raiz, consulta, cb) -> {
			Subquery<Long> enCurso = consulta.subquery(Long.class);
			var ronda = enCurso.from(RondaConsolidacion.class);
			enCurso.select(ronda.get(ID))
				.where(cb.equal(ronda.get(ID), raiz.get(RONDA)), cb.equal(ronda.get(ESTADO), EstadoRonda.EN_CURSO));
			return cb.or(cb.and(cb.isNull(raiz.get(RONDA)), cb.notEqual(raiz.get(ESTADO), EstadoCaso.CERRADO)),
					cb.exists(enCurso));
		};
	}

	/** Casos con al menos un contacto en que se habló con la persona. */
	public static Specification<CasoConsolidacion> conRespuesta() {
		return (raiz, consulta, cb) -> {
			Subquery<Long> hablado = consulta.subquery(Long.class);
			var contacto = hablado.from(ContactoConsolidacion.class);
			hablado.select(contacto.get(ID))
				.where(cb.equal(contacto.get(CASO), raiz.get(ID)),
						cb.equal(contacto.get(RESULTADO), ResultadoContacto.SE_HABLO));
			return cb.exists(hablado);
		};
	}

	public static Specification<CasoConsolidacion> enSeccion(SeccionMisCasos seccion, LocalDate hoy) {
		return switch (seccion) {
			case PARA_HOY -> (raiz, consulta, cb) -> cb.lessThanOrEqualTo(raiz.get(PROXIMO), hoy);
			case SIN_CONTACTAR -> (raiz, consulta, cb) -> cb.and(cb.equal(raiz.get(ESTADO), EstadoCaso.PENDIENTE),
					sinVencer(raiz.get(PROXIMO), hoy, cb));
			case EN_CONTACTO -> (raiz, consulta, cb) -> cb.and(cb.equal(raiz.get(ESTADO), EstadoCaso.EN_CONTACTO),
					sinVencer(raiz.get(PROXIMO), hoy, cb));
		};
	}

	private static Predicate sinVencer(Path<LocalDate> proximo, LocalDate hoy, CriteriaBuilder cb) {
		return cb.or(cb.isNull(proximo), cb.greaterThan(proximo, hoy));
	}

}
