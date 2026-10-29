package com.iglesia.api.aportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.iglesia.api.aportes.constant.LimitesAporte;

@Schema(description = "Datos para crear o editar una categoría de aporte.")
public record GuardarCategoriaAporteRequest(
		@Schema(description = "Nombre de la categoría. Único en la iglesia sin distinguir mayúsculas; se guarda sin "
				+ "espacios sobrantes.", example = "Misiones") @NotBlank @Size(max = LimitesAporte.NOMBRE_CATEGORIA_MAX) String nombre,
		@Schema(description = "Si sus aportes deben identificar al aportante (no admite anónimos).",
				example = "false") @NotNull Boolean exigeAportante) {
}
