package com.iglesia.api.consolidacion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Seguimiento individual (sin ronda) para una persona.")
public record AbrirSeguimientoRequest(
		@Schema(description = "Persona a la que se da seguimiento.", example = "40") @NotNull Long miembroId,
		@Schema(description = "Quién la consolida; por defecto, el miembro de quien lo abre. Solo un coordinador puede elegir a otro.", example = "25") Long consolidadorId) {
}
