package com.iglesia.api.miembros.mapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.miembros.dto.EslabonCadenaResponse;
import com.iglesia.api.miembros.dto.MiembroResponse;
import com.iglesia.api.miembros.dto.MiembroResumenResponse;
import com.iglesia.api.miembros.dto.OpcionMiembroResponse;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.niveles.mapper.NivelMapper;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;

/** Convierte miembros en sus DTO. La edad se calcula aquí, con el reloj de la aplicación; nunca se guarda. */
@Component
public class MiembroMapper {

	private final NivelMapper niveles;

	private final Clock reloj;

	MiembroMapper(NivelMapper niveles, Clock reloj) {
		this.niveles = niveles;
		this.reloj = reloj;
	}

	public MiembroResumenResponse aResumen(Miembro miembro, Map<Long, ReferenciaResponse> grupos) {
		return new MiembroResumenResponse(miembro.getId(), miembro.getNombres(), miembro.getApellidos(),
				miembro.getCondicion(), nivel(miembro), aReferencia(miembro.getLider()),
				miembro.getGrupoId() == null ? null : grupos.get(miembro.getGrupoId()), miembro.getTelefono(), miembro.isPastorPrincipal(),
				miembro.isActivo());
	}

	public MiembroResponse aRespuesta(Miembro miembro, ReferenciaResponse grupo, UbicacionResponse ubicacion,
			long cantidadLiderados) {
		Integer edad = edad(miembro);
		boolean aproximada = miembro.getFechaNacimiento() == null && edad != null;
		return new MiembroResponse(miembro.getId(), miembro.getNombres(), miembro.getApellidos(),
				miembro.getGenero(), miembro.getFechaNacimiento(), edad, aproximada, miembro.getFechaIngreso(),
				miembro.getCondicion(), miembro.getEstadoCivil(), miembro.getTelefono(), miembro.getCorreo(),
				miembro.getDireccion(), miembro.getDistritoCodigo(), ubicacion, miembro.getOcupacion(),
				miembro.getProfesion(), miembro.getNivelEstudios(), miembro.getCentroEstudios(),
				miembro.getPeticion(), nivel(miembro), aReferencia(miembro.getLider()), grupo,
				aReferencia(miembro.getInvitadoPor()), aReferencia(miembro.getRegistradoPor()), cantidadLiderados,
				miembro.isPastorPrincipal(), miembro.isActivo(), miembro.getCreadoEn(), miembro.getActualizadoEn());
	}

	public OpcionMiembroResponse aOpcion(Miembro miembro) {
		return new OpcionMiembroResponse(miembro.getId(), miembro.nombreCompleto());
	}

	public EslabonCadenaResponse aEslabon(Miembro miembro) {
		String nivel = miembro.getNivel() == null ? null : miembro.getNivel().getNombre();
		return new EslabonCadenaResponse(miembro.getId(), miembro.nombreCompleto(), nivel,
				miembro.isPastorPrincipal(), miembro.isActivo());
	}

	public ReferenciaResponse aReferencia(Miembro miembro) {
		return miembro == null ? null
				: new ReferenciaResponse(miembro.getId(), miembro.nombreCompleto(), miembro.isActivo());
	}

	/** Año de nacimiento estimado a partir de una edad aproximada. */
	public Short anioEstimado(Integer edadAproximada) {
		return edadAproximada == null ? null : (short) (LocalDate.now(reloj).getYear() - edadAproximada);
	}

	private Integer edad(Miembro miembro) {
		LocalDate hoy = LocalDate.now(reloj);
		if (miembro.getFechaNacimiento() != null) {
			return Period.between(miembro.getFechaNacimiento(), hoy).getYears();
		}
		if (miembro.getAnioNacimientoEstimado() != null) {
			return hoy.getYear() - miembro.getAnioNacimientoEstimado();
		}
		return null;
	}

	private ReferenciaResponse nivel(Miembro miembro) {
		return miembro.getNivel() == null ? null : niveles.aReferencia(miembro.getNivel());
	}

}
