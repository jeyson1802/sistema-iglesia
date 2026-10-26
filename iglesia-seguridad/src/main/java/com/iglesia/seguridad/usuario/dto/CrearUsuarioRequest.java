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
import com.iglesia.seguridad.usuario.model.Rol;

@Schema(description = "Datos para crear un usuario.")
public record CrearUsuarioRequest(
		@Schema(description = "Correo con el que iniciará sesión; único en todo el sistema.",
				example = "lider@iglesia.org") @NotBlank @Email @Size(max = LimitesUsuario.CORREO_MAX) String correo,
		@Schema(description = "Contraseña inicial.", example = "ClaveSegura123", format = "password") @NotBlank @Size(
				min = LimitesUsuario.CONTRASENA_MIN, max = LimitesUsuario.CONTRASENA_MAX) String contrasena,
		@Schema(description = "Iglesia del usuario. Obligatoria salvo con el rol `ADMIN_SISTEMA`, que no pertenece "
				+ "a ninguna. Un `ADMIN_IGLESIA` solo puede indicar la suya.", example = "1") @Positive Long iglesiaId,
		@Schema(description = "Id del miembro de iglesia-api vinculado. Obligatorio con los roles `LIDER` y `MIEMBRO`; "
				+ "un miembro solo puede tener un usuario.", example = "25") @Positive Long miembroId,
		@Schema(description = "Roles de acceso del usuario. Solo `ADMIN_SISTEMA` puede asignar `ADMIN_SISTEMA`.",
				example = "[\"LIDER\"]") @NotEmpty Set<@NotNull Rol> roles) {
}
