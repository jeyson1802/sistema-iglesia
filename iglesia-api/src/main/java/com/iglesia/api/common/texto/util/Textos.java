package com.iglesia.api.common.texto.util;

import org.springframework.util.StringUtils;

/** Normalización de los textos que llegan del cliente antes de guardarlos o compararlos. */
public final class Textos {

	private static final String ESPACIOS = "\\s+";

	private static final char ESCAPE = '\\';

	/** Letras con tilde o diéresis y la «ñ», y su equivalente sin ellas (misma posición). */
	public static final String CON_TILDE = "áéíóúüñÁÉÍÓÚÜÑ";

	public static final String SIN_TILDE = "aeiouunAEIOUUN";

	private Textos() {
	}

	/** Quita los espacios del inicio y del final y deja un solo espacio entre palabras. */
	public static String normalizar(String texto) {
		return texto.strip().replaceAll(ESPACIOS, " ");
	}

	/** Como {@link #normalizar(String)}, pero un texto vacío o nulo queda en {@code null}. */
	public static String normalizarOpcional(String texto) {
		return StringUtils.hasText(texto) ? normalizar(texto) : null;
	}

	/** Quita solo los espacios del inicio y del final (conserva los saltos de línea); vacío o nulo queda en {@code null}. */
	public static String recortarOpcional(String texto) {
		return StringUtils.hasText(texto) ? texto.strip() : null;
	}

	/**
	 * Si dos textos son iguales sin distinguir mayúsculas (incluidas las vocales con tilde y la «ñ») ni espacios
	 * sobrantes. Se compara en Java porque {@code lower()} de PostgreSQL depende de la configuración regional del
	 * servidor y con {@code C} no pliega las letras con tilde.
	 */
	public static boolean mismoTexto(String uno, String otro) {
		return normalizar(uno).equalsIgnoreCase(normalizar(otro));
	}

	/** Quita tildes, diéresis y la tilde de la «ñ», igual que {@code translate(texto, CON_TILDE, SIN_TILDE)}. */
	public static String sinTildes(String texto) {
		StringBuilder resultado = new StringBuilder(texto.length());
		for (char letra : texto.toCharArray()) {
			int posicion = CON_TILDE.indexOf(letra);
			resultado.append(posicion < 0 ? letra : SIN_TILDE.charAt(posicion));
		}
		return resultado.toString();
	}

	/**
	 * Patrón {@code LIKE} «contiene», sin tildes y en minúsculas, con {@code %} y {@code _} buscados literalmente. Se
	 * compara contra la columna pasada por {@code lower(translate(columna, CON_TILDE, SIN_TILDE))}.
	 */
	public static String patronContiene(String texto) {
		String escapado = sinTildes(normalizar(texto)).toLowerCase()
			.replace(String.valueOf(ESCAPE), "" + ESCAPE + ESCAPE)
			.replace("%", ESCAPE + "%")
			.replace("_", ESCAPE + "_");
		return "%" + escapado + "%";
	}

	public static char escape() {
		return ESCAPE;
	}

}
