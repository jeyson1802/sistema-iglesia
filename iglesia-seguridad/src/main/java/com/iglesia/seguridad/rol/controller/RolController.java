package com.iglesia.seguridad.rol.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iglesia.seguridad.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.seguridad.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.seguridad.common.seguridad.constant.Permisos;
import com.iglesia.seguridad.rol.dto.ActualizarRolRequest;
import com.iglesia.seguridad.rol.dto.CambiarEstadoRolRequest;
import com.iglesia.seguridad.rol.dto.CrearRolRequest;
import com.iglesia.seguridad.rol.dto.RolResponse;
import com.iglesia.seguridad.rol.service.RolService;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Roles", description = """
		Roles de acceso de la iglesia. Los 4 roles base (`ADMIN_SISTEMA`, `ADMIN_IGLESIA`, `LIDER` y `MIEMBRO`) son \
		globales y de solo lectura. Cada iglesia crea sus roles personalizados: heredan de un rol base su ámbito y su \
		página de inicio, y sus permisos son un subconjunto de los del rol base. Consultar exige `roles.ver` \
		(listar también se permite con `usuarios.administrar`) y modificar, `roles.administrar`. Los roles personalizados de otra iglesia se responden como inexistentes \
		(404). Los cambios de permisos llegan a los usuarios en el siguiente refresco de su token.""")
class RolController {

	private static final String DESCRIPCION_ID = "Identificador del rol.";

	private static final String NO_ENCONTRADO = "El rol no existe o es un rol personalizado de otra iglesia.";

	private static final String ROL_BASE = " También si el rol es un rol base, que es de solo lectura.";

	private final RolService servicio;

	RolController(RolService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@PreAuthorize(Permisos.PUEDE_VER_ROLES_O_ASIGNARLOS)
	@Operation(summary = "Listar roles", description = """
			Los roles base y los personalizados de la iglesia de quien consulta (activos e inactivos): primero \
			los base y luego los personalizados, por jerarquía de su rol base y por nombre. Requiere el permiso \
			`roles.ver` o `usuarios.administrar` (quien asigna roles a usuarios necesita verlos, aunque no pueda \
			editarlos). `requiereMiembro` indica los roles que solo se asignan a usuarios vinculados con un \
			miembro. `ADMIN_SISTEMA`, que no pertenece a ninguna iglesia, solo ve los roles base.""")
	@ApiResponse(responseCode = "200", description = "Roles visibles para quien consulta.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	List<RolResponse> listar() {
		return servicio.listar();
	}

	@GetMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_VER_ROLES)
	@Operation(summary = "Obtener un rol", description = """
			Datos de un rol con sus permisos y la cantidad de usuarios que lo tienen en la iglesia de quien \
			consulta. Requiere el permiso `roles.ver`.""")
	@ApiResponse(responseCode = "200", description = "Rol encontrado.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	RolResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id) {
		return servicio.obtener(id);
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_ROLES)
	@Operation(summary = "Crear un rol personalizado", description = """
			Crea un rol personalizado activo en la iglesia de quien lo crea. Requiere el permiso \
			`roles.administrar`.

			Reglas: el rol base es `ADMIN_IGLESIA`, `LIDER` o `MIEMBRO` (nunca `ADMIN_SISTEMA` ni otro rol \
			personalizado); los permisos son un subconjunto de los del rol base; el nombre es único en la \
			iglesia sin distinguir mayúsculas (puede repetirse en otra iglesia).""")
	@ApiResponse(responseCode = "201", description = "Rol creado.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL del rol creado."))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS
			+ " También si el rol base no existe o no es heredable, o si algún permiso no lo tiene el rol base.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Ya existe un rol con ese nombre en la iglesia.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<RolResponse> crear(@Valid @RequestBody CrearRolRequest solicitud) {
		RolResponse creado = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creado.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@PutMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_ROLES)
	@Operation(summary = "Actualizar un rol personalizado", description = """
			Cambia el nombre y los permisos de un rol personalizado; el rol base no se modifica. Requiere el \
			permiso `roles.administrar`. Mismas reglas que al crear. Los usuarios del rol reciben los nuevos \
			permisos en el siguiente refresco de su token. La iglesia debe conservar al menos un usuario activo \
			que pueda administrar usuarios.""")
	@ApiResponse(responseCode = "200", description = "Rol actualizado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS
			+ " También si algún permiso no lo tiene el rol base.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO + ROL_BASE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Ya existe otro rol con ese nombre en la iglesia, o quitarle "
			+ "`usuarios.administrar` dejaría la iglesia sin ningún usuario activo que pueda administrar usuarios.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	RolResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@Valid @RequestBody ActualizarRolRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PatchMapping("/{id}/estado")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_ROLES)
	@Operation(summary = "Desactivar o reactivar un rol personalizado", description = """
			Desactiva o reactiva un rol personalizado; los roles no se borran. Requiere el permiso \
			`roles.administrar`.

			Un rol desactivado sigue asignado a sus usuarios, pero no aporta permisos ni aparece en sus \
			perfiles desde el siguiente refresco del token, y no se puede asignar. No se puede desactivar si \
			algún usuario se quedaría sin roles activos, ni si la iglesia se quedaría sin ningún usuario activo \
			que pueda administrar usuarios.""")
	@ApiResponse(responseCode = "200", description = "Estado del rol actualizado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO + ROL_BASE,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "Desactivarlo dejaría a algún usuario sin roles activos, o "
			+ "la iglesia sin ningún usuario activo que pueda administrar usuarios.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	RolResponse cambiarEstado(@Parameter(description = DESCRIPCION_ID, example = "7") @PathVariable Long id,
			@Valid @RequestBody CambiarEstadoRolRequest solicitud) {
		return servicio.cambiarEstado(id, solicitud);
	}

}
