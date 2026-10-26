package com.iglesia.seguridad.usuario.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import io.swagger.v3.oas.annotations.media.Schema;

/** La iglesia de un usuario no cambia; el correo tampoco se edita por esta vía. */
@Schema(description = "Cambios de un usuario. La iglesia y el correo no se modifican por esta vía.")
public record ActualizarUsuarioRequest(
		@Schema(description = "Ids de los roles de acceso: roles base o roles personalizados activos de la iglesia "
				+ "del usuario. Solo `ADMIN_SISTEMA` puede asignar el rol base `ADMIN_SISTEMA`.",
				example = "[3, 7]") @NotEmpty Set<@NotNull @Positive Long> rolIds,
		@Schema(description = "Id del miembro de iglesia-api vinculado. Obligatorio con los roles `LIDER` y `MIEMBRO` "
				+ "y con los que heredan de ellos; "
				+ "un miembro solo puede tener un usuario.", example = "25") @Positive Long miembroId,
		@Schema(description = "`false` desactiva el usuario: no puede iniciar sesión y se cierran sus sesiones.",
				example = "true") @NotNull Boolean activo) {
}
