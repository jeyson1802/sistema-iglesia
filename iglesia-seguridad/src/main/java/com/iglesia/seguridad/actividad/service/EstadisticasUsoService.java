package com.iglesia.seguridad.actividad.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.seguridad.actividad.dto.ConteoResponse;
import com.iglesia.seguridad.actividad.dto.EstadisticasUsoResponse;
import com.iglesia.seguridad.actividad.repository.ActividadUsuarioRepository.FilaConteo;
import com.iglesia.seguridad.actividad.repository.ActividadUsuarioRepository.FilaSerie;
import com.iglesia.seguridad.actividad.repository.ActividadUsuarioRepository;
import com.iglesia.seguridad.common.periodo.component.CalculadoraPeriodo;
import com.iglesia.seguridad.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.seguridad.common.periodo.dto.PeriodoResponse;
import com.iglesia.seguridad.common.periodo.dto.PuntoSerieResponse;
import com.iglesia.seguridad.common.periodo.model.RangoPeriodo;
import com.iglesia.seguridad.common.seguridad.component.ProveedorUsuarioAutenticado;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;
import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.model.RolBase;

/**
 * Cifras de uso del sistema a partir de la actividad diaria. El alcance sale solo del token: el administrador del
 * sistema en la vista global (con {@code estadisticas.sistema}) ve todas las iglesias; quien opera en una iglesia
 * (con {@code usuarios.ver}), solo esa iglesia.
 */
@Service
public class EstadisticasUsoService {

	private static final Logger LOG = LoggerFactory.getLogger(EstadisticasUsoService.class);

	/** Días de la ventana de «activos semanales». */
	static final int DIAS_SEMANA = 7;

	/** Días de la ventana de «activos mensuales». */
	static final int DIAS_MES = 30;

	private final ActividadUsuarioRepository actividades;

	private final CalculadoraPeriodo calculadora;

	private final ProveedorUsuarioAutenticado proveedorUsuario;

	EstadisticasUsoService(ActividadUsuarioRepository actividades, CalculadoraPeriodo calculadora,
			ProveedorUsuarioAutenticado proveedorUsuario) {
		this.actividades = actividades;
		this.calculadora = calculadora;
		this.proveedorUsuario = proveedorUsuario;
	}

	@Transactional(readOnly = true)
	public EstadisticasUsoResponse calcular(ConsultaPeriodo periodo) {
		UsuarioAutenticado usuario = proveedorUsuario.actual();
		boolean global = usuario.esGlobal();
		Long iglesiaId = iglesiaDelAlcance(usuario, global);
		RangoPeriodo rango = calculadora.rango(periodo);
		LocalDate hoy = rango.hasta();
		LOG.debug("Estadísticas de uso periodo={} global={} iglesiaId={}", periodo, global, iglesiaId);
		return new EstadisticasUsoResponse(PeriodoResponse.de(rango), global,
				actividades.usuariosHabilitados(iglesiaId),
				actividades.usuariosCreados(iglesiaId, rango.desde(), rango.hasta()),
				actividades.usuariosActivos(iglesiaId, rango.desde(), rango.hasta()),
				actividades.usuariosActivos(iglesiaId, rango.anteriorDesde(), rango.anteriorHasta()),
				promedioDiario(iglesiaId, rango.desde(), rango.hasta(), rango.dias()),
				promedioDiario(iglesiaId, rango.anteriorDesde(), rango.anteriorHasta(), rango.dias()),
				actividades.usuariosActivos(iglesiaId, hoy.minusDays(DIAS_SEMANA - 1L), hoy),
				actividades.usuariosActivos(iglesiaId, hoy.minusDays(DIAS_MES - 1L), hoy),
				global ? actividades.iglesiasActivas(rango.desde(), rango.hasta()) : null,
				global ? actividades.iglesiasActivas(rango.anteriorDesde(), rango.anteriorHasta()) : null,
				serie(iglesiaId, rango), porRolBase(iglesiaId, rango, global));
	}

	/** {@code null} cuenta todas las iglesias; solo lo obtiene la vista global con el permiso del sistema. */
	private static Long iglesiaDelAlcance(UsuarioAutenticado usuario, boolean global) {
		if (global) {
			if (!usuario.permisos().contains(Permisos.ESTADISTICAS_SISTEMA)) {
				throw new AccessDeniedException("Sin permiso para ver el uso de todas las iglesias");
			}
			return null;
		}
		if (usuario.iglesiaId() == null || !usuario.permisos().contains(Permisos.USUARIOS_VER)) {
			throw new AccessDeniedException("Sin permiso para ver el uso de la iglesia");
		}
		return usuario.iglesiaId();
	}

	private double promedioDiario(Long iglesiaId, LocalDate desde, LocalDate hasta, long dias) {
		return Math.round(actividades.diasUsuario(iglesiaId, desde, hasta) * 10.0 / dias) / 10.0;
	}

	private List<PuntoSerieResponse> serie(Long iglesiaId, RangoPeriodo rango) {
		Map<LocalDate, Long> valores = actividades
			.serie(iglesiaId, rango.desde(), rango.hasta(), rango.granularidad().unidadSql())
			.stream()
			.collect(Collectors.toMap(FilaSerie::getInicio, FilaSerie::getValor));
		return rango.inicios()
			.stream()
			.map(inicio -> new PuntoSerieResponse(inicio, valores.getOrDefault(inicio, 0L)))
			.toList();
	}

	/** Todos los roles base del alcance, aunque no tengan actividad; en una iglesia no aparece el del sistema. */
	private List<ConteoResponse> porRolBase(Long iglesiaId, RangoPeriodo rango, boolean global) {
		Map<String, Long> valores = actividades.porRolBase(iglesiaId, rango.desde(), rango.hasta())
			.stream()
			.collect(Collectors.toMap(FilaConteo::getClave, FilaConteo::getValor));
		return Arrays.stream(RolBase.values())
			.filter(rol -> global || rol != RolBase.ADMIN_SISTEMA)
			.map(rol -> new ConteoResponse(rol.name(), valores.getOrDefault(rol.name(), 0L)))
			.toList();
	}

}
