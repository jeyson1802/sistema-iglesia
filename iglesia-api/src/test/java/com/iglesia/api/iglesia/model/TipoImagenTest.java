package com.iglesia.api.iglesia.model;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipoImagenTest {

	@Test
	void reconoceCadaFormatoPorSuFirma() {
		assertThat(TipoImagen.detectar(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0))).contains(TipoImagen.PNG);
		assertThat(TipoImagen.detectar(bytes(0xFF, 0xD8, 0xFF, 0xE0))).contains(TipoImagen.JPEG);
		assertThat(TipoImagen.detectar(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V')))
			.contains(TipoImagen.WEBP);
		assertThat(TipoImagen.WEBP.tipoMime()).isEqualTo("image/webp");
	}

	@Test
	void noAdmiteSvgNiArchivosRenombradosNiTruncados() {
		assertThat(TipoImagen.detectar("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8)))
			.isEmpty();
		assertThat(TipoImagen.detectar("%PDF-1.7".getBytes(StandardCharsets.UTF_8))).isEmpty();
		assertThat(TipoImagen.detectar(bytes('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'))).isEmpty();
		assertThat(TipoImagen.detectar(bytes(0x89, 'P'))).isEmpty();
		assertThat(TipoImagen.detectar(new byte[0])).isEmpty();
	}

	private static byte[] bytes(int... valores) {
		byte[] resultado = new byte[valores.length];
		for (int i = 0; i < valores.length; i++) {
			resultado[i] = (byte) valores[i];
		}
		return resultado;
	}

}
