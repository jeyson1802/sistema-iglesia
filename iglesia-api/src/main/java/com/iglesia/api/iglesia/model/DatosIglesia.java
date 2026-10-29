package com.iglesia.api.iglesia.model;

/**
 * Datos de identidad de una iglesia ya validados y normalizados por el servicio. Los opcionales vacíos llegan como
 * {@code null}.
 */
public record DatosIglesia(String nombreComercial, String denominacion, String direccion, String distritoCodigo,
		String telefono, String correo, String sitioWeb) {
}
