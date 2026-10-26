package com.iglesia.seguridad.usuario.dto;

import java.time.Instant;
import java.util.Set;

import com.iglesia.seguridad.usuario.model.Rol;

public record UsuarioResponse(Long id, String correo, Long iglesiaId, Long miembroId, boolean activo, Set<Rol> roles,
		Instant creadoEn) {
}
