package com.iglesia.api.consolidacion.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.asistencia.dto.AsistenciaMiembroResponse;
import com.iglesia.api.asistencia.dto.ReunionAsistidaResponse;
import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.consolidacion.model.EstadoCaso;
import com.iglesia.api.consolidacion.model.EstadoRonda;

@Schema(description = "Caso con todo lo necesario para contactar a la persona.")
public record CasoDetalleResponse(
		@Schema(description = "Identificador.", example = "310") Long id,
		@Schema(description = "Ronda; vacía si es un seguimiento individual.") ReferenciaResponse ronda,
		@Schema(description = "Estado de la ronda; vacío si es individual.", example = "EN_CURSO") EstadoRonda estadoRonda,
		@Schema(description = "Estado del caso.", example = "EN_CONTACTO") EstadoCaso estado,
		@Schema(description = "Consolidador asignado.") ReferenciaResponse consolidador,
		@Schema(description = "Resultado, si está cerrado.") ReferenciaResponse resultado,
		@Schema(description = "Observación final.") String observacionCierre,
		@Schema(description = "Próximo contacto previsto.", example = "2026-11-10") LocalDate proximoContacto,
		@Schema(description = "Cuándo empezó.", example = "2026-11-02T13:00:00Z") Instant creadoEn,
		@Schema(description = "Cuándo se cerró.") Instant cerradoEn,
		@Schema(description = "Datos para contactarla.") ContactoPersonaResponse persona,
		@Schema(description = "Resumen de su historia de consolidación.") AntesDeContactarResponse antes,
		@Schema(description = "Contactos de este caso, del más reciente al más antiguo.") List<ContactoResponse> contactos,
		@Schema(description = "Reuniones a las que asistió desde que empezó el caso (para la línea de tiempo).") List<ReunionAsistidaResponse> asistencias,
		@Schema(description = "Resumen de su asistencia (SPEC 09).") AsistenciaMiembroResponse asistencia,
		@Schema(description = "Iteraciones anteriores, de la más reciente a la más antigua.") List<IteracionResponse> anteriores,
		@Schema(description = "Si el usuario del token puede registrar contactos y cerrar o reabrir el caso.", example = "true") boolean puedeActuar,
		@Schema(description = "Si el usuario del token puede reasignarlo (coordinador).", example = "false") boolean puedeReasignar) {
}
