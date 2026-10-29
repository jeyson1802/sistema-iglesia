package com.iglesia.api.aportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.iglesia.api.aportes.constant.LimitesAporte;
import com.iglesia.api.aportes.model.MedioAporte;

@Schema(description = "Datos para registrar o corregir un aporte.")
public record GuardarAporteRequest(
		@Schema(description = "Fecha en que se entregó el aporte. No puede ser futura.", example = "2026-10-26",
				format = "date") @NotNull @PastOrPresent LocalDate fecha,
		@Schema(description = "Miembro que aporta. Sin valor, el aporte es anónimo (solo en categorías que lo "
				+ "admiten). Debe estar activo al asignarlo.", example = "12") Long miembroId,
		@Schema(description = "Categoría del aporte. Debe estar activa al asignarla.", example = "1") @NotNull Long categoriaId,
		@Schema(description = "Monto en la moneda de la iglesia, mayor que 0 y con hasta 2 decimales.",
				example = "150.50") @NotNull @Positive @Digits(integer = LimitesAporte.MONTO_ENTEROS,
						fraction = LimitesAporte.MONTO_DECIMALES) BigDecimal monto,
		@Schema(description = "Medio con el que se entregó.", example = "YAPE") @NotNull MedioAporte medio,
		@Schema(description = "Número o código de la operación (transferencia, Yape, Plin, cheque). Con `EFECTIVO` "
				+ "se ignora.", example = "00123456") @Size(max = LimitesAporte.NUMERO_OPERACION_MAX) String numeroOperacion,
		@Schema(description = "Petición o motivo de oración que acompaña el aporte.",
				example = "Por la salud de mi madre") @Size(max = LimitesAporte.PETICION_MAX) String peticion,
		@Schema(description = "Nota interna de quien registra.", example = "Sobre entregado en el culto de jóvenes")
		@Size(max = LimitesAporte.OBSERVACION_MAX) String observacion) {
}
