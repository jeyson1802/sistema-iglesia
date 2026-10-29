package com.iglesia.api.iglesia.controller;

import java.util.Arrays;
import java.util.Base64;

/** Archivos de prueba para el logo: un PNG real de 1×1, un SVG y PNG falsos de cualquier tamaño. */
final class ImagenesPrueba {

	static final byte[] PNG = Base64.getDecoder()
		.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

	static final byte[] SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes();

	private ImagenesPrueba() {
	}

	/** Empieza con la firma PNG y se rellena hasta el tamaño pedido. */
	static byte[] pngDe(int tamano) {
		byte[] contenido = Arrays.copyOf(PNG, tamano);
		Arrays.fill(contenido, PNG.length, tamano, (byte) 1);
		return contenido;
	}

}
