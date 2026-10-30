package com.iglesia.api.consolidacion.model;

/** Cantidad agrupada por miembro (casos abiertos de un consolidador, contactos recientes…). */
public record ConteoPorMiembro(Long miembroId, long cantidad) {
}
