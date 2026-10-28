package com.iglesia.seguridad.usuario.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.usuario.constant.LimitesUsuario;

/** La iglesia de un usuario no cambia. */
@Schema(description = "Cambios de un usuario. La iglesia no se modifica por esta vía.")
public record ActualizarUsuarioRequest(
		@Schema(description = "Correo con el que inicia sesión; único en todo el sistema. Si cambia, se cierran las "
				+ "sesiones del usuario y debe entrar con el nuevo.",
				example = "lider@iglesia.org") @NotBlank @Email @Size(max = LimitesUsuario.CORREO_MAX) String correo,
		@Schema(description = "Ids de los roles de acceso: roles base o roles personalizados activos de la iglesia "
				+ "del usuario. Solo `ADMIN_SISTEMA` puede asignar el rol base `ADMIN_SISTEMA`.",
				example = "[3, 7]") @NotEmpty Set<@NotNull @Positive Long> rolIds,
		@Schema(description = "Id del miembro de iglesia-api vinculado. Obligatorio con los roles `LIDER` y `MIEMBRO` "
				+ "y con los que heredan de ellos; "
				+ "un miembro solo puede tener un usuario.", example = "25") @Positive Long miembroId,
		@Schema(description = "`false` desactiva el usuario: no puede iniciar sesión y se cierran sus sesiones.",
				example = "true") @NotNull Boolean activo) {
}
