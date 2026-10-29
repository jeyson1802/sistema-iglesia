package com.iglesia.api.asistencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.iglesia.api.asistencia.constant.LimitesAsistencia;

@Schema(description = "Texto leído del QR por la cámara o el lector, o escrito a mano.")
public record MarcarCodigoRequest(
		@Schema(description = "Código de asistencia, con o sin el prefijo `ASIS:`, en mayúsculas o minúsculas y con o "
				+ "sin espacios o guiones.", example = "ASIS:7K2QM9XDR4TB")
		@NotBlank @Size(max = LimitesAsistencia.CODIGO_LEIDO_MAX) String codigo) {
}
