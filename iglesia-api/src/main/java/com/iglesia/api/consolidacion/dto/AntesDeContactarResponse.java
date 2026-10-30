package com.iglesia.api.consolidacion.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lo que conviene saber antes de contactar a la persona (la pantalla arma el texto).")
public record AntesDeContactarResponse(
		@Schema(description = "Número de esta iteración (1 es la primera).", example = "3") int iteracion,
		@Schema(description = "Contactos de las iteraciones anteriores.") ContactosPorMedioResponse contactosAnteriores,
		@Schema(description = "Resultados de las iteraciones anteriores, de la más reciente a la más antigua.") List<IteracionAnteriorResponse> resultadosAnteriores,
		@Schema(description = "Reuniones a las que asistió desde el último contacto de la iteración anterior.", example = "2") long asistenciasDesdeUltimoContacto,
		@Schema(description = "Petición o necesidad con la que llegó.", example = "Oración por su familia") String peticion) {
}
