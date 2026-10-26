package com.iglesia.seguridad.usuario.controller;

import java.net.URI;

import jakarta.validation.Valid;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.iglesia.seguridad.common.openapi.constant.RespuestasOpenApi;
import com.iglesia.seguridad.common.openapi.model.ProblemDetailEsquema;
import com.iglesia.seguridad.usuario.dto.ActualizarUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.CambiarContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.CrearUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.UsuarioResponse;
import com.iglesia.seguridad.usuario.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = """
		Administración de los usuarios que acceden al sistema y gestión de la cuenta propia. \
		`ADMIN_SISTEMA` administra usuarios de todas las iglesias; `ADMIN_IGLESIA`, solo los de su iglesia y sin \
		poder otorgar `ADMIN_SISTEMA`. Los usuarios fuera de su alcance se responden como inexistentes (404).""")
class UsuarioController {

	private static final String SOLO_ADMINISTRADORES = "hasAnyRole('ADMIN_SISTEMA', 'ADMIN_IGLESIA')";

	private static final String DESCRIPCION_ID = "Identificador del usuario.";

	private static final String NO_ENCONTRADO = "El usuario no existe o está fuera del alcance del administrador.";

	private final UsuarioService servicio;

	UsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	@PostMapping
	@PreAuthorize(SOLO_ADMINISTRADORES)
	@Operation(summary = "Crear un usuario", description = """
			Crea un usuario con sus roles. Requiere `ADMIN_SISTEMA` o `ADMIN_IGLESIA`.

			Reglas: el correo es único en todo el sistema; `ADMIN_SISTEMA` no pertenece a ninguna iglesia y el \
			resto de roles sí; `LIDER` y `MIEMBRO` exigen un miembro vinculado, y un miembro solo puede tener un \
			usuario. Un `ADMIN_IGLESIA` solo crea usuarios de su iglesia y nunca `ADMIN_SISTEMA`.""")
	@ApiResponse(responseCode = "201", description = "Usuario creado.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL del usuario creado."))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS
			+ " También si los roles no son coherentes con la iglesia o el miembro indicados.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO
			+ " También si un `ADMIN_IGLESIA` intenta crear en otra iglesia u otorgar `ADMIN_SISTEMA`.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El correo ya está registrado o el miembro ya tiene un usuario.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest solicitud) {
		UsuarioResponse creado = servicio.crear(solicitud);
		URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(creado.id())
			.toUri();
		return ResponseEntity.created(ubicacion).body(creado);
	}

	@GetMapping
	@PreAuthorize(SOLO_ADMINISTRADORES)
	@Operation(summary = "Listar usuarios", description = """
			Lista paginada de usuarios, ordenada por id por defecto (máximo 100 por página). Requiere \
			`ADMIN_SISTEMA` o `ADMIN_IGLESIA`.

			`ADMIN_SISTEMA` puede filtrar por `iglesiaId` (sin filtro ve todas las iglesias). `ADMIN_IGLESIA` \
			siempre ve solo su iglesia, aunque envíe otro `iglesiaId`.""")
	@ApiResponse(responseCode = "200", description = "Página de usuarios.")
	@ApiResponse(responseCode = "400", description = "Parámetro inválido, por ejemplo un campo de ordenación que no existe.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	Page<UsuarioResponse> listar(
			@Parameter(description = "Filtra por iglesia. Solo lo tiene en cuenta `ADMIN_SISTEMA`.",
					example = "1") @RequestParam(required = false) Long iglesiaId,
			@ParameterObject @PageableDefault(sort = "id") Pageable paginacion) {
		return servicio.listar(iglesiaId, paginacion);
	}

	@GetMapping("/{id}")
	@PreAuthorize(SOLO_ADMINISTRADORES)
	@Operation(summary = "Obtener un usuario", description = """
			Datos de un usuario. Requiere `ADMIN_SISTEMA` o `ADMIN_IGLESIA`; este último solo ve los de su \
			iglesia.""")
	@ApiResponse(responseCode = "200", description = "Usuario encontrado.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	UsuarioResponse obtener(@Parameter(description = DESCRIPCION_ID, example = "12") @PathVariable Long id) {
		return servicio.obtener(id);
	}

	@PutMapping("/{id}")
	@PreAuthorize(SOLO_ADMINISTRADORES)
	@Operation(summary = "Actualizar un usuario", description = """
			Cambia los roles, el miembro vinculado y el estado de un usuario. Requiere `ADMIN_SISTEMA` o \
			`ADMIN_IGLESIA`; nadie puede modificar sus propios roles ni su estado.

			Si algo cambia, se cierran todas las sesiones del usuario para que su próximo token refleje los \
			cambios (o para impedirle el acceso si se desactivó). Mismas reglas de roles que al crear.""")
	@ApiResponse(responseCode = "200", description = "Usuario actualizado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS
			+ " También si se intenta modificar el usuario propio o los roles no son coherentes.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO
			+ " También si un `ADMIN_IGLESIA` intenta otorgar `ADMIN_SISTEMA`.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El miembro ya tiene otro usuario.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	UsuarioResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "12") @PathVariable Long id,
			@Valid @RequestBody ActualizarUsuarioRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@GetMapping("/me")
	@Operation(summary = "Obtener mi usuario", description = """
			Datos del usuario autenticado. Disponible para cualquier rol; el frontend lo usa para conocer los \
			roles y la iglesia del usuario tras iniciar sesión.""")
	@ApiResponse(responseCode = "200", description = "Usuario autenticado.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	UsuarioResponse obtenerActual() {
		return servicio.obtenerActual();
	}

	@PutMapping("/me/contrasena")
	@Operation(summary = "Cambiar mi contraseña", description = """
			Cambia la contraseña del usuario autenticado tras confirmar la actual. Disponible para cualquier rol. \
			Cierra todas sus sesiones: los tokens de refresco existentes dejan de servir y debe iniciar sesión \
			de nuevo cuando expire el token de acceso.""")
	@ApiResponse(responseCode = "204", description = "Contraseña cambiada.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS
			+ " También si la contraseña actual no es correcta.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest solicitud) {
		servicio.cambiarContrasena(solicitud);
		return ResponseEntity.noContent().build();
	}

}
