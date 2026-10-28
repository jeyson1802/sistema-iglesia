package com.iglesia.api.common.seguridad.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.iglesia.api.common.error.handler.AccessDeniedProblemDetail;
import com.iglesia.api.common.error.handler.EntryPointProblemDetail;
import com.iglesia.api.common.openapi.config.OpenApiConfig;
import com.iglesia.api.common.seguridad.component.ConvertidorAutoridadesJwt;
import com.iglesia.api.common.seguridad.filter.FiltroCambioContrasenaPendiente;
import com.iglesia.api.common.traza.constant.TrazaConstantes;
import com.iglesia.api.common.traza.filter.FiltroContextoUsuario;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Resource server sin estado: toda la API exige un JWT válido de iglesia-seguridad.
 * Este servicio nunca gestiona credenciales.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SeguridadHttpConfig {

	private static final String[] RUTAS_SALUD = { "/actuator/health", "/actuator/health/**" };

	private static final List<String> METODOS_CORS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

	private static final List<String> CABECERAS_CORS = List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE);

	@Bean
	SecurityFilterChain cadenaSeguridad(HttpSecurity http, ConvertidorAutoridadesJwt convertidorJwt,
			EntryPointProblemDetail noAutenticado, AccessDeniedProblemDetail accesoDenegado) {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(withDefaults())
			.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(peticiones -> peticiones
				.requestMatchers(HttpMethod.GET, RUTAS_SALUD).permitAll()
				.requestMatchers("/error").permitAll()
				// Solo existen con el perfil local; en producción responden 404
				.requestMatchers(HttpMethod.GET, OpenApiConfig.RUTAS_DOCUMENTACION).permitAll()
				.anyRequest().authenticated())
			// 401 y 403 de la cadena de seguridad como ProblemDetail con traceId, igual que el resto de errores.
			.exceptionHandling(errores -> errores.authenticationEntryPoint(noAutenticado)
				.accessDeniedHandler(accesoDenegado))
			.oauth2ResourceServer(recurso -> recurso.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt))
				.authenticationEntryPoint(noAutenticado)
				.accessDeniedHandler(accesoDenegado))
			// usuarioId e iglesiaId en el MDC en cuanto se valida el JWT, para todos los logs posteriores.
			.addFilterAfter(new FiltroContextoUsuario(), BearerTokenAuthenticationFilter.class)
			// Con una contraseña temporal, toda la API responde 403 hasta que se cambie en iglesia-seguridad.
			.addFilterAfter(new FiltroCambioContrasenaPendiente(accesoDenegado), FiltroContextoUsuario.class)
			.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties propiedades) {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(propiedades.origenesPermitidos());
		configuracion.setAllowedMethods(METODOS_CORS);
		configuracion.setAllowedHeaders(CABECERAS_CORS);
		configuracion.setExposedHeaders(List.of(HttpHeaders.LOCATION, TrazaConstantes.CABECERA_TRACE_ID));
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}

}
