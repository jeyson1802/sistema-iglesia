package com.iglesia.api.grupos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cifras de un grupo en el periodo.")
public record GrupoEstadisticaResponse(
		@Schema(description = "Identificador del grupo.", example = "22") Long grupoId,
		@Schema(description = "Nombre del grupo (tipo de grupo de la iglesia y su líder).",
				example = "Célula de Ana Mendoza Silva") String nombre,
		@Schema(description = "Integrantes activos a hoy.", example = "12") long integrantes,
		@Schema(description = "Reuniones registradas en el periodo.", example = "4") long reuniones,
		@Schema(description = "Porcentaje de integrantes que asistieron a sus reuniones del periodo; nulo si no hubo "
				+ "reuniones con integrantes.", example = "75", nullable = true) Integer porcentaje) {
}
