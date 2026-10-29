package com.iglesia.api.aportes.constant;

import java.util.List;

/** Monedas (ISO 4217) que una iglesia puede elegir para sus aportes, en el orden en que se ofrecen. */
public final class MonedasAdmitidas {

	public static final String POR_DEFECTO = "PEN";

	public static final List<String> CODIGOS = List.of("PEN", "USD", "EUR", "MXN", "COP", "CLP", "ARS", "BOB", "BRL",
			"UYU", "PYG", "GTQ", "HNL", "NIO", "CRC", "PAB", "DOP", "VES");

	private MonedasAdmitidas() {
	}

}
