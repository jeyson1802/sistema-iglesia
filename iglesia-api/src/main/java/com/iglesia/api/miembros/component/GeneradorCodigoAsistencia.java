package com.iglesia.api.miembros.component;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

import com.iglesia.api.miembros.model.CodigoAsistencia;

/** Genera códigos de asistencia aleatorios e impredecibles (60 bits de {@link SecureRandom}). */
@Component
public class GeneradorCodigoAsistencia {

	private final SecureRandom azar = new SecureRandom();

	public CodigoAsistencia generar() {
		StringBuilder codigo = new StringBuilder(CodigoAsistencia.LONGITUD);
		for (int i = 0; i < CodigoAsistencia.LONGITUD; i++) {
			codigo.append(CodigoAsistencia.ALFABETO.charAt(azar.nextInt(CodigoAsistencia.ALFABETO.length())));
		}
		return new CodigoAsistencia(codigo.toString());
	}

}
