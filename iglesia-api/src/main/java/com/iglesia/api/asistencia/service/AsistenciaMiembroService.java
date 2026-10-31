package com.iglesia.api.asistencia.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.asistencia.dto.CodigoAsistenciaResponse;
import com.iglesia.api.miembros.model.CodigoAsistencia;
import com.iglesia.api.miembros.service.MiembrosParaAsistencia;

/**
 * El código del QR personal de un miembro, visto desde su ficha. Solo se ve o regenera dentro del ámbito de la ficha
 * (fuera de él responde 404). Su asistencia la resume {@link SeguimientoAsistenciaService}.
 */
@Service
public class AsistenciaMiembroService {

	private final MiembrosParaAsistencia miembros;

	AsistenciaMiembroService(MiembrosParaAsistencia miembros) {
		this.miembros = miembros;
	}

	/** Código del QR (lo genera si el miembro aún no tiene). Exige poder ver la ficha del miembro. */
	@Transactional
	public CodigoAsistenciaResponse codigo(Long miembroId) {
		return aRespuesta(miembros.codigoDe(miembroId));
	}

	/** Código del QR del propio miembro del token (lo genera si aún no tiene). */
	@Transactional
	public CodigoAsistenciaResponse codigoPropio() {
		return aRespuesta(miembros.codigoPropio());
	}

	/** Reemplaza el código: el anterior deja de valer al instante. */
	@Transactional
	public CodigoAsistenciaResponse regenerarCodigo(Long miembroId) {
		return aRespuesta(miembros.regenerar(miembroId));
	}

	private static CodigoAsistenciaResponse aRespuesta(CodigoAsistencia codigo) {
		return new CodigoAsistenciaResponse(codigo.valor(), codigo.contenidoQr());
	}

}
