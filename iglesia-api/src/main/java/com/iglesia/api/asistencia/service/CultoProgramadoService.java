package com.iglesia.api.asistencia.service;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.dto.CultoProgramadoResponse;
import com.iglesia.api.asistencia.dto.GuardarCultoRequest;
import com.iglesia.api.asistencia.entity.CultoProgramado;
import com.iglesia.api.asistencia.mapper.AsistenciaMapper;
import com.iglesia.api.asistencia.model.DatosCulto;
import com.iglesia.api.asistencia.repository.CultoProgramadoRepository;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;

/** Horarios de culto fijos de la iglesia del token. Se desactivan, no se borran, para conservar su historial. */
@Service
public class CultoProgramadoService {

	private static final Logger LOG = LoggerFactory.getLogger(CultoProgramadoService.class);

	static final String NO_ENCONTRADO = "Horario de culto no encontrado";

	static final String NOMBRE_REPETIDO = "Ya existe un culto con ese nombre";

	private static final String CAMPO_NOMBRE = "nombre";

	/** De lunes a domingo y, el mismo día, por hora. */
	private static final Comparator<CultoProgramado> POR_DIA_Y_HORA = Comparator
		.comparing(CultoProgramado::getDiaSemana)
		.thenComparing(CultoProgramado::getHora)
		.thenComparing(CultoProgramado::getId);

	private final CultoProgramadoRepository cultos;

	private final AsistenciaMapper mapper;

	private final UsuarioActual usuarioActual;

	CultoProgramadoService(CultoProgramadoRepository cultos, AsistenciaMapper mapper, UsuarioActual usuarioActual) {
		this.cultos = cultos;
		this.mapper = mapper;
		this.usuarioActual = usuarioActual;
	}

	@Transactional(readOnly = true)
	public List<CultoProgramadoResponse> listar() {
		return cultos.findAll().stream().sorted(POR_DIA_Y_HORA).map(mapper::aRespuesta).toList();
	}

	@Transactional
	public CultoProgramadoResponse crear(GuardarCultoRequest solicitud) {
		DatosCulto datos = datos(solicitud, null);
		CultoProgramado culto = cultos.saveAndFlush(CultoProgramado.nuevo(datos));
		LOG.info("Horario de culto creado id={} por usuarioId={}", culto.getId(), usuario());
		return mapper.aRespuesta(culto);
	}

	@Transactional
	public CultoProgramadoResponse actualizar(Long id, GuardarCultoRequest solicitud) {
		CultoProgramado culto = buscar(id);
		culto.actualizar(datos(solicitud, id));
		cultos.flush();
		LOG.info("Horario de culto actualizado id={} por usuarioId={}", id, usuario());
		return mapper.aRespuesta(culto);
	}

	@Transactional
	public CultoProgramadoResponse cambiarEstado(Long id, boolean activo) {
		CultoProgramado culto = buscar(id);
		culto.cambiarEstado(activo);
		LOG.info("Horario de culto {} id={} por usuarioId={}", activo ? "activado" : "desactivado", id, usuario());
		return mapper.aRespuesta(culto);
	}

	/** Lo usa la apertura de reuniones: un culto inexistente (u otra iglesia) responde 404. */
	CultoProgramado buscar(Long id) {
		return cultos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	private DatosCulto datos(GuardarCultoRequest solicitud, Long idExcluido) {
		String nombre = Textos.normalizar(solicitud.nombre());
		boolean ocupado = cultos.findAll()
			.stream()
			.anyMatch(culto -> !culto.getId().equals(idExcluido) && Textos.mismoTexto(culto.getNombre(), nombre));
		if (ocupado) {
			throw new ConflictoException(NOMBRE_REPETIDO, CAMPO_NOMBRE);
		}
		return new DatosCulto(nombre, solicitud.diaSemana(), solicitud.hora());
	}

	private Long usuario() {
		return usuarioActual.id().orElse(null);
	}

}
