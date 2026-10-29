package com.iglesia.api.aportes.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.aportes.dto.AnularAporteRequest;
import com.iglesia.api.aportes.dto.AporteResponse;
import com.iglesia.api.aportes.dto.FiltroAportes;
import com.iglesia.api.aportes.dto.GuardarAporteRequest;
import com.iglesia.api.aportes.entity.Aporte;
import com.iglesia.api.aportes.entity.CategoriaAporte;
import com.iglesia.api.aportes.mapper.AporteMapper;
import com.iglesia.api.aportes.model.DatosAporte;
import com.iglesia.api.aportes.model.MedioAporte;
import com.iglesia.api.aportes.repository.AporteRepository;
import com.iglesia.api.aportes.repository.EspecificacionesAporte;
import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.seguridad.component.UsuarioActual;
import com.iglesia.api.common.texto.util.Textos;
import com.iglesia.api.miembros.service.ReferenciasMiembro;

/**
 * Registro, corrección, anulación y consulta de los aportes de la iglesia del token. La lectura respeta el ámbito
 * ({@link AmbitoAportes}); la escritura la autoriza el controlador por permiso. Nunca registra montos, nombres,
 * peticiones ni observaciones en los logs.
 */
@Service
public class AporteService {

	private static final Logger LOG = LoggerFactory.getLogger(AporteService.class);

	static final String NO_ENCONTRADO = "Aporte no encontrado";

	static final String EXIGE_APORTANTE = "Esta categoría exige indicar quién aporta";

	static final String ANULADO = "El aporte está anulado y no se puede modificar";

	static final String YA_ANULADO = "El aporte ya está anulado";

	private static final String CAMPO_MIEMBRO = "miembroId";

	private final AporteRepository aportes;

	private final AporteMapper mapper;

	private final CategoriaAporteService categorias;

	private final ReferenciasMiembro miembros;

	private final ConsultaAportes consulta;

	private final UsuarioActual usuarioActual;

	private final Clock reloj;

	AporteService(AporteRepository aportes, AporteMapper mapper, CategoriaAporteService categorias,
			ReferenciasMiembro miembros, ConsultaAportes consulta, UsuarioActual usuarioActual, Clock reloj) {
		this.aportes = aportes;
		this.mapper = mapper;
		this.categorias = categorias;
		this.miembros = miembros;
		this.consulta = consulta;
		this.usuarioActual = usuarioActual;
		this.reloj = reloj;
	}

	/** Página de aportes del rango, con los aportantes resueltos en un solo lote. */
	@Transactional(readOnly = true)
	public Page<AporteResponse> listar(FiltroAportes filtro, Pageable paginacion) {
		boolean incluirAnulados = Boolean.TRUE.equals(filtro.incluirAnulados());
		Page<Aporte> pagina = consulta.criterio(filtro)
			.map(criterio -> aportes.findAll(EspecificacionesAporte.segun(criterio, incluirAnulados), paginacion))
			.orElseGet(() -> aportes.findAll(EspecificacionesAporte.ninguno(), paginacion));
		Map<Long, ReferenciaResponse> aportantes = miembros
			.referencias(pagina.getContent().stream().map(Aporte::getMiembroId).toList());
		return pagina.map(aporte -> mapper.aRespuesta(aporte, aportantes.get(aporte.getMiembroId())));
	}

	@Transactional(readOnly = true)
	public AporteResponse obtener(Long id) {
		Aporte aporte = aportes.findWithCategoriaById(id)
			.filter(encontrado -> consulta.alcance().incluye(encontrado.getMiembroId()))
			.orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
		return aRespuesta(aporte);
	}

	@Transactional
	public AporteResponse crear(GuardarAporteRequest solicitud) {
		CategoriaAporte categoria = categorias.asignable(solicitud.categoriaId(), null);
		validarAportante(solicitud.miembroId(), null, categoria);
		Aporte aporte = aportes.save(Aporte.nuevo(datos(solicitud), categoria, usuario(), ahora()));
		LOG.info("Aporte registrado id={} por usuarioId={}", aporte.getId(), usuario());
		return aRespuesta(aporte);
	}

	@Transactional
	public AporteResponse actualizar(Long id, GuardarAporteRequest solicitud) {
		Aporte aporte = buscar(id);
		if (aporte.isAnulado()) {
			throw new ConflictoException(ANULADO);
		}
		CategoriaAporte categoria = categorias.asignable(solicitud.categoriaId(), aporte.getCategoria());
		validarAportante(solicitud.miembroId(), aporte.getMiembroId(), categoria);
		aporte.corregir(datos(solicitud), categoria, usuario(), ahora());
		LOG.info("Aporte corregido id={} por usuarioId={}", id, usuario());
		return aRespuesta(aporte);
	}

	@Transactional
	public AporteResponse anular(Long id, AnularAporteRequest solicitud) {
		Aporte aporte = buscar(id);
		if (aporte.isAnulado()) {
			throw new ConflictoException(YA_ANULADO);
		}
		aporte.anular(Textos.normalizar(solicitud.motivo()), usuario(), ahora());
		LOG.info("Aporte anulado id={} por usuarioId={}", id, usuario());
		return aRespuesta(aporte);
	}

	/**
	 * El aportante es obligatorio si la categoría lo exige; si se indica, debe ser un miembro de la iglesia, activo
	 * al asignarlo (el que ya tenía el aporte se conserva aunque se haya desactivado).
	 */
	private void validarAportante(Long miembroId, Long actualId, CategoriaAporte categoria) {
		if (miembroId == null && categoria.isExigeAportante()) {
			throw new ConflictoException(EXIGE_APORTANTE, CAMPO_MIEMBRO);
		}
		miembros.validarAsignable(miembroId, actualId, CAMPO_MIEMBRO);
	}

	/** Textos limpios; el número de operación no aplica al efectivo. */
	private static DatosAporte datos(GuardarAporteRequest solicitud) {
		String numeroOperacion = solicitud.medio() == MedioAporte.EFECTIVO ? null
				: Textos.normalizarOpcional(solicitud.numeroOperacion());
		return new DatosAporte(solicitud.fecha(), solicitud.miembroId(), solicitud.monto(), solicitud.medio(),
				numeroOperacion, Textos.recortarOpcional(solicitud.peticion()),
				Textos.recortarOpcional(solicitud.observacion()));
	}

	private AporteResponse aRespuesta(Aporte aporte) {
		ReferenciaResponse aportante = aporte.getMiembroId() == null ? null
				: miembros.referencias(List.of(aporte.getMiembroId())).get(aporte.getMiembroId());
		return mapper.aRespuesta(aporte, aportante);
	}

	private Aporte buscar(Long id) {
		return aportes.findWithCategoriaById(id).orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO));
	}

	/** Quien registra o modifica: toda petición que escribe llega autenticada. */
	private Long usuario() {
		return usuarioActual.id().orElseThrow(() -> new IllegalStateException("Petición sin usuario autenticado"));
	}

	private Instant ahora() {
		return Instant.now(reloj);
	}

}
