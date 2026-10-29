package com.iglesia.api.grupos.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.iglesia.api.grupos.constant.LimitesGrupo;
import com.iglesia.api.grupos.model.DiaSemana;

@Schema(description = "Datos para crear o editar un grupo. Solo el líder es obligatorio: el grupo no tiene nombre, "
		+ "es «el grupo de» su líder.")
public record GuardarGrupoRequest(
		@Schema(description = "Miembro que lo lidera: activo al asignarlo y sin otro grupo.",
				example = "12") @NotNull Long liderId,
		@Schema(description = "Miembro en cuya casa se reúne; activo al asignarlo.", example = "15",
				nullable = true) Long anfitrionId,
		@Schema(description = "Día de la reunión semanal.", example = "JUEVES", nullable = true) DiaSemana diaReunion,
		@Schema(description = "Hora de la reunión (HH:mm).", example = "19:30", type = "string", format = "time",
				nullable = true) LocalTime horaReunion,
		@Schema(description = "Dirección de la reunión.", example = "Jr. Las Flores 456", nullable = true) @Size(
				max = LimitesGrupo.DIRECCION_MAX) String direccion,
		@Schema(description = "Código INEI del distrito de la reunión.", example = "150135", nullable = true) @Pattern(
				regexp = LimitesGrupo.DISTRITO_PATRON, message = "debe tener 6 dígitos") String distritoCodigo,
		@Schema(description = "Descripción libre del grupo.", example = "Grupo de matrimonios jóvenes",
				nullable = true) @Size(max = LimitesGrupo.DESCRIPCION_MAX) String descripcion) {
}
