package com.iglesia.api.miembros.mapper;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.miembros.dto.FotocheckResponse;
import com.iglesia.api.miembros.dto.EslabonCadenaResponse;
import com.iglesia.api.miembros.dto.LideradoResponse;
import com.iglesia.api.miembros.dto.MiembroResponse;
import com.iglesia.api.miembros.dto.MiembroResumenResponse;
import com.iglesia.api.miembros.dto.OpcionMiembroResponse;
import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.miembros.model.CodigoAsistencia;
import com.iglesia.api.miembros.model.IntegranteGrupo;
import com.iglesia.api.miembros.model.MiembroAsistente;
import com.iglesia.api.niveles.mapper.NivelMapper;
import com.iglesia.api.ubigeo.dto.UbicacionResponse;

/**
 * Convierte miembros en sus DTO. La edad se calcula aquí, con el reloj de la aplicación; nunca se guarda. La fecha de
 * nacimiento y la edad aproximada son independientes: la exacta manda en {@code edad} y la aproximada va aparte.
 */
@Component
public class MiembroMapper {

	private final NivelMapper niveles;

	private final Clock reloj;

	MiembroMapper(NivelMapper niveles, Clock reloj) {
		this.niveles = niveles;
		this.reloj = reloj;
	}

	public MiembroResumenResponse aResumen(Miembro miembro, Map<String, UbicacionResponse> ubicaciones) {
		return new MiembroResumenResponse(miembro.getId(), miembro.getNombres(), miembro.getApellidos(),
				miembro.getCondicion(), nivel(miembro), aReferencia(miembro.getLider()), miembro.getDireccion(),
				miembro.getDistritoCodigo() == null ? null : ubicaciones.get(miembro.getDistritoCodigo()),
				miembro.getTelefono(), miembro.isPastorPrincipal(), miembro.isActivo());
	}

	public MiembroResponse aRespuesta(Miembro miembro, ReferenciaResponse grupo, UbicacionResponse ubicacion,
			long cantidadLiderados) {
		Integer edadAproximada = edadAproximada(miembro);
		Integer edad = miembro.getFechaNacimiento() == null ? edadAproximada : edadExacta(miembro);
		boolean aproximada = miembro.getFechaNacimiento() == null && edad != null;
		return new MiembroResponse(miembro.getId(), miembro.getNombres(), miembro.getApellidos(),
				miembro.getGenero(), miembro.getFechaNacimiento(), edad, aproximada, edadAproximada,
				miembro.getFechaIngreso(),
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

	public LideradoResponse aLiderado(Miembro miembro, long cantidadLiderados) {
		String nivel = miembro.getNivel() == null ? null : miembro.getNivel().getNombre();
		return new LideradoResponse(miembro.getId(), miembro.nombreCompleto(), nivel, cantidadLiderados);
	}

	public IntegranteGrupo aIntegrante(Miembro miembro) {
		String nivel = miembro.getNivel() == null ? null : miembro.getNivel().getNombre();
		return new IntegranteGrupo(miembro.getId(), miembro.nombreCompleto(), nivel, miembro.getGrupoId());
	}

	public FotocheckResponse aFotocheck(Miembro miembro) {
		CodigoAsistencia codigo = new CodigoAsistencia(miembro.getCodigoAsistencia());
		return new FotocheckResponse(miembro.getId(), miembro.getNombres(), miembro.getApellidos(),
				miembro.getNivel() == null ? null : miembro.getNivel().getNombre(), codigo.valor(), codigo.contenidoQr());
	}

	public MiembroAsistente aAsistente(Miembro miembro) {
		String nivel = miembro.getNivel() == null ? null : miembro.getNivel().getNombre();
		return new MiembroAsistente(miembro.getId(), miembro.nombreCompleto(), nivel, miembro.getGrupoId(),
				miembro.isActivo());
	}

	public ReferenciaResponse aReferencia(Miembro miembro) {
		return miembro == null ? null
				: new ReferenciaResponse(miembro.getId(), miembro.nombreCompleto(), miembro.isActivo());
	}

	/** Año de nacimiento estimado a partir de una edad aproximada. */
	public Short anioEstimado(Integer edadAproximada) {
		return edadAproximada == null ? null : (short) (LocalDate.now(reloj).getYear() - edadAproximada);
	}

	private Integer edadExacta(Miembro miembro) {
		return Period.between(miembro.getFechaNacimiento(), LocalDate.now(reloj)).getYears();
	}

	private Integer edadAproximada(Miembro miembro) {
		return miembro.getAnioNacimientoEstimado() == null ? null
				: LocalDate.now(reloj).getYear() - miembro.getAnioNacimientoEstimado();
	}

	private ReferenciaResponse nivel(Miembro miembro) {
		return miembro.getNivel() == null ? null : niveles.aReferencia(miembro.getNivel());
	}

}
