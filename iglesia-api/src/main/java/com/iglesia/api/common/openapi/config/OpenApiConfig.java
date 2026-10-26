package com.iglesia.api.common.openapi.config;

import java.util.List;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.iglesia.api.common.traza.constant.TrazaConstantes;

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
			.info(new Info().title("Sistema Iglesia – API de negocio")
				.version(VERSION_API)
				.description("""
						Gestión de la iglesia: miembros, jerarquía de liderazgo, niveles y grupos configurables, \
						y los demás módulos que se irán agregando.

						- **Autenticación:** todos los endpoints exigen el token de acceso que emite el servicio \
						iglesia-seguridad (`Authorization: Bearer <token>`). En Swagger UI, botón «Authorize».
						- **Multi-iglesia:** la iglesia se toma del claim `iglesia_id` del token, nunca de un \
						parámetro; cada iglesia solo ve sus propios datos.
						- **Errores:** todos se devuelven como `ProblemDetail` (RFC 9457) en español, con la \
						propiedad `traceId`. La misma traza llega en la cabecera `X-Trace-Id` de toda respuesta; \
						comunícala a soporte para rastrear un error."""))
			.components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD,
					new SecurityScheme().type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description(
								"Token de acceso emitido por iglesia-seguridad (`POST /api/auth/login` o `POST /api/auth/refresh`).")))
			.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD));
	}

	/** Documenta en todas las respuestas la cabecera X-Trace-Id que agrega el filtro de traza. */
	@Bean
	OpenApiCustomizer cabeceraTraceIdEnTodasLasRespuestas() {
		Header cabecera = new Header()
			.description("Código de seguimiento de la petición. Comunicarlo a soporte para rastrear un error.")
			.schema(new StringSchema().example("4bf92f3577b34da6a3ce929d0e0e4736"));
		return especificacion -> {
			if (especificacion.getPaths() == null) {
				return;
			}
			especificacion.getPaths()
				.values()
				.stream()
				.map(PathItem::readOperations)
				.flatMap(List::stream)
				.flatMap(operacion -> operacion.getResponses().values().stream())
				.forEach(respuesta -> respuesta.addHeaderObject(TrazaConstantes.CABECERA_TRACE_ID, cabecera));
		};
	}

}
