package com.iglesia.seguridad.common.traza;

import java.util.Optional;

/** Da acceso al identificador de traza de la petición en curso sin acoplarse al tracer concreto. */
public interface ProveedorTraceId {

	/** Identificador de traza (32 caracteres hexadecimales) o vacío si no hay una traza activa. */
	Optional<String> traceIdActual();

}
