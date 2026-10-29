package com.iglesia.api.iglesia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.iglesia.api.iglesia.constant.LimitesIglesia;

@Schema(description = "Datos para dar de alta una iglesia o cambiarle el nombre.")
public record GuardarIglesiaRequest(
		@Schema(description = "Nombre de la iglesia. Es único sin distinguir mayúsculas y se guarda sin espacios al "
				+ "inicio ni al final.", example = "Iglesia Central") @NotBlank @Size(
						max = LimitesIglesia.NOMBRE_MAX) String nombre) {
}
