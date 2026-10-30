package com.iglesia.api.consolidacion.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.dto.ContactoResponse;
import com.iglesia.api.consolidacion.dto.RegistrarContactoRequest;
import com.iglesia.api.consolidacion.entity.CasoConsolidacion;
import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.mapper.ConsolidacionMapper;
import com.iglesia.api.consolidacion.model.DatosContacto;
import com.iglesia.api.consolidacion.repository.ContactoConsolidacionRepository;

/**
 * Contactos (llamadas, mensajes, visitas) de un caso. El primero pasa el caso a «en contacto»; corregir o borrar
 * recalcula el estado y el próximo contacto del caso. Las observaciones nunca se registran en los logs.
 */
@Service
public class ContactoService {

	private static final Logger LOG = LoggerFactory.getLogger(ContactoService.class);

	static final String NO_ENCONTRADO = "Contacto no encontrado";

	static final String FECHA_FUTURA = "La fecha del contacto no puede ser futura";

	static final String PROXIMO_ANTERIOR = "El próximo contacto debe ser el mismo día del contacto o después";

	static final String RESULTADO_NO_APLICA = "Ese resultado no corresponde al medio elegido";

	static final String SIN_CONSOLIDADOR = "Indica quién hizo el contacto";

	static final String SOLO_COORDINADOR = "Solo el coordinador puede registrar contactos en nombre de otra persona";

	static final String CASO_CERRADO = "El caso está cerrado: reábrelo para registrar contactos";

	static final String NO_ES_PROPIO = "Solo quien registró el contacto o el coordinador pueden cambiarlo";

	static final String VENTANA_VENCIDA = "Pasaron más de %d horas: el contacto ya forma parte del historial";

	private static final Duration TOLERANCIA_RELOJ = Duration.ofMinutes(LimitesConsolidacion.TOLERANCIA_RELOJ_MINUTOS);

	private final ContactoConsolidacionRepository contactos;

	private final CasoService casos;

	private final EquipoConsolidacionService equipo;

	private final ResumenesDeCasos resumenes;

	private final AmbitoConsolidacion ambito;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	ContactoService(ContactoConsolidacionRepository contactos, CasoService casos, EquipoConsolidacionService equipo,
			ResumenesDeCasos resumenes, AmbitoConsolidacion ambito, UsuarioActual usuarioActual, Clock reloj) {
		this.contactos = contactos;
		this.casos = casos;
		this.equipo = equipo;
		this.resumenes = resumenes;
		this.ambito = ambito;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	@Transactional
	public ContactoResponse registrar(Long casoId, RegistrarContactoRequest solicitud) {
		CasoConsolidacion caso = abierto(casos.paraActuar(casoId));
		ContactoConsolidacion contacto = contactos
			.save(ContactoConsolidacion.nuevo(casoId, datos(caso, solicitud), usuario(), Instant.now(reloj)));
		recalcular(caso);
		LOG.info("Contacto registrado contactoId={} casoId={} medio={} resultado={} por usuarioId={}",
				contacto.getId(), casoId, contacto.getMedio(), contacto.getResultado(), usuario());
		return resumenes.contacto(contacto);
	}

	/** Corrige un contacto: quien lo registró, durante 24 horas; el coordinador, siempre. */
	@Transactional
	public ContactoResponse actualizar(Long id, RegistrarContactoRequest solicitud) {
		ContactoConsolidacion contacto = editable(id);
		CasoConsolidacion caso = casos.paraActuar(contacto.getCasoId());
		contacto.actualizar(datos(caso, solicitud));
		contactos.flush();
		if (caso.getEstado().abierto()) {
			recalcular(caso);
		}
		LOG.info("Contacto corregido contactoId={} casoId={} por usuarioId={}", id, caso.getId(), usuario());
		return resumenes.contacto(contacto);
	}

	@Transactional
	public void borrar(Long id) {
		ContactoConsolidacion contacto = editable(id);
		CasoConsolidacion caso = casos.paraActuar(contacto.getCasoId());
		contactos.delete(contacto);
		contactos.flush();
		if (caso.getEstado().abierto()) {
			recalcular(caso);
		}
		LOG.info("Contacto borrado contactoId={} casoId={} por usuarioId={}", id, caso.getId(), usuario());
	}

	/**
	 * Contacto que el usuario puede cambiar. Fuera de su ámbito, 404; de otro consolidador, 403; propio pero pasadas
	 * las 24 horas, 409.
	 */
	private ContactoConsolidacion editable(Long id) {
		ContactoConsolidacion contacto = contactos.findById(id)
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
		casos.visible(contacto.getCasoId());
		if (ambito.esCoordinador()) {
			return contacto;
		}
		if (!contacto.getRegistradoPor().equals(usuario())) {
			throw new AccessDeniedException(NO_ES_PROPIO);
		}
		Instant limite = contacto.getRegistradoEn().plus(ConsolidacionMapper.VENTANA_EDICION);
		if (!Instant.now(reloj).isBefore(limite)) {
			throw new ConflictoException(VENTANA_VENCIDA.formatted(LimitesConsolidacion.HORAS_EDICION_CONTACTO));
		}
		return contacto;
	}

	private CasoConsolidacion abierto(CasoConsolidacion caso) {
		if (!caso.getEstado().abierto()) {
			throw new ConflictoException(CASO_CERRADO);
		}
		return caso;
	}

	/** Estado y próximo contacto del caso según el contacto más reciente que quede. */
	private void recalcular(CasoConsolidacion caso) {
		Optional<ContactoConsolidacion> ultimo = contactos.findFirstByCasoIdOrderByFechaHoraDescIdDesc(caso.getId());
		caso.actualizarContactos(ultimo.isPresent(), ultimo.map(ContactoConsolidacion::getProximoContacto).orElse(null));
	}

	private DatosContacto datos(CasoConsolidacion caso, RegistrarContactoRequest solicitud) {
		Instant ahora = Instant.now(reloj);
		if (solicitud.fechaHora().isAfter(ahora.plus(TOLERANCIA_RELOJ))) {
			throw new ReglaNegocioException(FECHA_FUTURA, "fechaHora");
		}
		LocalDate dia = LocalDate.ofInstant(solicitud.fechaHora(), reloj.getZone());
		if (solicitud.proximoContacto() != null && solicitud.proximoContacto().isBefore(dia)) {
			throw new ReglaNegocioException(PROXIMO_ANTERIOR, "proximoContacto");
		}
		if (!solicitud.resultado().aplicaA(solicitud.medio())) {
			throw new ReglaNegocioException(RESULTADO_NO_APLICA, "resultado");
		}
		return new DatosContacto(solicitud.fechaHora(), solicitud.medio(), solicitud.resultado(),
				Textos.recortarOpcional(solicitud.observaciones()), solicitud.proximoContacto(),
				quienContacto(caso, solicitud.consolidadorId()));
	}

	/**
	 * Por defecto, el consolidador del caso (o el miembro del coordinador, si el caso aún no tiene). Solo el
	 * coordinador puede indicar a otra persona, que debe ser un miembro activo.
	 */
	private Long quienContacto(CasoConsolidacion caso, Long indicado) {
		Long porDefecto = Optional.ofNullable(caso.getConsolidadorId()).or(usuarioActual::miembroId).orElse(null);
		if (indicado == null || indicado.equals(porDefecto)) {
			return Optional.ofNullable(porDefecto)
				.orElseThrow(() -> new ReglaNegocioException(SIN_CONSOLIDADOR, "consolidadorId"));
		}
		if (!ambito.esCoordinador()) {
			throw new AccessDeniedException(SOLO_COORDINADOR);
		}
		return equipo.personaActiva(indicado).id();
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
