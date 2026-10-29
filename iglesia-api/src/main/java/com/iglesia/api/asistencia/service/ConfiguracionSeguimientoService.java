package com.iglesia.api.asistencia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.entity.ConfiguracionSeguimiento;
import com.iglesia.api.asistencia.model.ReglasSeguimiento;
import com.iglesia.api.asistencia.repository.ConfiguracionSeguimientoRepository;
import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;

/** Reglas de las alertas de seguimiento de la iglesia del token; sin configurar valen las de por defecto. */
@Service
public class ConfiguracionSeguimientoService {

	private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionSeguimientoService.class);

	private final ConfiguracionSeguimientoRepository configuraciones;

	private final IglesiaActual iglesiaActual;

	private final UsuarioActual usuarioActual;

	ConfiguracionSeguimientoService(ConfiguracionSeguimientoRepository configuraciones, IglesiaActual iglesiaActual,
			UsuarioActual usuarioActual) {
		this.configuraciones = configuraciones;
		this.iglesiaActual = iglesiaActual;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public ReglasSeguimiento reglas() {
		return iglesiaActual.id()
			.flatMap(configuraciones::findById)
			.map(ConfiguracionSeguimiento::reglas)
			.orElseGet(ReglasSeguimiento::porDefecto);
	}

	@Transactional
	public ReglasSeguimiento guardar(ReglasSeguimiento reglas) {
		Long iglesiaId = iglesiaActual.requerida();
		ConfiguracionSeguimiento configuracion = configuraciones.findById(iglesiaId)
			.orElseGet(() -> new ConfiguracionSeguimiento(iglesiaId));
		configuracion.aplicar(reglas);
		configuraciones.save(configuracion);
		LOG.info("Reglas de seguimiento configuradas iglesiaId={} por usuarioId={}", iglesiaId,
				usuarioActual.id().orElse(null));
		return configuracion.reglas();
	}

}
