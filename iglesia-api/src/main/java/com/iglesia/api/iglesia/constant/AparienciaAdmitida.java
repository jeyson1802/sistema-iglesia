package com.iglesia.api.iglesia.constant;

import java.util.Set;

/**
 * Valores de apariencia que puede elegir una iglesia: los mismos presets, paletas y superficies del configurador de
 * temas de PrimeNG (Sakai) que usa el frontend, más las tipografías del nombre comercial que el frontend sabe cargar.
 */
public final class AparienciaAdmitida {

	public static final String PRESET_POR_DEFECTO = "Aura";

	public static final String COLOR_PRIMARIO_POR_DEFECTO = "emerald";

	public static final String TIPOGRAFIA_POR_DEFECTO = "Playfair Display";

	public static final Set<String> PRESETS = Set.of("Aura", "Lara", "Nora");

	/** {@code noir} usa la superficie como color primario. */
	public static final Set<String> COLORES_PRIMARIOS = Set.of("noir", "emerald", "green", "lime", "orange", "amber",
			"yellow", "teal", "cyan", "sky", "blue", "indigo", "violet", "purple", "fuchsia", "pink", "rose");

	public static final Set<String> SUPERFICIES = Set.of("slate", "gray", "zinc", "neutral", "stone", "soho", "viva",
			"ocean");

	public static final Set<String> TIPOGRAFIAS = Set.of("Playfair Display", "Cinzel", "Montserrat", "Merriweather",
			"Poppins");

	private AparienciaAdmitida() {
	}

}
