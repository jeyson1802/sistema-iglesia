package com.iglesia.api.common.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Referencia a otro registro (miembro, nivel o grupo) con su nombre para mostrar. Si el registro "
		+ "se desactivó después de asignarlo, se conserva y se marca con `activo: false`.")
public record ReferenciaResponse(
		@Schema(description = "Identificador del registro referenciado.", example = "12") Long id,
		@Schema(description = "Nombre para mostrar (nombres y apellidos en el caso de un miembro).",
				example = "Ana Torres") String nombre,
		@Schema(description = "Si el registro referenciado sigue activo.", example = "true") boolean activo) {
}
