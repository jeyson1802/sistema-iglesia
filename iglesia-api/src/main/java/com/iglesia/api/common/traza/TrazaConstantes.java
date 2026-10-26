package com.iglesia.api.common.traza;

/** Nombres compartidos de la trazabilidad: cabecera de respuesta, propiedad del ProblemDetail y claves del MDC. */
public final class TrazaConstantes {

	/** Cabecera de respuesta con el identificador de traza de la petición. */
	public static final String CABECERA_TRACE_ID = "X-Trace-Id";

	/** Propiedad del ProblemDetail con el identificador de traza. */
	public static final String PROPIEDAD_TRACE_ID = "traceId";

	public static final String MDC_USUARIO_ID = "usuarioId";

	public static final String MDC_IGLESIA_ID = "iglesiaId";

	private TrazaConstantes() {
	}

}
