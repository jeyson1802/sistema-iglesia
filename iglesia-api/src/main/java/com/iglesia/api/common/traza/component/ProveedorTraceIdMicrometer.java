package com.iglesia.api.common.traza.component;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

/**
 * Obtiene el traceId del span actual de Micrometer Tracing. Sin tracer configurado (por ejemplo, en pruebas
 * que no activan el tracing) no hay traza y el resultado es vacío.
 */
@Component
class ProveedorTraceIdMicrometer implements ProveedorTraceId {

	private final ObjectProvider<Tracer> tracer;

	ProveedorTraceIdMicrometer(ObjectProvider<Tracer> tracer) {
		this.tracer = tracer;
	}

	@Override
	public Optional<String> traceIdActual() {
		return Optional.ofNullable(tracer.getIfAvailable())
			.map(Tracer::currentSpan)
			.map(Span::context)
			.map(TraceContext::traceId)
			.filter(id -> !id.isBlank());
	}

}
