package com.iglesia.api.grupos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.grupos.constant.LimitesGrupo;
import com.iglesia.api.grupos.dto.ConfiguracionIglesiaResponse;
import com.iglesia.api.grupos.dto.GuardarConfiguracionIglesiaRequest;
import com.iglesia.api.grupos.entity.ConfiguracionIglesia;
import com.iglesia.api.grupos.repository.ConfiguracionIglesiaRepository;

/** Nombre con el que la iglesia del token llama a sus grupos; sin configurar vale «Grupo» / «Grupos». */
@Service
public class ConfiguracionIglesiaService {

	private static final Logger LOG = LoggerFactory.getLogger(ConfiguracionIglesiaService.class);

	private static final ConfiguracionIglesiaResponse POR_DEFECTO = new ConfiguracionIglesiaResponse(
			LimitesGrupo.NOMBRE_SINGULAR_POR_DEFECTO, LimitesGrupo.NOMBRE_PLURAL_POR_DEFECTO);

	private final ConfiguracionIglesiaRepository configuraciones;

	private final IglesiaActual iglesiaActual;

	private final UsuarioActual usuarioActual;

	ConfiguracionIglesiaService(ConfiguracionIglesiaRepository configuraciones, IglesiaActual iglesiaActual,
			UsuarioActual usuarioActual) {
		this.configuraciones = configuraciones;
		this.iglesiaActual = iglesiaActual;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public ConfiguracionIglesiaResponse obtener() {
		return iglesiaActual.id().flatMap(configuraciones::findById).map(this::aRespuesta).orElse(POR_DEFECTO);
	}

	@Transactional
	public ConfiguracionIglesiaResponse guardar(GuardarConfiguracionIglesiaRequest solicitud) {
		Long iglesiaId = iglesiaActual.requerida();
		ConfiguracionIglesia configuracion = configuraciones.findById(iglesiaId)
			.orElseGet(() -> new ConfiguracionIglesia(iglesiaId));
		configuracion.nombrarGrupos(Textos.normalizar(solicitud.nombreGrupoSingular()),
				Textos.normalizar(solicitud.nombreGrupoPlural()));
		configuraciones.save(configuracion);
		LOG.info("Nombre de los grupos configurado iglesiaId={} por usuarioId={}", iglesiaId,
				usuarioActual.id().orElse(null));
		return aRespuesta(configuracion);
	}

	private ConfiguracionIglesiaResponse aRespuesta(ConfiguracionIglesia configuracion) {
		return new ConfiguracionIglesiaResponse(configuracion.getNombreGrupoSingular(),
				configuracion.getNombreGrupoPlural());
	}

}
