package com.iglesia.api.ubigeo.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;
import com.iglesia.api.ubigeo.dto.UbigeoResponse;
import com.iglesia.api.ubigeo.entity.Distrito;
import com.iglesia.api.ubigeo.mapper.UbigeoMapper;
import com.iglesia.api.ubigeo.repository.DepartamentoRepository;
import com.iglesia.api.ubigeo.repository.DistritoRepository;
import com.iglesia.api.ubigeo.repository.ProvinciaRepository;

/** Consulta del ubigeo INEI para los selectores en cascada y para validar el distrito de miembros y grupos. */
@Service
public class UbigeoService {

	static final String DEPARTAMENTO_NO_ENCONTRADO = "Departamento no encontrado";

	static final String PROVINCIA_NO_ENCONTRADA = "Provincia no encontrada";

	static final String DISTRITO_INEXISTENTE = "El distrito no existe en el ubigeo";

	private final DepartamentoRepository departamentos;

	private final ProvinciaRepository provincias;

	private final DistritoRepository distritos;

	private final UbigeoMapper mapper;

	UbigeoService(DepartamentoRepository departamentos, ProvinciaRepository provincias,
			DistritoRepository distritos, UbigeoMapper mapper) {
		this.departamentos = departamentos;
		this.provincias = provincias;
		this.distritos = distritos;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	public List<UbigeoResponse> departamentos() {
		return departamentos.findAllByOrderByNombreAsc().stream().map(mapper::aRespuesta).toList();
	}

	@Transactional(readOnly = true)
	public List<UbigeoResponse> provincias(String departamentoCodigo) {
		if (!departamentos.existsById(departamentoCodigo)) {
			throw new RecursoNoEncontradoException(DEPARTAMENTO_NO_ENCONTRADO);
		}
		return provincias.findByDepartamentoCodigoOrderByNombreAsc(departamentoCodigo)
			.stream()
			.map(mapper::aRespuesta)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<UbigeoResponse> distritos(String provinciaCodigo) {
		if (!provincias.existsById(provinciaCodigo)) {
			throw new RecursoNoEncontradoException(PROVINCIA_NO_ENCONTRADA);
		}
		return distritos.findByProvinciaCodigoOrderByNombreAsc(provinciaCodigo)
			.stream()
			.map(mapper::aRespuesta)
			.toList();
	}

	/** Departamento, provincia y distrito del código; vacío si no se indicó distrito. */
	@Transactional(readOnly = true)
	public Optional<UbicacionResponse> ubicacion(String distritoCodigo) {
		if (distritoCodigo == null) {
			return Optional.empty();
		}
		return distritos.buscarConUbicacion(distritoCodigo).map(mapper::aUbicacion);
	}

	/** Ubicaciones de varios distritos en una sola consulta, por código. Los nulos no aparecen. */
	@Transactional(readOnly = true)
	public Map<String, UbicacionResponse> ubicaciones(Collection<String> distritoCodigos) {
		Set<String> codigos = distritoCodigos.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (codigos.isEmpty()) {
			return Collections.emptyMap();
		}
		return distritos.buscarConUbicacion(codigos)
			.stream()
			.collect(Collectors.toMap(Distrito::getCodigo, mapper::aUbicacion));
	}

	/** Rechaza con 400 un código de distrito que no existe; un código nulo es válido (sin ubicación). */
	@Transactional(readOnly = true)
	public void validarDistrito(String distritoCodigo, String campo) {
		if (distritoCodigo != null && !distritos.existsById(distritoCodigo)) {
			throw new ReglaNegocioException(DISTRITO_INEXISTENTE, campo);
		}
	}

}
