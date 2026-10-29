package com.iglesia.seguridad.auth.model;

import com.iglesia.seguridad.usuario.entity.Usuario;

/**
 * Resultado de consumir un token de refresco válido.
 *
 * @param usuario           dueño del token
 * @param iglesiaActuandoId iglesia en la que operaba el administrador del sistema; {@code null} = vista global
 */
public record RefrescoConsumido(Usuario usuario, Long iglesiaActuandoId) {
}
