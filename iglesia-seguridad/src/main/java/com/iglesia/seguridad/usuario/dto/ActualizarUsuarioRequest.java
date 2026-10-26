package com.iglesia.seguridad.usuario.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.usuario.model.Rol;

/** La iglesia de un usuario no cambia; el correo tampoco se edita por esta vía. */
@Schema(description = "Cambios de un usuario. La iglesia y el correo no se modifican por esta vía.")
public record ActualizarUsuarioRequest(
		@Schema(description = "Roles de acceso del usuario. Solo `ADMIN_SISTEMA` puede asignar `ADMIN_SISTEMA`.",
				example = "[\"LIDER\"]") @NotEmpty Set<@NotNull Rol> roles,
		@Schema(description = "Id del miembro de iglesia-api vinculado. Obligatorio con los roles `LIDER` y `MIEMBRO`; "
				+ "un miembro solo puede tener un usuario.", example = "25") @Positive Long miembroId,
		@Schema(description = "`false` desactiva el usuario: no puede iniciar sesión y se cierran sus sesiones.",
				example = "true") @NotNull Boolean activo) {
}
