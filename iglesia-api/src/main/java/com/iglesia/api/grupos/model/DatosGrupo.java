package com.iglesia.api.grupos.model;

import java.time.LocalTime;

/** Datos de un grupo ya normalizados y validados, listos para aplicarlos a la entidad. */
public record DatosGrupo(Long liderId, Long anfitrionId, DiaSemana diaReunion, LocalTime horaReunion,
		String direccion, String distritoCodigo, String descripcion) {
}
