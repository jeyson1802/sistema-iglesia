package com.iglesia.api.iglesia.model;

/** Apariencia de una iglesia ya validada: preset, color primario, superficie (nula = la del preset) y tipografía. */
public record Apariencia(String preset, String colorPrimario, String colorSuperficie, String tipografia) {
}
