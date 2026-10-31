package com.iglesia.api.perfil.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.miembros.model.CondicionMiembro;

@Schema(description = """
		Datos del propio miembro para su página de inicio: lo justo para saber quién es en la iglesia. Nunca incluye \
		aportes, consolidación ni datos de otras personas (salvo el nombre de su líder).""")
public record MiPerfilResponse(
		@Schema(description = "Identificador del miembro.", example = "40") Long miembroId,
		@Schema(description = "Nombres.", example = "Ana Lucía") String nombres,
		@Schema(description = "Apellidos.", example = "Quispe Rojas") String apellidos,
		@Schema(description = "Edad cumplida (calculada, nunca guardada); nula si no se conoce.", example = "29",
				nullable = true) Integer edad,
		@Schema(description = "`true` si la edad sale de un año de nacimiento estimado.",
				example = "false") boolean edadEsAproximada,
		@Schema(description = "Fecha en que llegó a la iglesia.", example = "2025-03-09", nullable = true) LocalDate fechaIngreso,
		@Schema(description = "Situación en la iglesia.", example = "NUEVO", nullable = true) CondicionMiembro condicion,
		@Schema(description = "Nivel (rango) en la iglesia.", example = "Discípulo", nullable = true) String nivel,
		@Schema(description = "Nombre de su líder.", example = "Luis Rojas", nullable = true) String lider,
		@Schema(description = "Grupo al que pertenece.", example = "{\"id\": 22, \"nombre\": \"Célula de Ana Mendoza "
				+ "Silva\", \"diaReunion\": \"JUEVES\", \"horaReunion\": \"19:30:00\"}",
				nullable = true) GrupoPerfilResponse grupo) {
}
