package com.iglesia.api.miembros.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.miembros.model.CondicionMiembro;

@Schema(description = "Miembro en el listado.")
public record MiembroResumenResponse(
		@Schema(description = "Identificador del miembro.", example = "25") Long id,
		@Schema(description = "Nombres.", example = "Juan Carlos") String nombres,
		@Schema(description = "Apellidos.", example = "Pérez Torres") String apellidos,
		@Schema(description = "Condición.", example = "NUEVO", nullable = true) CondicionMiembro condicion,
		@Schema(description = "Nivel asignado.", nullable = true) ReferenciaResponse nivel,
		@Schema(description = "Líder directo.", nullable = true) ReferenciaResponse lider,
		@Schema(description = "Grupo al que pertenece.", nullable = true) ReferenciaResponse grupo,
		@Schema(description = "Teléfono.", example = "+51 987 654 321", nullable = true) String telefono,
		@Schema(description = "Si es el pastor principal de la iglesia.", example = "false") boolean esPastorPrincipal,
		@Schema(description = "Si está activo.", example = "true") boolean activo) {
}
