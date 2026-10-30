package com.iglesia.api.miembros.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.common.multiiglesia.component.IglesiaActual;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.model.ContactoPersona;
import com.iglesia.api.miembros.model.PersonaConsolidacion;
import com.iglesia.api.miembros.repository.EspecificacionesMiembro;
import com.iglesia.api.miembros.repository.MiembroRepository;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;
import com.iglesia.api.ubigeo.service.UbigeoService;

/**
 * Lo que la consolidación necesita de los miembros: datos para listarlos y contactarlos, la red de un líder y la
 * búsqueda por nombre. El ámbito lo decide la consolidación (sus permisos son otros que los de la ficha); aquí solo se
 * lee dentro de la iglesia del token.
 */
@Service
public class MiembrosParaConsolidacion {

	private final MiembroRepository miembros;

	private final UbigeoService ubigeo;

	private final IglesiaActual iglesiaActual;

	private final Clock reloj;

	MiembrosParaConsolidacion(MiembroRepository miembros, UbigeoService ubigeo, IglesiaActual iglesiaActual,
			Clock reloj) {
		this.miembros = miembros;
		this.ubigeo = ubigeo;
		this.iglesiaActual = iglesiaActual;
		this.reloj = reloj;
	}

	@Transactional(readOnly = true)
	public Map<Long, PersonaConsolidacion> porIds(Collection<Long> ids) {
		Set<Long> validos = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (validos.isEmpty()) {
			return Collections.emptyMap();
		}
		return miembros.findAllById(validos)
			.stream()
			.map(this::aPersona)
			.collect(Collectors.toMap(PersonaConsolidacion::id, Function.identity()));
	}

	@Transactional(readOnly = true)
	public Optional<PersonaConsolidacion> porId(Long id) {
		return Optional.ofNullable(porIds(List.of(id)).get(id));
	}

	/** Datos de contacto de la persona, con su ubicación, quién la invitó y su líder. */
	@Transactional(readOnly = true)
	public Optional<ContactoPersona> contacto(Long id) {
		return miembros.findWithReferenciasById(id).map(miembro -> {
			String ubicacion = Optional.ofNullable(miembro.getDistritoCodigo())
				.flatMap(ubigeo::ubicacion)
				.map(MiembrosParaConsolidacion::textoUbicacion)
				.orElse(null);
			return new ContactoPersona(miembro.getId(), miembro.nombreCompleto(), miembro.getTelefono(),
					miembro.getCorreo(), miembro.getDireccion(), ubicacion, miembro.getCondicion(), ingreso(miembro),
					miembro.getFechaNacimiento(), nombre(miembro.getInvitadoPor()), nombre(miembro.getLider()),
					miembro.getGrupoId(), miembro.getPeticion(), miembro.isActivo());
		});
	}

	/** Ids de toda la red del líder (a quienes lidera directa e indirectamente), sin incluirlo. */
	@Transactional(readOnly = true)
	public Set<Long> red(Long liderId) {
		return new HashSet<>(miembros.idsDeLaRed(iglesiaActual.requerida(), liderId));
	}

	/** Ids de los miembros (activos o no) cuyo nombre contiene el texto. */
	@Transactional(readOnly = true)
	public Set<Long> idsQueCoinciden(String q) {
		return miembros.findAll(EspecificacionesMiembro.nombreContiene(q))
			.stream()
			.map(Miembro::getId)
			.collect(Collectors.toSet());
	}

	private PersonaConsolidacion aPersona(Miembro miembro) {
		return new PersonaConsolidacion(miembro.getId(), miembro.nombreCompleto(), miembro.getTelefono(),
				miembro.getCondicion(), ingreso(miembro), miembro.getGrupoId(), miembro.isActivo());
	}

	private LocalDate ingreso(Miembro miembro) {
		return miembro.getFechaIngreso() != null ? miembro.getFechaIngreso()
				: LocalDate.ofInstant(miembro.getCreadoEn(), reloj.getZone());
	}

	private static String nombre(Miembro miembro) {
		return miembro == null ? null : miembro.nombreCompleto();
	}

	private static String textoUbicacion(UbicacionResponse ubicacion) {
		return ubicacion.distrito().nombre() + ", " + ubicacion.provincia().nombre();
	}

}
