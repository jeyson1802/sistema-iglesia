package com.iglesia.api.miembros.dto;

import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.miembros.model.CondicionMiembro;
import com.iglesia.api.miembros.model.EstadoCivil;
import com.iglesia.api.miembros.model.Genero;
import com.iglesia.api.miembros.model.NivelEstudios;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;

@Schema(description = "Ficha completa del miembro.")
public record MiembroResponse(
		@Schema(description = "Identificador del miembro.", example = "25") Long id,
		@Schema(description = "Nombres.", example = "Juan Carlos") String nombres,
		@Schema(description = "Apellidos.", example = "Pérez Torres") String apellidos,
		@Schema(description = "Género.", example = "MASCULINO", nullable = true) Genero genero,
		@Schema(description = "Fecha de nacimiento.", example = "1990-05-14",
				nullable = true) LocalDate fechaNacimiento,
		@Schema(description = "Edad en años: exacta si hay fecha de nacimiento; si no, la aproximada; o nula.",
				example = "35", nullable = true) Integer edad,
		@Schema(description = "`true` si `edad` sale de la edad aproximada (no hay fecha de nacimiento).",
				example = "false") boolean edadEsAproximada,
		@Schema(description = "Edad aproximada que se registró (año actual menos el año estimado), independiente de "
				+ "la fecha de nacimiento.", example = "35", nullable = true) Integer edadAproximada,
		@Schema(description = "Fecha de ingreso a la iglesia.", example = "2024-01-07",
				nullable = true) LocalDate fechaIngreso,
		@Schema(description = "Condición.", example = "NUEVO", nullable = true) CondicionMiembro condicion,
		@Schema(description = "Estado civil.", example = "CASADO", nullable = true) EstadoCivil estadoCivil,
		@Schema(description = "Teléfono.", example = "+51 987 654 321", nullable = true) String telefono,
		@Schema(description = "Correo electrónico.", example = "juan.perez@correo.com", nullable = true) String correo,
		@Schema(description = "Dirección.", example = "Av. Los Olivos 123", nullable = true) String direccion,
		@Schema(description = "Código INEI del distrito.", example = "150101", nullable = true) String distritoCodigo,
		@Schema(description = "Departamento, provincia y distrito.", nullable = true) UbicacionResponse ubicacion,
		@Schema(description = "Ocupación.", example = "Comerciante", nullable = true) String ocupacion,
		@Schema(description = "Profesión.", example = "Contador", nullable = true) String profesion,
		@Schema(description = "Nivel de estudios.", example = "UNIVERSITARIO",
				nullable = true) NivelEstudios nivelEstudios,
		@Schema(description = "Centro de estudios.", example = "Universidad Nacional de Ingeniería",
				nullable = true) String centroEstudios,
		@Schema(description = "Petición o necesidad.", example = "Oración por su familia",
				nullable = true) String peticion,
		@Schema(description = "Nivel asignado.", nullable = true) ReferenciaResponse nivel,
		@Schema(description = "Líder directo.", nullable = true) ReferenciaResponse lider,
		@Schema(description = "Grupo al que pertenece.", nullable = true) ReferenciaResponse grupo,
		@Schema(description = "Quién lo invitó.", nullable = true) ReferenciaResponse invitadoPor,
		@Schema(description = "Quién lo registró.", nullable = true) ReferenciaResponse registradoPor,
		@Schema(description = "Cantidad de miembros activos que lidera directamente.",
				example = "4") long cantidadLiderados,
		@Schema(description = "Si es el pastor principal de la iglesia.", example = "false") boolean esPastorPrincipal,
		@Schema(description = "Si está activo.", example = "true") boolean activo,
		@Schema(description = "Instante del registro.", example = "2026-10-28T15:30:00Z",
				format = "date-time") Instant creadoEn,
		@Schema(description = "Instante del último cambio.", example = "2026-10-28T15:30:00Z",
				format = "date-time") Instant actualizadoEn) {
}
