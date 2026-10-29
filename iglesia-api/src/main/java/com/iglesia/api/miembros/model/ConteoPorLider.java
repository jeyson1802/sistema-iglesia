package com.iglesia.api.miembros.model;

/** Cantidad de miembros activos que lidera directamente un miembro. */
public record ConteoPorLider(Long liderId, long cantidad) {
}
