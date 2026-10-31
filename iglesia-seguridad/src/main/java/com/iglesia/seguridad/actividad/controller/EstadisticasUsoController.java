package com.iglesia.seguridad.actividad.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iglesia.seguridad.actividad.dto.EstadisticasUsoResponse;
import com.iglesia.seguridad.actividad.service.EstadisticasUsoService;
import com.iglesia.seguridad.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.seguridad.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.seguridad.common.periodo.dto.ConsultaPeriodo;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = "Estadísticas de uso", description = """
		Cuánto se usa el sistema, medido por la actividad diaria: un usuario cuenta como activo un día si inició \
		sesión o renovó su sesión ese día. Solo cifras: nunca nombres, correos, IP ni acciones.""")
class EstadisticasUsoController {

	private final EstadisticasUsoService servicio;

	EstadisticasUsoController(EstadisticasUsoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/uso")
	@PreAuthorize(Permisos.PUEDE_VER_USO)
	@Operation(summary = "Uso del sistema en el periodo", description = """
			Usuarios activos (en el periodo, promedio diario, últimos 7 y 30 días), usuarios nuevos, serie por \
			día, semana o mes y activos por rol base, comparados con el periodo anterior de la misma duración.

			Alcance según el token: `ADMIN_SISTEMA` en la vista global, con `estadisticas.sistema`, ve todas las \
			iglesias e incluye las iglesias activas; quien opera dentro de una iglesia, con `usuarios.ver`, ve solo \
			esa iglesia. La iglesia nunca se toma de un parámetro.""")
	@ApiResponse(responseCode = "200", description = "Cifras de uso del periodo.")
	@ApiResponse(responseCode = "400", description = "El periodo no es uno de `HOY`, `SEMANA`, `MES`, "
			+ "`MES_ANTERIOR`, `ANIO` o `PERSONALIZADO`, o el rango personalizado no es válido: faltan `desde` o "
			+ "`hasta`, `hasta` es anterior a `desde` o posterior a hoy, o supera 366 días. Se devuelve como "
			+ "`ProblemDetail`.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO
			+ " También en la vista global sin `estadisticas.sistema`, o dentro de una iglesia sin `usuarios.ver`.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	EstadisticasUsoResponse uso(@ParameterObject ConsultaPeriodo periodo) {
		return servicio.calcular(periodo);
	}

}
