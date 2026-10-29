package com.iglesia.api.aportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categoría (concepto) configurable de los aportes de la iglesia.")
public record CategoriaAporteResponse(
		@Schema(description = "Identificador de la categoría.", example = "1") Long id,
		@Schema(description = "Nombre de la categoría.", example = "Diezmo") String nombre,
		@Schema(description = "`true` si sus aportes deben identificar al aportante; `false` si admite anónimos.",
				example = "true") boolean exigeAportante,
		@Schema(description = "Posición en la lista: 1 es la primera.", example = "1") int orden,
		@Schema(description = "`false` si está desactivada: no se ofrece para aportes nuevos, pero sus aportes "
				+ "siguen contando.", example = "true") boolean activo) {
}
