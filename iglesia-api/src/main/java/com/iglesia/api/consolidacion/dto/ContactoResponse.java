package com.iglesia.api.consolidacion.dto;

import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.consolidacion.model.MedioContacto;
import com.iglesia.api.consolidacion.model.ResultadoContacto;

@Schema(description = "Contacto registrado.")
public record ContactoResponse(
		@Schema(description = "Identificador.", example = "900") Long id,
		@Schema(description = "Caso.", example = "310") Long casoId,
		@Schema(description = "Cuándo fue.", example = "2026-11-03T19:30:00Z") Instant fechaHora,
		@Schema(description = "Medio.", example = "LLAMADA") MedioContacto medio,
		@Schema(description = "Qué pasó.", example = "SE_HABLO") ResultadoContacto resultado,
		@Schema(description = "Observaciones.", example = "Está bien, pide oración por su trabajo") String observaciones,
		@Schema(description = "Próximo contacto previsto.", example = "2026-11-10") LocalDate proximoContacto,
		@Schema(description = "Quién hizo el contacto.") ReferenciaResponse consolidador,
		@Schema(description = "Si lo registró el usuario del token.", example = "true") boolean registradoPorMi,
		@Schema(description = "Hasta cuándo puede corregirlo o borrarlo quien lo registró.", example = "2026-11-04T19:31:00Z") Instant editableHasta,
		@Schema(description = "Si el usuario del token puede corregirlo o borrarlo ahora.", example = "true") boolean editable) {
}
