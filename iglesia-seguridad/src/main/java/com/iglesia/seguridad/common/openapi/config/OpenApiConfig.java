package com.iglesia.seguridad.common.openapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Información general de la especificación OpenAPI y esquema de seguridad JWT. Todos los endpoints exigen
 * {@value #ESQUEMA_SEGURIDAD} salvo los públicos, que se marcan con {@code @SecurityRequirements()} vacío.
 * La especificación y Swagger UI solo se publican con el perfil {@code local} (propiedades {@code springdoc.*}).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	/** Nombre del esquema de seguridad que referencian los endpoints protegidos. */
	public static final String ESQUEMA_SEGURIDAD = "bearerAuth";

	/** Rutas de la especificación y de Swagger UI; públicas, aunque solo existen con el perfil local. */
	public static final String[] RUTAS_DOCUMENTACION = { "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html",
			"/swagger-ui/**" };

	private static final String VERSION_API = "v1";

	@Bean
	OpenAPI especificacionOpenApi() {
		return new OpenAPI()
			.info(new Info().title("Sistema Iglesia – Seguridad")
				.version(VERSION_API)
				.description("""
						Autenticación, usuarios y emisión de tokens del Sistema Iglesia.

						- **Inicio de sesión:** `POST /api/auth/login` devuelve el token de acceso (JWT RS256 de \
						corta duración) y deja el token de refresco en una cookie HttpOnly.
						- **Uso del token:** enviarlo en la cabecera `Authorization: Bearer <token>`. En Swagger UI, \
						botón «Authorize».
						- **Errores:** todos se devuelven como `ProblemDetail` (RFC 9457) en español, con la \
						propiedad `traceId`. La misma traza llega en la cabecera `X-Trace-Id` de toda respuesta; \
						comunícala a soporte para rastrear un error.
						- **Claves públicas:** `GET /oauth2/jwks`, para que otros servicios validen los tokens."""))
			.components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD,
					new SecurityScheme().type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description("Token de acceso obtenido en `POST /api/auth/login` o `POST /api/auth/refresh`.")))
			.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD));
	}

}
