package com.iglesia.api.common.estadisticas.constant;

/** Textos comunes de la documentación OpenAPI de los endpoints de estadísticas. */
public final class DocumentacionEstadisticas {

	public static final String TAG = "Estadísticas";

	public static final String TAG_DESCRIPCION = """
			Cifras de las páginas de inicio por módulo, del periodo elegido y comparadas con el periodo anterior. Cada \
			endpoint exige el permiso de su módulo y calcula el alcance desde el token. Solo cifras: nunca datos \
			personales, y ningún endpoint salvo el de aportes devuelve datos de aportes.""";

	public static final String PERIODO_INVALIDO = "El periodo no es uno de `HOY`, `SEMANA`, `MES`, `MES_ANTERIOR`, "
			+ "`ANIO` o `PERSONALIZADO`, o el rango personalizado no es válido: faltan `desde` o `hasta`, `hasta` es "
			+ "anterior a `desde` o posterior a hoy, o el rango supera 366 días. Se devuelve como `ProblemDetail`.";

	public static final String SIN_ALCANCE = " También si el usuario no tiene alcance de estadísticas: fuera de una "
			+ "iglesia, o sin ser administrador ni líder vinculado a un miembro.";

	public static final String ALCANCE = """
			Alcance según el token (por rol base): `ADMIN_IGLESIA`, o `ADMIN_SISTEMA` dentro de una iglesia, ve toda \
			la iglesia; un `LIDER` vinculado a un miembro, solo su red (las personas que lidera directa e \
			indirectamente). Solo devuelve cifras, nunca datos personales.""";

	private DocumentacionEstadisticas() {
	}

}
