package com.iglesia.api.consolidacion.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import com.iglesia.api.asistencia.model.TipoAlerta;
import com.iglesia.api.miembros.model.CondicionMiembro;

@Schema(description = "Criterios para armar el lote de una ronda. Los criterios vacíos no restringen; con `miembroIds` solo entran esas personas.")
public record FiltroLoteRequest(
		@Schema(description = "Condición de la persona.", example = "NUEVO") CondicionMiembro condicion,
		@Schema(description = "Fecha de ingreso desde (o de registro, si no la tiene).", example = "2026-08-01") LocalDate ingresoDesde,
		@Schema(description = "Fecha de ingreso hasta.", example = "2026-10-31") LocalDate ingresoHasta,
		@Schema(description = "Solo personas con esta alerta de asistencia (SPEC 09).", example = "NUEVO_SIN_VOLVER") TipoAlerta alerta,
		@Schema(description = "Solo integrantes de este grupo.", example = "4") Long grupoId,
		@Schema(description = "Solo personas sin grupo.", example = "false") Boolean sinGrupo,
		@Schema(description = "Solo personas que nunca estuvieron en un seguimiento de consolidación.", example = "true") Boolean nuncaConsolidados,
		@Schema(description = "Personas elegidas a mano.", example = "[12, 15]") List<@NotNull Long> miembroIds) {

	public boolean soloSinGrupo() {
		return Boolean.TRUE.equals(sinGrupo);
	}

	public boolean soloNuncaConsolidados() {
		return Boolean.TRUE.equals(nuncaConsolidados);
	}

}
