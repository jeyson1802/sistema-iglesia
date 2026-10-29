package com.iglesia.api.miembros.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iglesia.api.common.api.dto.CambiarEstadoRequest;
import com.iglesia.api.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.api.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.api.common.seguridad.constant.Permisos;
import com.iglesia.api.miembros.constant.LimitesMiembro;
import com.iglesia.api.miembros.dto.EslabonCadenaResponse;
import com.iglesia.api.miembros.dto.FiltroMiembros;
import com.iglesia.api.miembros.dto.GuardarMiembroRequest;
import com.iglesia.api.miembros.dto.LideradoResponse;
import com.iglesia.api.miembros.dto.MiembroResponse;
import com.iglesia.api.miembros.dto.MiembroResumenResponse;
import com.iglesia.api.miembros.dto.OpcionMiembroResponse;
import com.iglesia.api.miembros.service.MiembroService;

@RestController
@RequestMapping("/api/miembros")
@Tag(name = "Miembros", description = """
		Registro de los congregantes de la iglesia del token (antiguos, nuevos y recuperados) con su ficha, su nivel, \
		su líder y su grupo. Solo nombres y apellidos son obligatorios.

		**Ámbito de lectura** (se calcula desde el token): `ADMIN_IGLESIA` y el administrador del sistema dentro de \
		una iglesia ven toda la iglesia; un `LIDER` vinculado a un miembro ve a ese miembro, a su red (directos e \
		indirectos) y a sus superiores hasta el pastor; cualquier otro no ve a nadie. Un miembro fuera del ámbito \
		responde 404, como si no existiera.

		Escribir exige `miembros.crear`, `miembros.editar` o `miembros.desactivar`.""")
class MiembroController {

	private static final String DESCRIPCION_ID = "Identificador del miembro.";

	private static final String NO_ENCONTRADO = "El miembro no existe en la iglesia del token o está fuera del "
			+ "ámbito de lectura del usuario.";

	private static final String CONFLICTO_FICHA = "Una referencia no existe en la iglesia o está desactivada "
			+ "(`nivelId`, `liderId`, `grupoId`, `invitadoPorId`, `registradoPorId`), el líder es de la propia red "
			+ "(ciclo) o se le asigna líder al pastor principal. La propiedad `errores` indica el campo.";

	private static final String DATOS_FICHA = "Datos inválidos: un campo no cumple las validaciones, una fecha es "
			+ "futura, el distrito no existe o el miembro se referencia a sí mismo. La propiedad `errores` indica el "
			+ "campo.";

	private final MiembroService servicio;

	MiembroController(MiembroService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_MIEMBROS)
	@Operation(summary = "Listar miembros", description = """
			Lista paginada de los miembros del ámbito del usuario, ordenada por apellidos y nombres por defecto \
			(máximo 100 por página). Requiere `miembros.ver`. Los filtros son opcionales y se combinan.""")
	@ApiResponse(responseCode = "200", description = "Página de miembros.")
	@ApiResponse(responseCode = "400", description = "Parámetro inválido, por ejemplo una búsqueda de más de 100 caracteres, un `ubigeo` que no tiene 2, 4 o 6 dígitos o un campo de ordenación que no existe.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<MiembroResumenResponse> listar(@Valid @ParameterObject FiltroMiembros filtro,
			@ParameterObject @PageableDefault(sort = { "apellidos", "nombres" }) Pageable paginacion) {
		return servicio.listar(filtro, paginacion);
	}

	@GetMapping("/opciones")
	@PreAuthorize(Permisos.PUEDE_VER_MIEMBROS)
	@Operation(summary = "Sugerir miembros para un selector", description = """
			Hasta 20 miembros **activos** del ámbito del usuario cuyo nombre contiene el texto, ordenados por \
			apellidos. Lo usa el selector de miembro (líder, invitador, anfitrión...). Requiere `miembros.ver`.""")
	@ApiResponse(responseCode = "200", description = "Miembros sugeridos.")
	@ApiResponse(responseCode = "400", description = "El texto de búsqueda supera los 100 caracteres.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<OpcionMiembroResponse> opciones(
			@Parameter(description = "Parte de los nombres y apellidos. Sin valor devuelve los primeros 20.",
					example = "ana") @RequestParam(required = false) @Size(max = LimitesMiembro.BUSQUEDA_MAX) String q) {
		return servicio.opciones(q);
	}

	@GetMapping("/posibles-duplicados")
	@PreAuthorize(Permisos.PUEDE_CREAR_MIEMBROS)
	@Operation(summary = "Buscar posibles duplicados", description = """
			Miembros de la iglesia (activos o no) con exactamente los mismos nombres y apellidos, sin distinguir \
			mayúsculas ni espacios sobrantes. Se consulta antes de registrar para avisar y pedir confirmación; no \
			impide el registro. Requiere `miembros.crear`.""")
	@ApiResponse(responseCode = "200", description = "Coincidencias; vacío si no hay ninguna.")
	@ApiResponse(responseCode = "400", description = "Falta `nombres` o `apellidos`, o superan los 100 caracteres.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<MiembroResumenResponse> posiblesDuplicados(
			@Parameter(description = "Nombres a comparar.", example = "juan") @RequestParam @NotBlank @Size(
					max = LimitesMiembro.NOMBRES_MAX) String nombres,
			@Parameter(description = "Apellidos a comparar.", example = "PÉREZ ") @RequestParam @NotBlank @Size(
					max = LimitesMiembro.APELLIDOS_MAX) String apellidos) {
		return servicio.posiblesDuplicados(nombres, apellidos);
	}

	@GetMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_VER_MIEMBROS)
	@Operation(summary = "Obtener la ficha de un miembro", description = """
			Ficha completa con la edad calculada, la ubicación deducida del distrito, sus referencias (nivel, líder, \
			grupo, invitador y registrador, marcadas `activo: false` si se desactivaron) y a cuántos lidera \
			directamente. Requiere `miembros.ver` y que el miembro esté en el ámbito del usuario.""")
	@ApiResponse(responseCode = "200", description = "Ficha del miembro.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	MiembroResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "25") @PathVariable Long id) {
		return servicio.obtener(id);
	}

	@GetMapping("/{id}/cadena")
	@PreAuthorize(Permisos.PUEDE_VER_MIEMBROS)
	@Operation(summary = "Obtener la cadena de liderazgo", description = """
			Líderes del miembro en orden, desde su líder directo hasta el pastor principal (vacía si no tiene \
			líder). Requiere `miembros.ver` y que el miembro esté en el ámbito del usuario.""")
	@ApiResponse(responseCode = "200", description = "Cadena de liderazgo.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<EslabonCadenaResponse> cadena(
			@Parameter(description = DESCRIPCION_ID, example = "25") @PathVariable Long id) {
		return servicio.cadena(id);
	}

	@GetMapping("/{id}/liderados")
	@PreAuthorize(Permisos.PUEDE_VER_MIEMBROS)
	@Operation(summary = "Listar los liderados directos", description = """
			Miembros activos que lidera directamente, ordenados por apellidos, con su nivel y cuántos lidera cada \
			uno. El árbol de liderazgo de la ficha lo usa para expandir un nodo cada vez. Requiere `miembros.ver` y \
			que el miembro esté en el ámbito del usuario; solo aparecen los liderados que el usuario puede ver.""")
	@ApiResponse(responseCode = "200", description = "Liderados directos (vacía si no lidera a nadie).")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<LideradoResponse> liderados(
			@Parameter(description = DESCRIPCION_ID, example = "25") @PathVariable Long id) {
		return servicio.liderados(id);
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_CREAR_MIEMBROS)
	@Operation(summary = "Registrar un miembro", description = """
			Registra un miembro con los datos que se tengan: solo `nombres` y `apellidos` son obligatorios. \
			Requiere `miembros.crear`. Reglas: fechas no futuras; la fecha de nacimiento y la edad aproximada son \
			independientes (se pueden enviar las dos); \
			las referencias deben ser de la iglesia y estar activas. No impide nombres repetidos: consultar antes \
			`GET /api/miembros/posibles-duplicados`.""")
	@ApiResponse(responseCode = "201", description = "Miembro registrado.", headers = @Header(name = HttpHeaders.LOCATION, description = "URL del miembro registrado."))
	@ApiResponse(responseCode = "400", description = DATOS_FICHA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = CONFLICTO_FICHA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<MiembroResponse> crear(@Valid @RequestBody GuardarMiembroRequest solicitud) {
		MiembroResponse creado = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creado.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_EDITAR_MIEMBROS)
	@Operation(summary = "Editar la ficha de un miembro", description = """
			Reemplaza la ficha completa (los campos que no se envían quedan vacíos). Requiere `miembros.editar`. \
			Mismas reglas que al registrar y, además: el miembro no puede referenciarse a sí mismo (400), el pastor \
			principal no tiene líder y el líder no puede ser de su propia red (409). Las referencias que no cambian \
			se conservan aunque se hayan desactivado. Cambiar el líder mueve con él a toda su red.""")
	@ApiResponse(responseCode = "200", description = "Ficha actualizada.")
	@ApiResponse(responseCode = "400", description = DATOS_FICHA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El miembro no existe en la iglesia del token.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = CONFLICTO_FICHA,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	MiembroResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "25") @PathVariable Long id,
			@Valid @RequestBody GuardarMiembroRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize(Permisos.PUEDE_DESACTIVAR_MIEMBROS)
	@Operation(summary = "Desactivar o reactivar un miembro", description = """
			Un miembro nunca se borra: se desactiva y deja de ofrecerse en los selectores, pero se conserva donde ya \
			estaba asignado. Requiere `miembros.desactivar`. No se desactiva al pastor principal ni a quien lidera \
			miembros activos o un grupo activo (409).""")
	@ApiResponse(responseCode = "200", description = "Miembro con su nuevo estado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El miembro no existe en la iglesia del token.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Es el pastor principal, o lidera miembros activos o un grupo activo.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	MiembroResponse cambiarEstado(@Parameter(description = DESCRIPCION_ID, example = "25") @PathVariable Long id,
			@Valid @RequestBody CambiarEstadoRequest solicitud) {
		return servicio.cambiarEstado(id, solicitud.activo());
	}

	@PutMapping("/{id}/pastor-principal")
	@PreAuthorize(Permisos.PUEDE_EDITAR_MIEMBROS)
	@Operation(summary = "Designar al pastor principal", description = """
			Marca al miembro como el único pastor principal de la iglesia: se le quita la marca al anterior y el \
			nuevo queda sin líder. Requiere `miembros.editar`.""")
	@ApiResponse(responseCode = "200", description = "Ficha del nuevo pastor principal.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = "El miembro no existe en la iglesia del token.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El miembro está desactivado, u otro usuario designó un pastor al mismo tiempo.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemDetailEsquema.class)))
	MiembroResponse designarPastorPrincipal(
			@Parameter(description = DESCRIPCION_ID, example = "25") @PathVariable Long id) {
		return servicio.designarPastorPrincipal(id);
	}

}
