package com.iglesia.api.common.error.component;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import com.iglesia.api.common.traza.component.ProveedorTraceId;
import com.iglesia.api.common.traza.constant.TrazaConstantes;

/**
 * Crea las respuestas de error {@link ProblemDetail} (RFC 9457) en español y con el {@code traceId} de la
 * petición, para que el usuario o soporte puedan rastrear el error en los logs.
 */
@Component
public class FabricaProblemDetail {

	public static final String TITULO_NO_AUTENTICADO = "No autenticado";

	/** Mensaje genérico a propósito: no revelar por qué se rechazó el token. */
	public static final String DETALLE_NO_AUTENTICADO = "Sesión inválida o expirada";

	public static final String TITULO_ACCESO_DENEGADO = "Acceso denegado";

	public static final String DETALLE_ACCESO_DENEGADO = "No tiene permisos para realizar esta operación";

	public static final String TITULO_ERROR_INTERNO = "Error interno";

	/** Nunca se devuelve el mensaje de la excepción: puede contener detalles internos. */
	public static final String DETALLE_ERROR_INTERNO = "Ocurrió un error inesperado. Comunica el código de seguimiento a soporte.";

	private final ProveedorTraceId proveedorTraceId;

	public FabricaProblemDetail(ProveedorTraceId proveedorTraceId) {
		this.proveedorTraceId = proveedorTraceId;
	}

	public ProblemDetail crear(HttpStatusCode estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		agregarTraceId(problema);
		return problema;
	}

	public ProblemDetail noAutenticado() {
		return crear(HttpStatus.UNAUTHORIZED, TITULO_NO_AUTENTICADO, DETALLE_NO_AUTENTICADO);
	}

	public ProblemDetail accesoDenegado() {
		return crear(HttpStatus.FORBIDDEN, TITULO_ACCESO_DENEGADO, DETALLE_ACCESO_DENEGADO);
	}

	public ProblemDetail errorInterno() {
		return crear(HttpStatus.INTERNAL_SERVER_ERROR, TITULO_ERROR_INTERNO, DETALLE_ERROR_INTERNO);
	}

	/** Para los ProblemDetail que arma Spring MVC (404 de ruta inexistente, 405, cuerpo ilegible…). */
	public void agregarTraceId(ProblemDetail problema) {
		proveedorTraceId.traceIdActual()
			.ifPresent(traceId -> problema.setProperty(TrazaConstantes.PROPIEDAD_TRACE_ID, traceId));
	}

}
