package com.iglesia.api.perfil.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.dto.AsistenciaMiembroResponse;
import com.iglesia.api.asistencia.dto.CodigoAsistenciaResponse;
import com.iglesia.api.asistencia.service.AsistenciaMiembroService;
import com.iglesia.api.asistencia.service.SeguimientoAsistenciaService;
import com.iglesia.api.common.api.dto.ReferenciaResponse;
import com.iglesia.api.grupos.service.AgendaGrupos;
import com.iglesia.api.miembros.dto.MiembroResponse;
import com.iglesia.api.miembros.service.MiembroService;
import com.iglesia.api.perfil.dto.GrupoPerfilResponse;
import com.iglesia.api.perfil.dto.MiPerfilResponse;

/**
 * Portal del miembro: su propia información, con el miembro tomado solo del token (nunca de un parámetro). Delega en
 * los servicios públicos de miembros, grupos y asistencia. No expone aportes ni consolidación.
 */
@Service
public class MiPerfilService {

	private final MiembroService miembros;

	private final AgendaGrupos grupos;

	private final SeguimientoAsistenciaService seguimiento;

	private final AsistenciaMiembroService asistencia;

	MiPerfilService(MiembroService miembros, AgendaGrupos grupos, SeguimientoAsistenciaService seguimiento,
			AsistenciaMiembroService asistencia) {
		this.miembros = miembros;
		this.grupos = grupos;
		this.seguimiento = seguimiento;
		this.asistencia = asistencia;
	}

	@Transactional(readOnly = true)
	public MiPerfilResponse datos() {
		MiembroResponse miembro = miembros.propio();
		GrupoPerfilResponse grupo = miembro.grupo() == null ? null
				: grupos.buscar(miembro.grupo().id())
					.map(agenda -> new GrupoPerfilResponse(agenda.id(), agenda.nombre(), agenda.diaReunion(),
							agenda.horaReunion()))
					.orElse(null);
		return new MiPerfilResponse(miembro.id(), miembro.nombres(), miembro.apellidos(), miembro.edad(),
				miembro.edadEsAproximada(), miembro.fechaIngreso(), miembro.condicion(), nombre(miembro.nivel()),
				nombre(miembro.lider()), grupo);
	}

	public AsistenciaMiembroResponse asistencia() {
		return seguimiento.delPropio();
	}

	public CodigoAsistenciaResponse codigoAsistencia() {
		return asistencia.codigoPropio();
	}

	private static String nombre(ReferenciaResponse referencia) {
		return referencia == null ? null : referencia.nombre();
	}

}
