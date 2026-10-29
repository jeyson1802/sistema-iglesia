package com.iglesia.api.aportes.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.aportes.model.MedioAporte;
import com.iglesia.api.common.api.dto.ReferenciaResponse;

@Schema(description = "Aporte registrado.")
public record AporteResponse(
		@Schema(description = "Identificador del aporte.", example = "45") Long id,
		@Schema(description = "Fecha del aporte.", example = "2026-10-26", format = "date") LocalDate fecha,
		@Schema(description = "Miembro aportante; `null` si es anónimo.") ReferenciaResponse miembro,
		@Schema(description = "Categoría del aporte.") ReferenciaResponse categoria,
		@Schema(description = "Monto en la moneda de la iglesia.", example = "150.50") BigDecimal monto,
		@Schema(description = "Medio con el que se entregó.", example = "YAPE") MedioAporte medio,
		@Schema(description = "Número de operación.", example = "00123456") String numeroOperacion,
		@Schema(description = "Petición que acompaña el aporte.", example = "Por la salud de mi madre") String peticion,
		@Schema(description = "Nota interna.", example = "Sobre del culto de jóvenes") String observacion,
		@Schema(description = "`true` si se anuló: no cuenta en los totales.", example = "false") boolean anulado,
		@Schema(description = "Motivo de la anulación.", example = "Registrado dos veces") String motivoAnulacion,
		@Schema(description = "Cuándo se registró.", example = "2026-10-26T15:30:00Z") Instant registradoEn,
		@Schema(description = "Cuándo se modificó o anuló por última vez; `null` si nunca.",
				example = "2026-10-27T09:10:00Z") Instant modificadoEn) {
}
