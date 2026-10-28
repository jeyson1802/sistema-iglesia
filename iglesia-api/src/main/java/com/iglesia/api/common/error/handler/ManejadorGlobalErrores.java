package com.iglesia.api.common.error.handler;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.iglesia.api.common.error.component.FabricaProblemDetail;
import com.iglesia.api.common.error.exception.ConflictoException;
import com.iglesia.api.common.error.exception.RecursoNoEncontradoException;
import com.iglesia.api.common.error.exception.ReglaNegocioException;
import com.iglesia.api.common.seguridad.exception.CambioContrasenaPendienteException;

/**
 * Traduce las excepciones a respuestas {@link ProblemDetail} (RFC 9457) con mensajes en español y {@code traceId}.
 * Es el único lugar que registra los errores: {@code WARN} sin stack trace para los 4xx y {@code ERROR} con la
 * excepción completa para los 5xx. Las capas inferiores no registran y relanzan.
 */
@RestControllerAdvice
class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

	private static final String PROPIEDAD_ERRORES = "errores";

	private final FabricaProblemDetail fabrica;

	ManejadorGlobalErrores(FabricaProblemDetail fabrica) {
		this.fabrica = fabrica;
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail noEncontrado(RecursoNoEncontradoException ex, HttpServletRequest peticion) {
		return registrar(peticion, fabrica.crear(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage()), ex);
	}

	@ExceptionHandler(ConflictoException.class)
	ProblemDetail conflicto(ConflictoException ex, HttpServletRequest peticion) {
		return registrar(peticion, fabrica.crear(HttpStatus.CONFLICT, "Conflicto", ex.getMessage()), ex);
	}

	@ExceptionHandler(ReglaNegocioException.class)
	ProblemDetail reglaNegocio(ReglaNegocioException ex, HttpServletRequest peticion) {
		return registrar(peticion,
				fabrica.crear(HttpStatus.BAD_REQUEST, "Regla de negocio incumplida", ex.getMessage()), ex);
	}

	@ExceptionHandler(PropertyReferenceException.class)
	ProblemDetail ordenInvalido(PropertyReferenceException ex, HttpServletRequest peticion) {
		return registrar(peticion,
				fabrica.crear(HttpStatus.BAD_REQUEST, "Parámetro inválido", "El campo de ordenación no existe"), ex);
	}

	/** También atiende los 401 de la cadena de seguridad, que llegan aquí desde {@link EntryPointProblemDetail}. */
	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail noAutenticado(AuthenticationException ex, HttpServletRequest peticion) {
		return registrar(peticion, fabrica.noAutenticado(), ex);
	}

	/** También atiende los 403 de la cadena de seguridad, que llegan aquí desde {@link AccessDeniedProblemDetail}. */
	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail accesoDenegado(AccessDeniedException ex, HttpServletRequest peticion) {
		return registrar(peticion, fabrica.accesoDenegado(), ex);
	}

	/** 403 con el motivo explícito, para que el usuario sepa cómo desbloquearse. */
	@ExceptionHandler(CambioContrasenaPendienteException.class)
	ProblemDetail cambioContrasenaPendiente(CambioContrasenaPendienteException ex, HttpServletRequest peticion) {
		return registrar(peticion, fabrica.crear(HttpStatus.FORBIDDEN, FabricaProblemDetail.TITULO_ACCESO_DENEGADO,
				ex.getMessage()), ex);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail errorInesperado(Exception ex, HttpServletRequest peticion) {
		return registrar(peticion, fabrica.errorInterno(), ex);
	}

	@Override
	protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = fabrica.crear(HttpStatus.BAD_REQUEST, "Datos inválidos",
				"La petición contiene campos inválidos");
		Map<String, String> errores = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.collect(Collectors.toMap(FieldError::getField, ManejadorGlobalErrores::mensaje, (a, b) -> a));
		problema.setProperty(PROPIEDAD_ERRORES, errores);
		return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
	}

	/** Punto común de los errores propios de Spring MVC: se les agrega el traceId y se registran. */
	@Override
	protected @Nullable ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body,
			HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> respuesta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (respuesta != null && respuesta.getBody() instanceof ProblemDetail problema
				&& request instanceof ServletWebRequest peticionServlet) {
			fabrica.agregarTraceId(problema);
			registrar(peticionServlet.getRequest(), problema, ex);
		}
		return respuesta;
	}

	private static String mensaje(FieldError error) {
		return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido";
	}

	private static ProblemDetail registrar(HttpServletRequest peticion, ProblemDetail problema, Exception ex) {
		if (HttpStatusCode.valueOf(problema.getStatus()).is5xxServerError()) {
			LOG.error("Error no controlado en {} {}", peticion.getMethod(), peticion.getRequestURI(), ex);
		}
		else {
			LOG.warn("{} {} {}: {}", problema.getStatus(), peticion.getMethod(), peticion.getRequestURI(),
					problema.getDetail());
		}
		return problema;
	}

}
