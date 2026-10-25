package com.iglesia.seguridad.usuario;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import com.iglesia.seguridad.usuario.dto.ActualizarUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.CambiarContrasenaRequest;
import com.iglesia.seguridad.usuario.dto.CrearUsuarioRequest;
import com.iglesia.seguridad.usuario.dto.UsuarioResponse;

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController {

	private static final String SOLO_ADMINISTRADORES = "hasAnyRole('ADMIN_SISTEMA', 'ADMIN_IGLESIA')";

	private final UsuarioService servicio;

	UsuarioController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	@PostMapping
	@PreAuthorize(SOLO_ADMINISTRADORES)
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
	Page<UsuarioResponse> listar(@RequestParam(required = false) Long iglesiaId,
			@PageableDefault(sort = "id") Pageable paginacion) {
		return servicio.listar(iglesiaId, paginacion);
	}

	@GetMapping("/{id}")
	@PreAuthorize(SOLO_ADMINISTRADORES)
	UsuarioResponse obtener(@PathVariable Long id) {
		return servicio.obtener(id);
	}

	@PutMapping("/{id}")
	@PreAuthorize(SOLO_ADMINISTRADORES)
	UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@GetMapping("/me")
	UsuarioResponse obtenerActual() {
		return servicio.obtenerActual();
	}

	@PutMapping("/me/contrasena")
	ResponseEntity<Void> cambiarContrasena(@Valid @RequestBody CambiarContrasenaRequest solicitud) {
		servicio.cambiarContrasena(solicitud);
		return ResponseEntity.noContent().build();
	}

}
