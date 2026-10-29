package com.iglesia.api.aportes.service;

import java.util.Currency;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.aportes.constant.MonedasAdmitidas;
import com.iglesia.api.aportes.dto.ConfiguracionAportesResponse;
import com.iglesia.api.aportes.dto.GuardarConfiguracionAportesRequest;
import com.iglesia.api.aportes.dto.MonedaResponse;
import com.iglesia.api.aportes.entity.ConfiguracionAportes;
import com.iglesia.api.aportes.repository.AporteRepository;
import com.iglesia.api.aportes.repository.ConfiguracionAportesRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;

/**
 * Moneda de los aportes de la iglesia del token; sin configurar vale {@code PEN}. Solo se puede cambiar mientras la
 * iglesia no tenga aportes vigentes, para que ningún monto ya registrado cambie de significado.
 */
@Service
public class ConfiguracionAportesService {

	private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionAportesService.class);

	static final String MONEDA_NO_ADMITIDA = "La moneda no está entre las admitidas";

	static final String MONEDA_BLOQUEADA = "Ya hay aportes registrados en %s: no se puede cambiar la moneda";

	private static final String CAMPO_MONEDA = "moneda";

	/** Idioma de los nombres y región de los símbolos de las monedas (p. ej. «S/» para el sol). */
	private static final Locale ESPANOL = Locale.of("es", "PE");

	private final ConfiguracionAportesRepository configuraciones;

	private final AporteRepository aportes;

	private final IglesiaActual iglesiaActual;

	private final UsuarioActual usuarioActual;

	ConfiguracionAportesService(ConfiguracionAportesRepository configuraciones, AporteRepository aportes,
			IglesiaActual iglesiaActual, UsuarioActual usuarioActual) {
		this.configuraciones = configuraciones;
		this.aportes = aportes;
		this.iglesiaActual = iglesiaActual;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public ConfiguracionAportesResponse obtener() {
		return new ConfiguracionAportesResponse(monedaActual(), !aportes.existsByAnuladoFalse());
	}

	/** Moneda vigente de la iglesia del token. */
	@Transactional(readOnly = true)
	public String monedaActual() {
		return iglesiaActual.id()
			.flatMap(configuraciones::findById)
			.map(ConfiguracionAportes::getMoneda)
			.orElse(MonedasAdmitidas.POR_DEFECTO);
	}

	@Transactional
	public ConfiguracionAportesResponse guardar(GuardarConfiguracionAportesRequest solicitud) {
		String moneda = solicitud.moneda();
		if (!MonedasAdmitidas.CODIGOS.contains(moneda)) {
			throw new ReglaNegocioException(MONEDA_NO_ADMITIDA, CAMPO_MONEDA);
		}
		String actual = monedaActual();
		if (moneda.equals(actual)) {
			return obtener();
		}
		if (aportes.existsByAnuladoFalse()) {
			throw new ConflictoException(MONEDA_BLOQUEADA.formatted(actual), CAMPO_MONEDA);
		}
		Long iglesiaId = iglesiaActual.requerida();
		ConfiguracionAportes configuracion = configuraciones.findById(iglesiaId)
			.orElseGet(() -> new ConfiguracionAportes(iglesiaId));
		configuracion.cambiarMoneda(moneda);
		configuraciones.save(configuracion);
		LOG.info("Moneda de aportes configurada iglesiaId={} por usuarioId={}", iglesiaId,
				usuarioActual.id().orElse(null));
		return new ConfiguracionAportesResponse(moneda, true);
	}

	public List<MonedaResponse> monedas() {
		return MonedasAdmitidas.CODIGOS.stream().map(Currency::getInstance)
			.map(moneda -> new MonedaResponse(moneda.getCurrencyCode(), moneda.getDisplayName(ESPANOL),
					moneda.getSymbol(ESPANOL)))
			.toList();
	}

}
