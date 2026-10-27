package com.iglesia.seguridad.rol.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Permiso del catálogo fijo: una acción concreta que se puede otorgar a un rol.")
public record PermisoResponse(
		@Schema(description = "Código con la forma `<modulo>.<accion>`.", example = "miembros.crear") String codigo,
		@Schema(description = "Módulo al que pertenece, para agruparlo en la interfaz.",
				example = "Miembros") String modulo,
		@Schema(description = "Qué permite hacer.", example = "Registrar miembros") String descripcion) {
}
