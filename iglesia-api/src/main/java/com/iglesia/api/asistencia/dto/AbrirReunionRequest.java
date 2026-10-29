package com.iglesia.api.asistencia.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

@Schema(description = "Culto o grupo y fecha de la reunión que se abre para tomar asistencia. Se indica uno de los "
		+ "dos: `cultoProgramadoId` o `grupoId`.")
public record AbrirReunionRequest(
		@Schema(description = "Horario de culto de la reunión.", example = "3") Long cultoProgramadoId,
		@Schema(description = "Grupo de la reunión.", example = "8") Long grupoId,
		@Schema(description = "Fecha de la reunión. No puede ser futura; puede no coincidir con el día programado.",
				example = "2026-11-01") @NotNull @PastOrPresent LocalDate fecha) {

	@AssertTrue(message = "Indique el culto o el grupo, uno de los dos")
	@Schema(hidden = true)
	public boolean isOrigenUnico() {
		return (cultoProgramadoId == null) != (grupoId == null);
	}

}
