package com.iglesia.api.consolidacion.dto;

import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.iglesia.api.consolidacion.constant.LimitesConsolidacion;
import com.iglesia.api.consolidacion.model.MedioContacto;
import com.iglesia.api.consolidacion.model.ResultadoContacto;

@Schema(description = "Una llamada, mensaje o visita.")
public record RegistrarContactoRequest(
		@Schema(description = "Cuándo se hizo el contacto. No puede ser futuro (se toleran unos minutos de diferencia de reloj).", example = "2026-11-03T19:30:00Z") @NotNull Instant fechaHora,
		@Schema(description = "Medio del contacto.", example = "LLAMADA") @NotNull MedioContacto medio,
		@Schema(description = "Qué pasó; debe tener sentido para el medio (p. ej. `NO_ESTABA` solo en visitas).", example = "SE_HABLO") @NotNull ResultadoContacto resultado,
		@Schema(description = "Observaciones. Nunca se registran en los logs.", example = "Está bien, pide oración por su trabajo") @Size(max = LimitesConsolidacion.OBSERVACIONES_MAX) String observaciones,
		@Schema(description = "Fecha del próximo contacto, igual o posterior a la del contacto.", example = "2026-11-10") LocalDate proximoContacto,
		@Schema(description = "Quién hizo el contacto; por defecto, el consolidador del caso. Solo un coordinador puede indicar a otro.", example = "25") Long consolidadorId) {
}
