package com.iglesia.api.consolidacion.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.miembros.model.CondicionMiembro;

@Schema(description = "Datos para contactar a la persona.")
public record ContactoPersonaResponse(
		@Schema(description = "Miembro.", example = "40") Long miembroId,
		@Schema(description = "Nombres y apellidos.", example = "Ana Quispe") String nombreCompleto,
		@Schema(description = "Teléfono.", example = "987654321") String telefono,
		@Schema(description = "Correo.", example = "ana@correo.com") String correo,
		@Schema(description = "Dirección.", example = "Av. Los Olivos 123") String direccion,
		@Schema(description = "Distrito y provincia.", example = "LOS OLIVOS, LIMA") String ubicacion,
		@Schema(description = "Condición.", example = "NUEVO") CondicionMiembro condicion,
		@Schema(description = "Fecha de ingreso (o de registro).", example = "2026-10-05") LocalDate fechaIngreso,
		@Schema(description = "Fecha de nacimiento.", example = "1995-04-12") LocalDate fechaNacimiento,
		@Schema(description = "Quién la invitó.", example = "Rosa Díaz") String invitadoPor,
		@Schema(description = "Su líder.", example = "Juan Pérez") String lider,
		@Schema(description = "Su grupo.", example = "Célula de Juan Pérez") String grupo,
		@Schema(description = "Petición con la que llegó.", example = "Oración por su familia") String peticion,
		@Schema(description = "Si sigue activa.", example = "true") boolean activo) {
}
