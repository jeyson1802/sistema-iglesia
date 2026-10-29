package com.iglesia.api.iglesia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iglesia.api.iglesia.constant.LimitesIglesia;

@Schema(description = "Datos de identidad de una iglesia, para darla de alta o editarla. Los textos se guardan sin "
		+ "espacios sobrantes; los opcionales vacíos se guardan como nulos.")
public record GuardarIglesiaRequest(
		@Schema(description = "Nombre corto con el que se reconoce la iglesia; va en la topbar, el footer y el título "
				+ "de la pestaña. Único sin distinguir mayúsculas.", example = "MMM Lima Centro") @NotBlank @Size(
						min = LimitesIglesia.NOMBRE_COMERCIAL_MIN,
						max = LimitesIglesia.NOMBRE_COMERCIAL_MAX) String nombreComercial,
		@Schema(description = "Denominación completa (nombre formal) de la iglesia.",
				example = "Iglesia Evangélica Pentecostal Movimiento Misionero Mundial en el Perú") @NotBlank @Size(
						max = LimitesIglesia.DENOMINACION_MAX) String denominacion,
		@Schema(description = "Dirección del local principal.", example = "Av. Los Próceres 123") @NotBlank @Size(
				max = LimitesIglesia.DIRECCION_MAX) String direccion,
		@Schema(description = "Código INEI del distrito; la provincia y el departamento se deducen de él.",
				example = "150132") @NotBlank @Pattern(regexp = LimitesIglesia.DISTRITO_PATRON,
						message = "debe ser un código de distrito de 6 dígitos") String distritoCodigo,
		@Schema(description = "Teléfono de contacto (opcional).", example = "+51 1 456 7890") @Size(
				max = LimitesIglesia.TELEFONO_MAX) @Pattern(regexp = LimitesIglesia.TELEFONO_PATRON,
						message = "solo admite dígitos, espacios, +, - y paréntesis") String telefono,
		@Schema(description = "Correo de contacto (opcional).", example = "contacto@mmmlimacentro.org") @Email @Size(
				max = LimitesIglesia.CORREO_MAX) String correo,
		@Schema(description = "Sitio web (opcional), con http:// o https://.",
				example = "https://mmmlimacentro.org") @Size(max = LimitesIglesia.SITIO_WEB_MAX) @Pattern(
						regexp = LimitesIglesia.SITIO_WEB_PATRON,
						message = "debe empezar con http:// o https://") String sitioWeb) {
}
