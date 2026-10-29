package com.iglesia.api.miembros.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.miembros.model.CondicionMiembro;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;

@Schema(description = "Miembro en el listado.")
public record MiembroResumenResponse(
		@Schema(description = "Identificador del miembro.", example = "25") Long id,
		@Schema(description = "Nombres.", example = "Juan Carlos") String nombres,
		@Schema(description = "Apellidos.", example = "Pérez Torres") String apellidos,
		@Schema(description = "Condición.", example = "NUEVO", nullable = true) CondicionMiembro condicion,
		@Schema(description = "Nivel asignado.", nullable = true) ReferenciaResponse nivel,
		@Schema(description = "Líder directo.", nullable = true) ReferenciaResponse lider,
		@Schema(description = "Dirección (calle, número, referencia).", example = "Av. Los Olivos 123",
				nullable = true) String direccion,
		@Schema(description = "Departamento, provincia y distrito.", nullable = true) UbicacionResponse ubicacion,
		@Schema(description = "Teléfono.", example = "+51 987 654 321", nullable = true) String telefono,
		@Schema(description = "Si es el pastor principal de la iglesia.", example = "false") boolean esPastorPrincipal,
		@Schema(description = "Si está activo.", example = "true") boolean activo) {
}
