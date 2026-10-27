package com.iglesia.seguridad.rol.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.seguridad.rol.constant.LimitesRol;

@Schema(description = "Datos para crear un rol personalizado en la iglesia de quien lo crea.")
public record CrearRolRequest(
		@Schema(description = "Nombre del rol; único en la iglesia sin distinguir mayúsculas.",
				example = "Secretaría") @NotBlank @Size(max = LimitesRol.NOMBRE_MAX) String nombre,
		@Schema(description = "Id del rol base del que hereda ámbito y página de inicio: `ADMIN_IGLESIA`, `LIDER` o "
				+ "`MIEMBRO`.", example = "2") @NotNull @Positive Long rolBaseId,
		@Schema(description = "Códigos de los permisos; deben ser un subconjunto de los del rol base.",
				example = "[\"miembros.ver\", \"miembros.crear\"]") @NotNull Set<@NotBlank String> permisos) {
}
