package com.iglesia.api.grupos.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.grupos.model.DiaSemana;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;

@Schema(description = "Grupo de la iglesia. No tiene nombre: se muestra como «<tipo de grupo> de <líder>».")
public record GrupoResponse(
		@Schema(description = "Identificador del grupo.", example = "4") Long id,
		@Schema(description = "Miembro que lo lidera; identifica al grupo.") ReferenciaResponse lider,
		@Schema(description = "Miembro anfitrión.", nullable = true) ReferenciaResponse anfitrion,
		@Schema(description = "Día de la reunión.", example = "JUEVES", nullable = true) DiaSemana diaReunion,
		@Schema(description = "Hora de la reunión.", example = "19:30", type = "string", format = "time",
				nullable = true) LocalTime horaReunion,
		@Schema(description = "Dirección de la reunión.", example = "Jr. Las Flores 456",
				nullable = true) String direccion,
		@Schema(description = "Código INEI del distrito.", example = "150135", nullable = true) String distritoCodigo,
		@Schema(description = "Departamento, provincia y distrito.", nullable = true) UbicacionResponse ubicacion,
		@Schema(description = "Descripción del grupo.", example = "Grupo de matrimonios jóvenes",
				nullable = true) String descripcion,
		@Schema(description = "Si está activo.", example = "true") boolean activo,
		@Schema(description = "Cantidad de miembros activos que pertenecen al grupo.",
				example = "9") long cantidadMiembros) {
}
