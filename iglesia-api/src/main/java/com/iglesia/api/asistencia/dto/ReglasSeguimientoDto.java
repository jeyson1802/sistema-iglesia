package com.iglesia.api.asistencia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.iglesia.api.asistencia.constant.LimitesSeguimiento;
import com.iglesia.api.asistencia.model.ReglasSeguimiento;

@Schema(description = "Reglas de las alertas de seguimiento de asistencia de la iglesia. Sin configurar valen 3, 30 y 90.")
public record ReglasSeguimientoDto(
		@Schema(description = "Ocasiones seguidas sin asistir para la alerta «Faltas seguidas».", example = "3") @Min(
				LimitesSeguimiento.FALTAS_SEGUIDAS_MIN) @Max(LimitesSeguimiento.FALTAS_SEGUIDAS_MAX) int faltasSeguidas,
		@Schema(description = "Puntos que debe bajar el porcentaje de las últimas 4 semanas frente a las 8 anteriores "
				+ "para la alerta «Asistencia en caída».", example = "30") @Min(LimitesSeguimiento.PUNTOS_CAIDA_MIN) @Max(
						LimitesSeguimiento.PUNTOS_CAIDA_MAX) int puntosCaida,
		@Schema(description = "Días desde el ingreso en que alguien todavía es nuevo, para la alerta «Nuevo sin volver».",
				example = "90") @Min(LimitesSeguimiento.DIAS_NUEVO_MIN) @Max(LimitesSeguimiento.DIAS_NUEVO_MAX) int diasNuevo) {

	public static ReglasSeguimientoDto de(ReglasSeguimiento reglas) {
		return new ReglasSeguimientoDto(reglas.faltasSeguidas(), reglas.puntosCaida(), reglas.diasNuevo());
	}

	public ReglasSeguimiento aModelo() {
		return new ReglasSeguimiento(faltasSeguidas, puntosCaida, diasNuevo);
	}

}
