package com.iglesia.seguridad.common.openapi.config;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;

import com.iglesia.seguridad.TestcontainersConfiguration;
import com.iglesia.seguridad.common.traza.constant.TrazaConstantes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Recorre la especificación generada y falla si algo quedó sin documentar: cada operación con resumen,
 * descripción y respuestas descritas, y cada DTO propio (paquetes {@code dto}) con descripción en la clase
 * y en cada campo. Los DTOs de parámetros de consulta ({@code @ParameterObject}) no son esquemas: se exige
 * descripción y ejemplo en cada parámetro.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class OpenApiCoberturaIntegrationTest {

	private static final String PAQUETE_BASE = "com.iglesia.seguridad";

	private static final Set<String> METODOS_HTTP = Set.of("get", "post", "put", "patch", "delete");

	private static final Set<String> OPERACIONES_PUBLICAS = Set.of("POST /api/auth/login", "POST /api/auth/refresh",
			"POST /api/auth/logout", "GET /oauth2/jwks");

	private static final int ERROR_HTTP_MINIMO = 400;

	private static final String REFERENCIA_PROBLEM_DETAIL = "#/components/schemas/ProblemDetail";

	@Autowired
	private MockMvc mvc;

	private String especificacion;

	@BeforeEach
	void obtenerEspecificacion() throws Exception {
		especificacion = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
	}

	@Test
	void todaOperacionTieneResumenDescripcionYRespuestasDescritas() {
		List<String> faltantes = new ArrayList<>();
		operaciones().forEach((nombre, operacion) -> {
			if (vacio(operacion.get("summary"))) {
				faltantes.add(nombre + ": sin summary");
			}
			if (vacio(operacion.get("description"))) {
				faltantes.add(nombre + ": sin description");
			}
			if (vacio(operacion.get("tags"))) {
				faltantes.add(nombre + ": sin tag");
			}
			mapa(operacion.get("responses")).forEach((codigo, respuesta) -> {
				if (vacio(mapa(respuesta).get("description"))) {
					faltantes.add(nombre + " " + codigo + ": respuesta sin description");
				}
				if (!mapa(mapa(respuesta).get("headers")).containsKey(TrazaConstantes.CABECERA_TRACE_ID)) {
					faltantes.add(nombre + " " + codigo + ": sin cabecera " + TrazaConstantes.CABECERA_TRACE_ID);
				}
			});
		});

		assertThat(operaciones()).isNotEmpty();
		assertThat(faltantes).isEmpty();
	}

	@Test
	void soloLasOperacionesPublicasNoExigenToken() {
		Map<String, Map<String, Object>> operaciones = operaciones();

		assertThat(operaciones.keySet()).containsAll(OPERACIONES_PUBLICAS);
		operaciones.forEach((nombre, operacion) -> {
			boolean publica = operacion.containsKey("security") && ((List<?>) operacion.get("security")).isEmpty();
			assertThat(publica).as("¿%s es pública?", nombre).isEqualTo(OPERACIONES_PUBLICAS.contains(nombre));
		});
	}

	@Test
	void todoDtoPropioTieneDescripcionEnLaClaseYEnCadaCampo() {
		Map<String, Object> esquemas = JsonPath.read(especificacion, "$.components.schemas");
		Map<String, Class<?>> dtos = dtosPropios();
		Map<String, Map<String, Object>> parametrosConsulta = parametrosDeConsulta();
		List<String> faltantes = new ArrayList<>();
		dtos.forEach((dto, clase) -> {
			Map<String, Object> esquema = mapa(esquemas.get(dto));
			if (esquema.isEmpty() && esParametroDeConsulta(clase, parametrosConsulta)) {
				revisarParametros(dto, clase, parametrosConsulta, faltantes);
				return;
			}
			if (esquema.isEmpty()) {
				faltantes.add(dto + ": no aparece en la especificación");
				return;
			}
			if (vacio(esquema.get("description"))) {
				faltantes.add(dto + ": sin description");
			}
			mapa(esquema.get("properties")).forEach((campo, propiedad) -> {
				if (vacio(mapa(propiedad).get("description"))) {
					faltantes.add(dto + "." + campo + ": sin description");
				}
				if (!mapa(propiedad).containsKey("example")) {
					faltantes.add(dto + "." + campo + ": sin example");
				}
			});
		});

		assertThat(dtos).isNotEmpty();
		assertThat(faltantes).isEmpty();
	}

	@Test
	void losErroresSeDocumentanComoProblemDetail() {
		Map<String, Object> problema = JsonPath.read(especificacion, "$.components.schemas.ProblemDetail");
		assertThat(mapa(problema.get("properties"))).containsKeys("title", "status", "detail",
				TrazaConstantes.PROPIEDAD_TRACE_ID);

		List<String> faltantes = new ArrayList<>();
		operaciones().forEach((nombre, operacion) -> mapa(operacion.get("responses")).forEach((codigo, respuesta) -> {
			if (Integer.parseInt(codigo) < ERROR_HTTP_MINIMO) {
				return;
			}
			Object esquema = mapa(mapa(mapa(respuesta).get("content")).get(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
				.get("schema");
			if (!REFERENCIA_PROBLEM_DETAIL.equals(mapa(esquema).get("$ref"))) {
				faltantes.add(nombre + " " + codigo + ": no declara ProblemDetail como application/problem+json");
			}
		}));
		assertThat(faltantes).isEmpty();
	}

	/** Operaciones indexadas como «MÉTODO /ruta». */
	private Map<String, Map<String, Object>> operaciones() {
		Map<String, Object> rutas = JsonPath.read(especificacion, "$.paths");
		return rutas.entrySet()
			.stream()
			.flatMap(ruta -> mapa(ruta.getValue()).entrySet()
				.stream()
				.filter(metodo -> METODOS_HTTP.contains(metodo.getKey()))
				.map(metodo -> Map.entry(metodo.getKey().toUpperCase() + " " + ruta.getKey(), mapa(metodo.getValue()))))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/** Parámetros {@code in: query} de todas las operaciones, indexados por nombre. */
	private Map<String, Map<String, Object>> parametrosDeConsulta() {
		Map<String, Map<String, Object>> parametros = new HashMap<>();
		operaciones().values()
			.forEach(operacion -> ((List<?>) operacion.getOrDefault("parameters", List.of())).stream()
				.map(OpenApiCoberturaIntegrationTest::mapa)
				.filter(parametro -> "query".equals(parametro.get("in")))
				.forEach(parametro -> parametros.putIfAbsent((String) parametro.get("name"), parametro)));
		return parametros;
	}

	/** Un {@code record} cuyos campos están todos publicados como parámetros de consulta. */
	private static boolean esParametroDeConsulta(Class<?> clase, Map<String, Map<String, Object>> parametros) {
		return clase.isRecord() && Arrays.stream(clase.getRecordComponents())
			.allMatch(componente -> parametros.containsKey(componente.getName()));
	}

	private static void revisarParametros(String dto, Class<?> clase, Map<String, Map<String, Object>> parametros,
			List<String> faltantes) {
		for (RecordComponent componente : clase.getRecordComponents()) {
			Map<String, Object> parametro = parametros.get(componente.getName());
			if (vacio(parametro.get("description"))) {
				faltantes.add(dto + "." + componente.getName() + ": parámetro sin description");
			}
			if (!parametro.containsKey("example") && !mapa(parametro.get("schema")).containsKey("example")) {
				faltantes.add(dto + "." + componente.getName() + ": parámetro sin example");
			}
		}
	}

	/** Clases de los paquetes {@code dto} del proyecto, indexadas por nombre simple. */
	private static Map<String, Class<?>> dtosPropios() {
		ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition definicion) {
				return true;
			}
		};
		escaner.addIncludeFilter((lector, fabrica) -> lector.getClassMetadata().getClassName().contains(".dto."));
		return escaner.findCandidateComponents(PAQUETE_BASE)
			.stream()
			.map(definicion -> ClassUtils.resolveClassName(definicion.getBeanClassName(), null))
			.collect(Collectors.toMap(ClassUtils::getShortName, clase -> clase));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> mapa(Object valor) {
		return valor instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
	}

	private static boolean vacio(Object valor) {
		return valor == null || valor.toString().isBlank() || (valor instanceof List<?> lista && lista.isEmpty());
	}

}
