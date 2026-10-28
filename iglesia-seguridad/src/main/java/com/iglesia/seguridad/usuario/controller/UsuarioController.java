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
import com.iglesia.seguridad.common.seguridad.constant.Permisos;
import com.iglesia.seguridad.usuario.dto.ActualizarUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.CambiarContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.CrearUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.FiltroUsuarios;
import com.iglesia.seguridad.usuario.dto.MiUsuarioResponse;
import com.iglesia.seguridad.usuario.dto.RestablecerContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.UsuarioResponse;
import com.iglesia.seguridad.usuario.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = """
		Administración de los usuarios que acceden al sistema y gestión de la cuenta propia. Consultar exige el \
		permiso `usuarios.ver` y crear o editar, `usuarios.administrar`. `ADMIN_SISTEMA` actúa sobre usuarios de \
		todas las iglesias; el resto, solo sobre los de su iglesia y sin poder otorgar `ADMIN_SISTEMA`. Los \
		usuarios fuera de su alcance se responden como inexistentes (404).""")
class UsuarioController {

	private static final String DESCRIPCION_ID = "Identificador del usuario.";

	private static final String NO_ENCONTRADO = "El usuario no existe o está fuera del alcance del administrador.";

	private final UsuarioService servicio;

	UsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	@PostMapping
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_USUARIOS)
	@Operation(summary = "Crear un usuario", description = """
			Crea un usuario con sus roles (`rolIds`). Requiere el permiso `usuarios.administrar`.

			Reglas: el correo es único en todo el sistema; cada rol es un rol base o un rol personalizado activo \
			de la iglesia del usuario; `ADMIN_SISTEMA` no pertenece a ninguna iglesia y el resto de roles sí; \
			`LIDER`, `MIEMBRO` y los roles que heredan de ellos exigen un miembro vinculado, y un miembro solo \
			puede tener un usuario. Quien no es `ADMIN_SISTEMA` solo crea usuarios de su iglesia y nunca le \
			otorga `ADMIN_SISTEMA`.""")
	@ApiResponse(responseCode = "201", description = "Usuario creado.",
			headers = @Header(name = HttpHeaders.LOCATION, description = "URL del usuario creado."))
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS
			+ " También si algún rol no existe, es de otra iglesia o está inactivo, o si los roles no son "
			+ "coherentes con la iglesia o el miembro indicados.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO
			+ " También si quien no es `ADMIN_SISTEMA` intenta crear en otra iglesia u otorgar `ADMIN_SISTEMA`.",
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
	@PreAuthorize(Permisos.PUEDE_VER_USUARIOS)
	@Operation(summary = "Listar usuarios", description = """
			Lista paginada de usuarios, ordenada por id por defecto (máximo 100 por página). Requiere el \
			permiso `usuarios.ver`.

			`ADMIN_SISTEMA` puede filtrar por `iglesiaId` (sin filtro ve todas las iglesias). El resto siempre \
			ve solo su iglesia, aunque envíe otro `iglesiaId`.

			Los filtros `correo` (contiene, sin distinguir mayúsculas), `activo` y `rolId` son opcionales y se \
			combinan entre sí y con la paginación.""")
	@ApiResponse(responseCode = "200", description = "Página de usuarios.")
	@ApiResponse(responseCode = "400", description = "Parámetro inválido, por ejemplo un campo de ordenación que no "
			+ "existe o un `correo` de más de 254 caracteres.",
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
			@Valid @ParameterObject FiltroUsuarios filtro,
			@ParameterObject @PageableDefault(sort = "id") Pageable paginacion) {
		return servicio.listar(iglesiaId, filtro, paginacion);
	}

	@GetMapping("/{id}")
	@PreAuthorize(Permisos.PUEDE_VER_USUARIOS)
	@Operation(summary = "Obtener un usuario", description = """
			Datos de un usuario con sus roles. Requiere el permiso `usuarios.ver`; quien no es `ADMIN_SISTEMA` \
			solo ve los de su iglesia.""")
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
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_USUARIOS)
	@Operation(summary = "Actualizar un usuario", description = """
			Cambia el correo, los roles (`rolIds`), el miembro vinculado y el estado de un usuario. Requiere el \
			permiso `usuarios.administrar`; nadie puede modificar su propio usuario por esta vía.

			El correo se normaliza (sin espacios y en minúsculas) y debe ser único en todo el sistema. Si algo \
			cambia, se cierran todas las sesiones del usuario para que su próximo token refleje los cambios (o \
			para impedirle el acceso si se desactivó); si cambió el correo, debe entrar con el nuevo. Mismas \
			reglas de roles que al crear.""")
	@ApiResponse(responseCode = "200", description = "Usuario actualizado.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS
			+ " También si se intenta modificar el usuario propio, algún rol no existe, es de otra iglesia o está "
			+ "inactivo, o los roles no son coherentes con el miembro indicado.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO
			+ " También si quien no es `ADMIN_SISTEMA` intenta otorgar `ADMIN_SISTEMA`.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "409", description = "El correo es de otro usuario o el miembro ya tiene otro usuario.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	UsuarioResponse actualizar(@Parameter(description = DESCRIPCION_ID, example = "12") @PathVariable Long id,
			@Valid @RequestBody ActualizarUsuarioRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PutMapping("/{id}/contrasena")
	@PreAuthorize(Permisos.PUEDE_ADMINISTRAR_USUARIOS)
	@Operation(summary = "Restablecer la contraseña de un usuario", description = """
			Fija una contraseña temporal a otro usuario. Requiere el permiso `usuarios.administrar`; para la \
			contraseña propia se usa `PUT /api/usuarios/me/contrasena`, que pide la actual.

			El usuario queda con `debeCambiarContrasena: true`: tras iniciar sesión con la contraseña temporal, \
			solo puede consultar su usuario y cambiarla. Se cierran todas sus sesiones: los tokens de refresco \
			existentes dejan de servir. Mismas reglas de alcance que al actualizar.""")
	@ApiResponse(responseCode = "204", description = "Contraseña restablecida.")
	@ApiResponse(responseCode = "400", description = RespuestasOpenApi.DATOS_INVALIDOS
			+ " También si se intenta restablecer la contraseña propia.",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "403", description = RespuestasOpenApi.SIN_PERMISO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	@ApiResponse(responseCode = "404", description = NO_ENCONTRADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	ResponseEntity<Void> restablecerContrasena(
			@Parameter(description = DESCRIPCION_ID, example = "12") @PathVariable Long id,
			@Valid @RequestBody RestablecerContrasenaRequest solicitud) {
		servicio.restablecerContrasena(id, solicitud);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	@Operation(summary = "Obtener mi usuario", description = """
			Datos del usuario autenticado con sus perfiles: uno por cada rol activo, con su rol base y sus \
			permisos. Disponible para cualquier usuario autenticado, sin permiso específico; el frontend lo usa \
			tras iniciar sesión para armar el selector de perfil, el menú y los guards.""")
	@ApiResponse(responseCode = "200", description = "Usuario autenticado con sus perfiles.")
	@ApiResponse(responseCode = "401", description = RespuestasOpenApi.NO_AUTENTICADO,
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetailEsquema.class)))
	MiUsuarioResponse obtenerActual() {
		return servicio.obtenerActual();
	}

	@PutMapping("/me/contrasena")
	@Operation(summary = "Cambiar mi contraseña", description = """
			Cambia la contraseña del usuario autenticado tras confirmar la actual. Disponible para cualquier \
			usuario autenticado, también con una contraseña temporal, que deja de serlo. Cierra todas sus \
			sesiones: los tokens de refresco existentes dejan de servir y debe iniciar sesión de nuevo cuando \
			expire el token de acceso.""")
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
