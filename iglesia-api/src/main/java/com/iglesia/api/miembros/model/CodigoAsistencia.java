package com.iglesia.api.miembros.model;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Código del QR personal de asistencia: 12 caracteres en base32 de Crockford (sin {@code I}, {@code L}, {@code O} ni
 * {@code U}, para que no se confundan al leerlos o escribirlos). El QR lleva el prefijo {@value #PREFIJO} para
 * distinguirlo de cualquier otro QR. No revela el id, el nombre ni el documento del miembro.
 */
public record CodigoAsistencia(String valor) {

	public static final String PREFIJO = "ASIS:";

	public static final String ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";

	public static final int LONGITUD = 12;

	private static final Pattern FORMATO = Pattern.compile("[" + ALFABETO + "]{" + LONGITUD + "}");

	/** Separadores que se aceptan al escribirlo a mano (se imprime en grupos de cuatro: {@code 7K2Q M9XD R4TB}). */
	private static final Pattern SEPARADORES = Pattern.compile("[\\s-]");

	public CodigoAsistencia {
		if (valor == null || !FORMATO.matcher(valor).matches()) {
			throw new IllegalArgumentException("Código de asistencia con formato inválido");
		}
	}

	/**
	 * Lee lo que entrega la cámara o el lector: con o sin prefijo, en mayúsculas o minúsculas y con espacios o guiones.
	 * Si no tiene el formato de un código de asistencia (p. ej. el QR de un enlace o de un pago) no devuelve nada.
	 */
	public static Optional<CodigoAsistencia> leer(String texto) {
		if (texto == null) {
			return Optional.empty();
		}
		String limpio = texto.strip().toUpperCase(Locale.ROOT);
		if (limpio.startsWith(PREFIJO)) {
			limpio = limpio.substring(PREFIJO.length());
		}
		limpio = SEPARADORES.matcher(limpio).replaceAll("");
		return FORMATO.matcher(limpio).matches() ? Optional.of(new CodigoAsistencia(limpio)) : Optional.empty();
	}

	/** Texto que se codifica en el QR. */
	public String contenidoQr() {
		return PREFIJO + valor;
	}

}
