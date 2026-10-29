package com.iglesia.api.iglesia.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.iglesia.constant.AparienciaAdmitida;
import com.iglesia.api.iglesia.dto.AparienciaResponse;
import com.iglesia.api.iglesia.dto.FiltroIglesias;
import com.iglesia.api.iglesia.dto.GuardarAparienciaRequest;
import com.iglesia.api.iglesia.dto.GuardarIglesiaRequest;
import com.iglesia.api.iglesia.dto.IglesiaResponse;
import com.iglesia.api.iglesia.entity.Iglesia;
import com.iglesia.api.iglesia.mapper.IglesiaMapper;
import com.iglesia.api.iglesia.model.Apariencia;
import com.iglesia.api.iglesia.model.DatosIglesia;
import com.iglesia.api.iglesia.model.IglesiaCreada;
import com.iglesia.api.iglesia.repository.EspecificacionesIglesia;
import com.iglesia.api.iglesia.repository.IglesiaRepository;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;
import com.iglesia.api.ubigeo.service.UbigeoService;

/**
 * Catálogo de iglesias (tenants) con su identidad y su apariencia. Dar de alta una iglesia es insertar una fila, sin
 * crear bases ni esquemas. El nombre comercial es único sin distinguir mayúsculas; el índice
 * {@code ux_iglesia_nombre_comercial} lo garantiza aunque dos altas coincidan en el tiempo. Al dar de alta publica
 * {@link IglesiaCreada} para que otros contextos preparen sus datos iniciales en la misma transacción.
 * <p>
 * Recibe siempre el id de la iglesia: quien lo decide es el llamador ({@code IglesiaController} con el id del
 * catálogo, solo para el administrador del sistema, o {@link MiIglesiaService} con la iglesia del token).
 */
@Service
public class IglesiaService {

	private static final Logger LOG = LoggerFactory.getLogger(IglesiaService.class);

	static final String NOMBRE_REPETIDO = "Ya existe una iglesia con ese nombre comercial";

	static final String NO_ENCONTRADA = "Iglesia no encontrada";

	static final String PRESET_NO_ADMITIDO = "Elige el preset Aura, Lara o Nora";

	static final String COLOR_NO_ADMITIDO = "Elige uno de los colores primarios disponibles";

	static final String SUPERFICIE_NO_ADMITIDA = "Elige una de las superficies disponibles";

	static final String TIPOGRAFIA_NO_ADMITIDA = "Elige una de las tipografías disponibles";

	static final String CAMPO_NOMBRE_COMERCIAL = "nombreComercial";

	static final String CAMPO_DISTRITO = "distritoCodigo";

	private final IglesiaRepository iglesias;

	private final IglesiaMapper mapper;

	private final UbigeoService ubigeo;

	private final UsuarioActual usuarioActual;

	private final ApplicationEventPublisher eventos;

	IglesiaService(IglesiaRepository iglesias, IglesiaMapper mapper, UbigeoService ubigeo, UsuarioActual usuarioActual,
			ApplicationEventPublisher eventos) {
		this.iglesias = iglesias;
		this.mapper = mapper;
		this.ubigeo = ubigeo;
		this.usuarioActual = usuarioActual;
		this.eventos = eventos;
	}

	/** Página del catálogo filtrada; las ubicaciones de la página se resuelven en una sola consulta. */
	@Transactional(readOnly = true)
	public Page<IglesiaResponse> listar(FiltroIglesias filtro, Pageable paginacion) {
		Specification<Iglesia> criterio = Specification.allOf(EspecificacionesIglesia.nombreContiene(filtro.q()),
				EspecificacionesIglesia.enUbigeo(filtro.ubigeo()), EspecificacionesIglesia.conEstado(filtro.activa()));
		Page<Iglesia> pagina = iglesias.findAll(criterio, paginacion);
		Map<String, UbicacionResponse> ubicaciones = ubigeo
			.ubicaciones(pagina.getContent().stream().map(Iglesia::getDistritoCodigo).toList());
		return pagina.map(iglesia -> mapper.aRespuesta(iglesia, ubicaciones.get(iglesia.getDistritoCodigo())));
	}

	@Transactional(readOnly = true)
	public IglesiaResponse obtener(Long id) {
		return aRespuesta(buscar(id));
	}

	@Transactional
	public IglesiaResponse crear(GuardarIglesiaRequest solicitud) {
		DatosIglesia datos = validar(solicitud);
		if (iglesias.existsByNombreComercialIgnoreCase(datos.nombreComercial())) {
			throw new ConflictoException(NOMBRE_REPETIDO, CAMPO_NOMBRE_COMERCIAL);
		}
		Iglesia iglesia = guardar(Iglesia.nueva(datos));
		eventos.publishEvent(new IglesiaCreada(iglesia.getId()));
		LOG.info("Iglesia creada id={} por usuarioId={}", iglesia.getId(), usuarioActual.id().orElse(null));
		return aRespuesta(iglesia);
	}

	@Transactional
	public IglesiaResponse actualizar(Long id, GuardarIglesiaRequest solicitud) {
		Iglesia iglesia = buscar(id);
		DatosIglesia datos = validar(solicitud);
		if (iglesias.existsByNombreComercialIgnoreCaseAndIdNot(datos.nombreComercial(), id)) {
			throw new ConflictoException(NOMBRE_REPETIDO, CAMPO_NOMBRE_COMERCIAL);
		}
		iglesia.actualizar(datos);
		guardar(iglesia);
		LOG.info("Iglesia actualizada id={} por usuarioId={}", id, usuarioActual.id().orElse(null));
		return aRespuesta(iglesia);
	}

	@Transactional
	public AparienciaResponse cambiarApariencia(Long id, GuardarAparienciaRequest solicitud) {
		Iglesia iglesia = buscar(id);
		iglesia.cambiarApariencia(validar(solicitud));
		LOG.info("Apariencia de la iglesia id={} cambiada por usuarioId={}", id, usuarioActual.id().orElse(null));
		return mapper.aRespuesta(iglesia.getApariencia());
	}

	Iglesia buscar(Long id) {
		return iglesias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADA));
	}

	private IglesiaResponse aRespuesta(Iglesia iglesia) {
		return mapper.aRespuesta(iglesia, ubigeo.ubicacion(iglesia.getDistritoCodigo()).orElse(null));
	}

	private DatosIglesia validar(GuardarIglesiaRequest solicitud) {
		ubigeo.validarDistrito(solicitud.distritoCodigo(), CAMPO_DISTRITO);
		return new DatosIglesia(Textos.normalizar(solicitud.nombreComercial()),
				Textos.normalizar(solicitud.denominacion()), Textos.normalizar(solicitud.direccion()),
				solicitud.distritoCodigo(), Textos.normalizarOpcional(solicitud.telefono()),
				Textos.normalizarOpcional(solicitud.correo()), Textos.normalizarOpcional(solicitud.sitioWeb()));
	}

	private static Apariencia validar(GuardarAparienciaRequest solicitud) {
		exigir(AparienciaAdmitida.PRESETS.contains(solicitud.preset()), PRESET_NO_ADMITIDO, "preset");
		exigir(AparienciaAdmitida.COLORES_PRIMARIOS.contains(solicitud.colorPrimario()), COLOR_NO_ADMITIDO,
				"colorPrimario");
		String superficie = Textos.normalizarOpcional(solicitud.colorSuperficie());
		exigir(superficie == null || AparienciaAdmitida.SUPERFICIES.contains(superficie), SUPERFICIE_NO_ADMITIDA,
				"colorSuperficie");
		exigir(AparienciaAdmitida.TIPOGRAFIAS.contains(solicitud.tipografia()), TIPOGRAFIA_NO_ADMITIDA, "tipografia");
		return new Apariencia(solicitud.preset(), solicitud.colorPrimario(), superficie, solicitud.tipografia());
	}

	private static void exigir(boolean condicion, String mensaje, String campo) {
		if (!condicion) {
			throw new ReglaNegocioException(mensaje, campo);
		}
	}

	/** Escribe ya para que un nombre repetido por un alta simultánea responda 409 y no 500. */
	private Iglesia guardar(Iglesia iglesia) {
		try {
			return iglesias.saveAndFlush(iglesia);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConflictoException(NOMBRE_REPETIDO, CAMPO_NOMBRE_COMERCIAL);
		}
	}

}
