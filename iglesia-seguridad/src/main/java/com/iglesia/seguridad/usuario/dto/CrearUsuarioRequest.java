package com.iglesia.seguridad.usuario.dto;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.iglesia.seguridad.usuario.constant.LimitesUsuario;
import com.iglesia.seguridad.usuario.model.Rol;

public record CrearUsuarioRequest(
		@NotBlank @Email @Size(max = LimitesUsuario.CORREO_MAX) String correo,
		@NotBlank @Size(min = LimitesUsuario.CONTRASENA_MIN, max = LimitesUsuario.CONTRASENA_MAX) String contrasena,
		@Positive Long iglesiaId,
		@Positive Long miembroId,
		@NotEmpty Set<@NotNull Rol> roles) {
}
