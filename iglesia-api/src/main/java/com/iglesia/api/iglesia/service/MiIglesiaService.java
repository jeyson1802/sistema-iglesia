package com.iglesia.api.iglesia.service;

import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.iglesia.dto.AparienciaResponse;
import com.iglesia.api.iglesia.dto.GuardarAparienciaRequest;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.model.LogoIglesia;

/**
 * Identidad de la iglesia en la que se opera. La iglesia sale siempre del token ({@link IglesiaActual}), nunca de un
 * parámetro del cliente: así un administrador de iglesia solo puede leer y personalizar la suya. Sin iglesia en el
 * token responde 400, como el resto de las operaciones de una iglesia.
 */
@Service
public class MiIglesiaService {

	private final IglesiaActual iglesiaActual;

	private final IglesiaService catalogo;

	private final LogoIglesiaService logos;

	MiIglesiaService(IglesiaActual iglesiaActual, IglesiaService catalogo, LogoIglesiaService logos) {
		this.iglesiaActual = iglesiaActual;
		this.catalogo = catalogo;
		this.logos = logos;
	}

	public IglesiaResponse obtener() {
		return catalogo.obtener(iglesiaActual.requerida());
	}

	public IglesiaResponse actualizar(GuardarIglesiaRequest solicitud) {
		return catalogo.actualizar(iglesiaActual.requerida(), solicitud);
	}

	public AparienciaResponse cambiarApariencia(GuardarAparienciaRequest solicitud) {
		return catalogo.cambiarApariencia(iglesiaActual.requerida(), solicitud);
	}

	public LogoIglesia logo() {
		return logos.obtener(iglesiaActual.requerida());
	}

	public void guardarLogo(InputStream contenido) {
		logos.guardar(iglesiaActual.requerida(), contenido);
	}

	public void quitarLogo() {
		logos.quitar(iglesiaActual.requerida());
	}

}
