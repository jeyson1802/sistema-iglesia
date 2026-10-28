package com.iglesia.seguridad.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.iglesia.seguridad.common.error.handler.AccessDeniedProblemDetail;
import com.iglesia.seguridad.common.error.handler.EntryPointProblemDetail;
import com.iglesia.seguridad.common.openapi.config.OpenApiConfig;
import com.iglesia.seguridad.common.seguridad.filter.FiltroCambioContrasenaPendiente;
import com.iglesia.seguridad.common.traza.constant.TrazaConstantes;
import com.iglesia.seguridad.common.traza.filter.FiltroContextoUsuario;
import com.iglesia.seguridad.jwt.controller.JwksController;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Cadena de seguridad HTTP: API sin estado protegida con los JWT que emite este mismo servicio.
 * Solo el login, el refresco, el cierre de sesión, las claves públicas y la salud son públicos.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SeguridadHttpConfig {

	private static final String[] RUTAS_AUTENTICACION = { "/api/auth/login", "/api/auth/refresh", "/api/auth/logout" };

	private static final String[] RUTAS_SALUD = { "/actuator/health", "/actuator/health/**" };

	private static final List<String> METODOS_CORS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

	private static final List<String> CABECERAS_CORS = List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE);

	@Bean
	SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtAuthenticationConverter convertidorJwt,
			EntryPointProblemDetail noAutenticado, AccessDeniedProblemDetail accesoDenegado) {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(withDefaults())
			.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(peticiones -> peticiones
				.requestMatchers(HttpMethod.POST, RUTAS_AUTENTICACION).permitAll()
				.requestMatchers(HttpMethod.GET, JwksController.RUTA).permitAll()
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
			// Con contraseña temporal solo se permite consultar el propio usuario y cambiarla. Va después del
			// contexto de usuario para que el WARN del 403 lleve usuarioId e iglesiaId.
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
		// Necesario para que el navegador envíe y acepte la cookie del token de refresco entre orígenes.
		configuracion.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", configuracion);
		return fuente;
	}

	/** Codificador delegante: BCrypt por defecto, con prefijo {id} para poder migrar de algoritmo. */
	@Bean
	PasswordEncoder codificadorContrasenas() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	AuthenticationManager gestorAutenticacion(UserDetailsService usuarios, PasswordEncoder codificador) {
		DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(usuarios);
		proveedor.setPasswordEncoder(codificador);
		return new ProviderManager(proveedor);
	}

}
