package com.iglesia.api.iglesia.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ContenidoDemasiadoGrandeException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.iglesia.component.AlmacenLogoIglesia;
import com.iglesia.api.iglesia.constant.LimitesIglesia;
import com.iglesia.api.iglesia.entity.Iglesia;
import com.iglesia.api.iglesia.model.LogoIglesia;
import com.iglesia.api.iglesia.model.TipoImagen;

/**
 * Logo de una iglesia: se valida por su firma (PNG, JPEG o WebP) y su tamaño, se guarda en {@code logo_iglesia} y se
 * anota en el catálogo cuándo se subió, que sirve de {@code ETag}. Como {@link IglesiaService}, recibe el id de la
 * iglesia ya decidido por el llamador.
 */
@Service
public class LogoIglesiaService {

	private static final Logger LOG = LoggerFactory.getLogger(LogoIglesiaService.class);

	static final String SIN_LOGO = "La iglesia no tiene logo";

	static final String DEMASIADO_GRANDE = "La imagen supera 1 MB; elige una más liviana";

	static final String VACIO = "Elige una imagen para el logo";

	static final String TIPO_NO_ADMITIDO = "El archivo no es una imagen PNG, JPEG o WebP";

	static final String CAMPO_LOGO = "logo";

	private final IglesiaService catalogo;

	private final AlmacenLogoIglesia almacen;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	LogoIglesiaService(IglesiaService catalogo, AlmacenLogoIglesia almacen, UsuarioActual usuarioActual, Clock reloj) {
		this.catalogo = catalogo;
		this.almacen = almacen;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public LogoIglesia obtener(Long iglesiaId) {
		Iglesia iglesia = catalogo.buscar(iglesiaId);
		if (!iglesia.tieneLogo()) {
			throw new RecursoNoEncontradoException(SIN_LOGO);
		}
		return almacen.leer(iglesiaId)
			.map(imagen -> new LogoIglesia(imagen.contenido(), imagen.tipo(), iglesia.getLogoActualizadoEn()))
			.orElseThrow(() -> new RecursoNoEncontradoException(SIN_LOGO));
	}

	/**
	 * Lee como máximo un byte más que el límite: así se rechaza un archivo enorme sin cargarlo entero en memoria.
	 */
	@Transactional
	public void guardar(Long iglesiaId, InputStream entrada) {
		Iglesia iglesia = catalogo.buscar(iglesiaId);
		byte[] contenido = leerAcotado(entrada);
		if (contenido.length == 0) {
			throw new ReglaNegocioException(VACIO, CAMPO_LOGO);
		}
		TipoImagen tipo = TipoImagen.detectar(contenido)
			.orElseThrow(() -> new ReglaNegocioException(TIPO_NO_ADMITIDO, CAMPO_LOGO));
		almacen.guardar(iglesiaId, contenido, tipo);
		iglesia.registrarLogo(reloj.instant());
		LOG.info("Logo de la iglesia id={} actualizado por usuarioId={}", iglesiaId, usuarioActual.id().orElse(null));
	}

	@Transactional
	public void quitar(Long iglesiaId) {
		Iglesia iglesia = catalogo.buscar(iglesiaId);
		almacen.borrar(iglesiaId);
		iglesia.quitarLogo();
		LOG.info("Logo de la iglesia id={} quitado por usuarioId={}", iglesiaId, usuarioActual.id().orElse(null));
	}

	private static byte[] leerAcotado(InputStream entrada) {
		try {
			byte[] contenido = entrada.readNBytes(LimitesIglesia.LOGO_MAX_BYTES + 1);
			if (contenido.length > LimitesIglesia.LOGO_MAX_BYTES) {
				throw new ContenidoDemasiadoGrandeException(DEMASIADO_GRANDE);
			}
			return contenido;
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
