package com.iglesia.seguridad.common.error;

import java.util.Map;
import java.util.stream.Collectors;

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
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Traduce las excepciones a respuestas {@link ProblemDetail} (RFC 9457) con mensajes en español. */
@RestControllerAdvice
class ManejadorGlobalErrores extends ResponseEntityExceptionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalErrores.class);

	private static final String PROPIEDAD_ERRORES = "errores";

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail noEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
	}

	@ExceptionHandler(ConflictoException.class)
	ProblemDetail conflicto(ConflictoException ex) {
		return problema(HttpStatus.CONFLICT, "Conflicto", ex.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	ProblemDetail reglaNegocio(ReglaNegocioException ex) {
		return problema(HttpStatus.BAD_REQUEST, "Regla de negocio incumplida", ex.getMessage());
	}

	@ExceptionHandler(PropertyReferenceException.class)
	ProblemDetail ordenInvalido(PropertyReferenceException ex) {
		return problema(HttpStatus.BAD_REQUEST, "Parámetro inválido", "El campo de ordenación no existe");
	}

	@ExceptionHandler(AuthenticationException.class)
	ProblemDetail noAutenticado(AuthenticationException ex) {
		// Mensaje genérico a propósito: no revelar si el correo existe o si la cuenta está inactiva.
		return problema(HttpStatus.UNAUTHORIZED, "No autenticado", "Credenciales inválidas o sesión expirada");
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail accesoDenegado(AccessDeniedException ex) {
		return problema(HttpStatus.FORBIDDEN, "Acceso denegado", "No tiene permisos para realizar esta operación");
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail errorInesperado(Exception ex) {
		LOG.error("Error no controlado", ex);
		return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", "Ocurrió un error inesperado");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Datos inválidos",
				"La petición contiene campos inválidos");
		Map<String, String> errores = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.collect(Collectors.toMap(FieldError::getField, ManejadorGlobalErrores::mensaje, (a, b) -> a));
		problema.setProperty(PROPIEDAD_ERRORES, errores);
		return ResponseEntity.badRequest().body(problema);
	}

	private static String mensaje(FieldError error) {
		return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido";
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		return problema;
	}

}
