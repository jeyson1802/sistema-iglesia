package com.iglesia.api.consolidacion.mapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.dto.ContactoResponse;
import com.iglesia.api.consolidacion.dto.ContactosPorMedioResponse;
import com.iglesia.api.consolidacion.dto.PersonaBreveResponse;
import com.iglesia.api.consolidacion.dto.PersonaCasoResponse;
import com.iglesia.api.consolidacion.dto.ResultadoConsolidacionResponse;
import com.iglesia.api.consolidacion.dto.RondaResponse;
import com.iglesia.api.consolidacion.dto.UltimoContactoResponse;
import com.iglesia.api.consolidacion.entity.ContactoConsolidacion;
import com.iglesia.api.consolidacion.entity.ResultadoConsolidacion;
import com.iglesia.api.consolidacion.entity.RondaConsolidacion;
import com.iglesia.api.consolidacion.model.EstadoRonda;
import com.iglesia.api.consolidacion.model.MedioContacto;
import com.iglesia.api.consolidacion.model.ResultadoContacto;
import com.iglesia.api.consolidacion.model.ResumenRonda;
import com.iglesia.api.miembros.model.PersonaConsolidacion;

/** Conversión entre las entidades de consolidación y sus DTO. Los nombres llegan ya resueltos en mapas. */
@Component
public class ConsolidacionMapper {

	public static final Duration VENTANA_EDICION = Duration.ofHours(LimitesConsolidacion.HORAS_EDICION_CONTACTO);

	public ResultadoConsolidacionResponse aResultado(ResultadoConsolidacion resultado) {
		return new ResultadoConsolidacionResponse(resultado.getId(), resultado.getNombre(), resultado.getOrden(),
				resultado.isActivo());
	}

	public RondaResponse aRonda(RondaConsolidacion ronda, ResumenRonda resumen) {
		return new RondaResponse(ronda.getId(), ronda.getNombre(), ronda.getDescripcion(), ronda.getFechaInicio(),
				ronda.getFechaFin(), ronda.getEstado(), resumen.personas(), resumen.pendientes(), resumen.enContacto(),
				resumen.cerrados(), resumen.noConcluidos(), resumen.sinAsignar(), resumen.vencidos());
	}

	public ReferenciaResponse aReferencia(RondaConsolidacion ronda) {
		return ronda == null ? null
				: new ReferenciaResponse(ronda.getId(), ronda.getNombre(), ronda.getEstado() != EstadoRonda.CERRADA);
	}

	public ReferenciaResponse aReferencia(ResultadoConsolidacion resultado) {
		return resultado == null ? null
				: new ReferenciaResponse(resultado.getId(), resultado.getNombre(), resultado.isActivo());
	}

	public ReferenciaResponse aReferencia(PersonaConsolidacion persona) {
		return persona == null ? null
				: new ReferenciaResponse(persona.id(), persona.nombreCompleto(), persona.activo());
	}

	public PersonaBreveResponse aBreve(PersonaConsolidacion persona) {
		return new PersonaBreveResponse(persona.id(), persona.nombreCompleto());
	}

	public PersonaCasoResponse aPersona(PersonaConsolidacion persona) {
		return new PersonaCasoResponse(persona.id(), persona.nombreCompleto(), persona.telefono(), persona.condicion(),
				persona.fechaIngreso(), persona.activo());
	}

	public ContactosPorMedioResponse porMedio(Collection<ContactoConsolidacion> contactos) {
		return new ContactosPorMedioResponse(contactos.size(), contar(contactos, MedioContacto.LLAMADA),
				contar(contactos, MedioContacto.WHATSAPP), contar(contactos, MedioContacto.VISITA),
				contar(contactos, MedioContacto.EN_LA_IGLESIA),
				contactos.stream().filter(contacto -> contacto.getResultado() == ResultadoContacto.SE_HABLO).count());
	}

	/** El primero de la lista (que llega del más reciente al más antiguo), o {@code null}. */
	public UltimoContactoResponse aUltimo(List<ContactoConsolidacion> contactos) {
		if (contactos.isEmpty()) {
			return null;
		}
		ContactoConsolidacion ultimo = contactos.getFirst();
		return new UltimoContactoResponse(ultimo.getFechaHora(), ultimo.getMedio(), ultimo.getResultado());
	}

	/**
	 * @param usuarioId usuario del token, para saber si lo registró él
	 * @param coordinador el coordinador puede corregir o borrar cualquier contacto, siempre
	 */
	public ContactoResponse aContacto(ContactoConsolidacion contacto, Map<Long, PersonaConsolidacion> personas,
			Long usuarioId, boolean coordinador, Instant ahora) {
		boolean propio = contacto.getRegistradoPor().equals(usuarioId);
		Instant editableHasta = contacto.getRegistradoEn().plus(VENTANA_EDICION);
		boolean editable = coordinador || (propio && ahora.isBefore(editableHasta));
		return new ContactoResponse(contacto.getId(), contacto.getCasoId(), contacto.getFechaHora(),
				contacto.getMedio(), contacto.getResultado(), contacto.getObservaciones(),
				contacto.getProximoContacto(), aReferencia(personas.get(contacto.getConsolidadorId())), propio,
				editableHasta, editable);
	}

	private static long contar(Collection<ContactoConsolidacion> contactos, MedioContacto medio) {
		return contactos.stream().filter(contacto -> contacto.getMedio() == medio).count();
	}

}
