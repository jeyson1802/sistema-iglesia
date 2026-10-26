package com.iglesia.api.common.traza.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.iglesia.api.common.traza.component.ProveedorTraceId;
import com.iglesia.api.common.traza.constant.TrazaConstantes;

/**
 * Agrega la cabecera X-Trace-Id a toda respuesta. Se escribe antes de continuar la cadena para que también
 * salga en las respuestas que corta Spring Security (401/403) o que fallan más adelante.
 */
@Component
@Order(FiltroCabeceraTraza.ORDEN)
class FiltroCabeceraTraza extends OncePerRequestFilter {

	/** Después del filtro de observación HTTP (que abre la traza, HIGHEST_PRECEDENCE + 1) y antes de Spring Security. */
	static final int ORDEN = Ordered.HIGHEST_PRECEDENCE + 2;

	private final ProveedorTraceId proveedorTraceId;

	FiltroCabeceraTraza(ProveedorTraceId proveedorTraceId) {
		this.proveedorTraceId = proveedorTraceId;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
			throws ServletException, IOException {
		proveedorTraceId.traceIdActual()
			.ifPresent(traceId -> respuesta.setHeader(TrazaConstantes.CABECERA_TRACE_ID, traceId));
		cadena.doFilter(peticion, respuesta);
	}

}
