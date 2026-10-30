package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.service.AsistenciaParaConsolidacion;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.dto.FiltroLoteRequest;
import com.iglesia.api.consolidacion.dto.LoteAgregadoResponse;
import com.iglesia.api.consolidacion.dto.VistaPreviaLoteResponse;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.RondaConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.CriteriosLote;
import com.iglesia.api.consolidacion.repository.CasoConsolidacionRepository;
import com.iglesia.api.consolidacion.repository.ContactoConsolidacionRepository;
import com.iglesia.api.miembros.model.PersonaConsolidacion;
import com.iglesia.api.miembros.service.MiembrosParaConsolidacion;

/**
 * Lote de una ronda: las personas que entran según los filtros (o elegidas a mano). Todo se resuelve en la base, en
 * pocas consultas, aunque la iglesia tenga miles de miembros. Quien ya tiene un seguimiento abierto no entra.
 */
@Service
public class LoteService {

	private static final Logger LOG = LoggerFactory.getLogger(LoteService.class);

	static final String LOTE_DEMASIADO_GRANDE = "El lote tendría %d personas; el máximo es %d. Ajusta los filtros";

	static final String CASO_NO_ENCONTRADO = "La persona no está en esta ronda";

	static final String CASO_CON_CONTACTOS = "No se puede quitar a una persona que ya tiene contactos registrados";

	private static final String SEPARADOR_IDS = ",";

	private static final String SIN_IDS = "";

	private final RondaService rondas;

	private final CasoConsolidacionRepository casos;

	private final ContactoConsolidacionRepository contactos;

	private final AsistenciaParaConsolidacion asistencia;

	private final MiembrosParaConsolidacion miembros;

	private final ConsolidacionMapper mapper;

	private final IglesiaActual iglesiaActual;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	LoteService(RondaService rondas, CasoConsolidacionRepository casos, ContactoConsolidacionRepository contactos,
			AsistenciaParaConsolidacion asistencia, MiembrosParaConsolidacion miembros, ConsolidacionMapper mapper,
			IglesiaActual iglesiaActual, UsuarioActual usuarioActual, Clock reloj) {
		this.rondas = rondas;
		this.casos = casos;
		this.contactos = contactos;
		this.asistencia = asistencia;
		this.miembros = miembros;
		this.mapper = mapper;
		this.iglesiaActual = iglesiaActual;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public VistaPreviaLoteResponse vistaPrevia(Long rondaId, FiltroLoteRequest filtro) {
		RondaConsolidacion ronda = rondas.editable(rondaId);
		CriteriosLote criterios = criterios(filtro);
		long iglesiaId = iglesiaActual.requerida();
		long entrarian = contar(iglesiaId, ronda.getId(), criterios);
		long conCasoAbierto = contarConCasoAbierto(iglesiaId, ronda.getId(), criterios);
		// Todas las que entrarían (hasta el tope de un lote), para que el coordinador vea a quiénes agrega.
		List<Long> ids = casos.idsLote(iglesiaId, ronda.getId(), criterios.condicion(),
				criterios.ingresoDesde(), criterios.ingresoHasta(), criterios.grupoId(), criterios.sinGrupo(),
				criterios.nuncaConsolidados(), criterios.filtrarIds(), criterios.ids(), LimitesConsolidacion.LOTE_MAX);
		Map<Long, PersonaConsolidacion> personas = miembros.porIds(ids);
		return new VistaPreviaLoteResponse(entrarian, conCasoAbierto,
				ids.stream().map(personas::get).map(mapper::aBreve).toList());
	}

	/** Agrega en una sola sentencia; más de {@link LimitesConsolidacion#LOTE_MAX} personas responde 400. */
	@Transactional
	public LoteAgregadoResponse agregar(Long rondaId, FiltroLoteRequest filtro) {
		RondaConsolidacion ronda = rondas.editable(rondaId);
		CriteriosLote criterios = criterios(filtro);
		long iglesiaId = iglesiaActual.requerida();
		long entrarian = contar(iglesiaId, ronda.getId(), criterios);
		if (entrarian > LimitesConsolidacion.LOTE_MAX) {
			throw new ReglaNegocioException(LOTE_DEMASIADO_GRANDE.formatted(entrarian, LimitesConsolidacion.LOTE_MAX));
		}
		long conCasoAbierto = contarConCasoAbierto(iglesiaId, ronda.getId(), criterios);
		int agregadas = casos.agregarLote(iglesiaId, ronda.getId(), criterios.condicion(), criterios.ingresoDesde(),
				criterios.ingresoHasta(), criterios.grupoId(), criterios.sinGrupo(), criterios.nuncaConsolidados(),
				criterios.filtrarIds(), criterios.ids(), usuario(), Instant.now(reloj));
		LOG.info("Lote agregado a la ronda rondaId={} personas={} omitidas={} por usuarioId={}", rondaId, agregadas,
				conCasoAbierto, usuario());
		return new LoteAgregadoResponse(agregadas, conCasoAbierto);
	}

	/** Quita a una persona del lote mientras su caso no tenga contactos. */
	@Transactional
	public void quitar(Long rondaId, Long casoId) {
		rondas.editable(rondaId);
		CasoConsolidacion caso = casos.findById(casoId)
			.filter(encontrado -> rondaId.equals(encontrado.getRondaId()))
			.orElseThrow(() -> new RecursoNoEncontradoException(CASO_NO_ENCONTRADO));
		if (contactos.findFirstByCasoIdOrderByFechaHoraDescIdDesc(casoId).isPresent()) {
			throw new ConflictoException(CASO_CON_CONTACTOS);
		}
		casos.delete(caso);
		LOG.info("Persona quitada del lote rondaId={} casoId={} por usuarioId={}", rondaId, casoId, usuario());
	}

	/**
	 * Las personas elegidas a mano y las que tienen la alerta se combinan: si hay ambas, solo entran las que cumplen
	 * las dos.
	 */
	private CriteriosLote criterios(FiltroLoteRequest filtro) {
		Set<Long> ids = null;
		if (filtro.miembroIds() != null && !filtro.miembroIds().isEmpty()) {
			ids = new HashSet<>(filtro.miembroIds());
		}
		if (filtro.alerta() != null) {
			Set<Long> conAlerta = asistencia.conAlerta(filtro.alerta());
			if (ids == null) {
				ids = new HashSet<>(conAlerta);
			}
			else {
				ids.retainAll(conAlerta);
			}
		}
		String condicion = filtro.condicion() == null ? null : filtro.condicion().name();
		String lista = ids == null ? SIN_IDS
				: ids.stream().map(String::valueOf).collect(Collectors.joining(SEPARADOR_IDS));
		return new CriteriosLote(condicion, filtro.ingresoDesde(), filtro.ingresoHasta(), filtro.grupoId(),
				filtro.soloSinGrupo(), filtro.soloNuncaConsolidados(), ids != null, lista);
	}

	private long contar(long iglesiaId, long rondaId, CriteriosLote c) {
		return casos.contarLote(iglesiaId, rondaId, c.condicion(), c.ingresoDesde(), c.ingresoHasta(), c.grupoId(),
				c.sinGrupo(), c.nuncaConsolidados(), c.filtrarIds(), c.ids());
	}

	private long contarConCasoAbierto(long iglesiaId, long rondaId, CriteriosLote c) {
		return casos.contarConCasoAbierto(iglesiaId, rondaId, c.condicion(), c.ingresoDesde(), c.ingresoHasta(),
				c.grupoId(), c.sinGrupo(), c.nuncaConsolidados(), c.filtrarIds(), c.ids());
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
