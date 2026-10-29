package com.iglesia.api.iglesia.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.ubigeo.dto.UbicacionResponse;

@Schema(description = "Iglesia del catálogo con su identidad y su apariencia.")
public record IglesiaResponse(
		@Schema(description = "Identificador de la iglesia; es el `iglesia_id` de los tokens y de los datos de "
				+ "negocio.", example = "1") Long id,
		@Schema(description = "Nombre comercial (corto).", example = "MMM Lima Centro") String nombreComercial,
		@Schema(description = "Denominación completa.",
				example = "Iglesia Evangélica Pentecostal Movimiento Misionero Mundial en el Perú") String denominacion,
		@Schema(description = "Dirección; nula en iglesias anteriores a la SPEC 08 que aún no la completaron.",
				example = "Av. Los Próceres 123") String direccion,
		@Schema(description = "Código INEI del distrito.", example = "150132") String distritoCodigo,
		@Schema(description = "Departamento, provincia y distrito; nulo si no tiene distrito.") UbicacionResponse ubicacion,
		@Schema(description = "Teléfono de contacto.", example = "+51 1 456 7890") String telefono,
		@Schema(description = "Correo de contacto.", example = "contacto@mmmlimacentro.org") String correo,
		@Schema(description = "Sitio web.", example = "https://mmmlimacentro.org") String sitioWeb,
		@Schema(description = "Si la iglesia está activa.", example = "true") boolean activa,
		@Schema(description = "Si tiene logo; se obtiene de `GET …/logo`.", example = "true") boolean tieneLogo,
		@Schema(description = "Cuándo se subió el logo vigente; cambia cada vez que se reemplaza (sirve para "
				+ "refrescar la imagen).", example = "2026-10-29T15:30:00Z", format = "date-time") Instant logoActualizadoEn,
		@Schema(description = "Apariencia de la iglesia.") AparienciaResponse apariencia,
		@Schema(description = "Instante del alta.", example = "2026-10-28T15:30:00Z",
				format = "date-time") Instant creadaEn) {
}
