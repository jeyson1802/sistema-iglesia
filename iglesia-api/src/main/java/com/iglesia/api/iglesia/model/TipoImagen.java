package com.iglesia.api.iglesia.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos admitidos para el logo, reconocidos por los primeros bytes del archivo (su firma) y no por la cabecera que
 * envía el cliente, que se puede falsear. SVG no se admite porque puede contener código.
 */
public enum TipoImagen {

	PNG("image/png") {
		@Override
		boolean reconoce(byte[] contenido) {
			return empiezaCon(contenido, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
		}
	},
	JPEG("image/jpeg") {
		@Override
		boolean reconoce(byte[] contenido) {
			return empiezaCon(contenido, 0, 0xFF, 0xD8, 0xFF);
		}
	},
	/** Contenedor RIFF con la marca «WEBP» en el byte 8. */
	WEBP("image/webp") {
		@Override
		boolean reconoce(byte[] contenido) {
			return empiezaCon(contenido, 0, 'R', 'I', 'F', 'F') && empiezaCon(contenido, 8, 'W', 'E', 'B', 'P');
		}
	};

	private final String tipoMime;

	TipoImagen(String tipoMime) {
		this.tipoMime = tipoMime;
	}

	public String tipoMime() {
		return tipoMime;
	}

	abstract boolean reconoce(byte[] contenido);

	public static Optional<TipoImagen> detectar(byte[] contenido) {
		return Arrays.stream(values()).filter(tipo -> tipo.reconoce(contenido)).findFirst();
	}

	private static boolean empiezaCon(byte[] contenido, int desde, int... firma) {
		if (contenido.length < desde + firma.length) {
			return false;
		}
		for (int i = 0; i < firma.length; i++) {
			if ((contenido[desde + i] & 0xFF) != firma[i]) {
				return false;
			}
		}
		return true;
	}

}
