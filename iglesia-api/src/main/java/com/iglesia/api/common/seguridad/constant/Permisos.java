package com.iglesia.api.common.seguridad.constant;

/**
 * Códigos de permisos del catálogo de iglesia-seguridad (claim {@code permisos} del JWT) y expresiones de
 * {@code @PreAuthorize} para exigirlos. La autorización se decide siempre por permiso, nunca por rol.
 */
public final class Permisos {

	public static final String IGLESIAS_VER = "iglesias.ver";

	public static final String IGLESIAS_ADMINISTRAR = "iglesias.administrar";

	public static final String IGLESIA_PERSONALIZAR = "iglesia.personalizar";

	public static final String MIEMBROS_VER = "miembros.ver";

	public static final String MIEMBROS_CREAR = "miembros.crear";

	public static final String MIEMBROS_EDITAR = "miembros.editar";

	public static final String MIEMBROS_DESACTIVAR = "miembros.desactivar";

	public static final String NIVELES_ADMINISTRAR = "niveles.administrar";

	public static final String GRUPOS_VER = "grupos.ver";

	public static final String GRUPOS_ADMINISTRAR = "grupos.administrar";

	public static final String APORTES_VER = "aportes.ver";

	public static final String APORTES_REGISTRAR = "aportes.registrar";

	public static final String APORTES_EDITAR = "aportes.editar";

	public static final String APORTES_ANULAR = "aportes.anular";

	public static final String APORTES_CONFIGURAR = "aportes.configurar";

	public static final String ASISTENCIA_REGISTRAR = "asistencia.registrar";

	public static final String ASISTENCIA_GRUPO = "asistencia.grupo";

	public static final String ASISTENCIA_VER = "asistencia.ver";

	public static final String ASISTENCIA_CONFIGURAR = "asistencia.configurar";

	public static final String CONSOLIDACION_ADMINISTRAR = "consolidacion.administrar";

	public static final String CONSOLIDACION_VER = "consolidacion.ver";

	public static final String CONSOLIDACION_CONTACTAR = "consolidacion.contactar";

	/** Solo del rol base {@code ADMIN_SISTEMA}: las cifras de todas las iglesias, en la vista global. */
	public static final String ESTADISTICAS_SISTEMA = "estadisticas.sistema";

	public static final String PUEDE_VER_IGLESIAS = "hasAuthority('" + IGLESIAS_VER + "')";

	public static final String PUEDE_ADMINISTRAR_IGLESIAS = "hasAuthority('" + IGLESIAS_ADMINISTRAR + "')";

	public static final String PUEDE_PERSONALIZAR_IGLESIA = "hasAuthority('" + IGLESIA_PERSONALIZAR + "')";

	public static final String PUEDE_VER_MIEMBROS = "hasAuthority('" + MIEMBROS_VER + "')";

	public static final String PUEDE_CREAR_MIEMBROS = "hasAuthority('" + MIEMBROS_CREAR + "')";

	public static final String PUEDE_EDITAR_MIEMBROS = "hasAuthority('" + MIEMBROS_EDITAR + "')";

	public static final String PUEDE_DESACTIVAR_MIEMBROS = "hasAuthority('" + MIEMBROS_DESACTIVAR + "')";

	/** Los niveles se muestran en la ficha del miembro, así que también los lee quien ve miembros. */
	public static final String PUEDE_VER_NIVELES = "hasAnyAuthority('" + MIEMBROS_VER + "', '" + NIVELES_ADMINISTRAR
			+ "')";

	public static final String PUEDE_ADMINISTRAR_NIVELES = "hasAuthority('" + NIVELES_ADMINISTRAR + "')";

	/** Los grupos se eligen en la ficha del miembro, así que también los lee quien ve miembros. */
	public static final String PUEDE_VER_GRUPOS = "hasAnyAuthority('" + GRUPOS_VER + "', '" + MIEMBROS_VER + "')";

	public static final String PUEDE_ADMINISTRAR_GRUPOS = "hasAuthority('" + GRUPOS_ADMINISTRAR + "')";

	public static final String PUEDE_VER_APORTES = "hasAuthority('" + APORTES_VER + "')";

	public static final String PUEDE_REGISTRAR_APORTES = "hasAuthority('" + APORTES_REGISTRAR + "')";

	public static final String PUEDE_EDITAR_APORTES = "hasAuthority('" + APORTES_EDITAR + "')";

	public static final String PUEDE_ANULAR_APORTES = "hasAuthority('" + APORTES_ANULAR + "')";

	public static final String PUEDE_CONFIGURAR_APORTES = "hasAuthority('" + APORTES_CONFIGURAR + "')";

	/** Quien registra o configura aportes necesita ver las categorías y la moneda para elegirlas. */
	public static final String PUEDE_CONSULTAR_CONFIGURACION_APORTES = "hasAnyAuthority('" + APORTES_VER + "', '"
			+ APORTES_REGISTRAR + "', '" + APORTES_CONFIGURAR + "')";

	public static final String PUEDE_CONFIGURAR_ASISTENCIA = "hasAuthority('" + ASISTENCIA_CONFIGURAR + "')";

	/** Quien toma asistencia en cultos necesita ver los horarios para elegir el culto. */
	public static final String PUEDE_CONSULTAR_CULTOS = "hasAnyAuthority('" + ASISTENCIA_CONFIGURAR + "', '"
			+ ASISTENCIA_REGISTRAR + "')";

	/** Tomar asistencia en todas las reuniones o, el líder, en las de sus grupos (el servicio aplica el ámbito). */
	public static final String PUEDE_TOMAR_ASISTENCIA = "hasAnyAuthority('" + ASISTENCIA_REGISTRAR + "', '"
			+ ASISTENCIA_GRUPO + "')";

	/** Ver el historial de toda la iglesia o, el líder, el de sus grupos (el servicio aplica el ámbito). */
	public static final String PUEDE_VER_HISTORIAL_ASISTENCIA = "hasAnyAuthority('" + ASISTENCIA_VER + "', '"
			+ ASISTENCIA_GRUPO + "')";

	/** Los asistentes de una reunión los ve quien toma asistencia en ella y quien ve el historial. */
	public static final String PUEDE_VER_ASISTENTES = "hasAnyAuthority('" + ASISTENCIA_VER + "', '"
			+ ASISTENCIA_REGISTRAR + "', '" + ASISTENCIA_GRUPO + "')";

	/** La asistencia de un miembro la ve quien ve toda la asistencia o quien ve su ficha (el servicio aplica el ámbito). */
	public static final String PUEDE_VER_ASISTENCIA_DE_MIEMBRO = "hasAnyAuthority('" + ASISTENCIA_VER + "', '"
			+ MIEMBROS_VER + "')";

	/** Seguimiento de asistencia de toda la iglesia o, el líder, de su red (el servicio aplica el ámbito). */
	public static final String PUEDE_VER_SEGUIMIENTO_ASISTENCIA = PUEDE_VER_HISTORIAL_ASISTENCIA;

	/** La evolución de los servicios de la iglesia solo la ve quien ve toda la asistencia. */
	public static final String PUEDE_VER_TODA_LA_ASISTENCIA = "hasAuthority('" + ASISTENCIA_VER + "')";

	/** Las reglas de seguimiento las lee quien ve toda la asistencia o quien la configura. */
	public static final String PUEDE_VER_REGLAS_SEGUIMIENTO = "hasAnyAuthority('" + ASISTENCIA_VER + "', '"
			+ ASISTENCIA_CONFIGURAR + "')";

	/** Coordinador de consolidación: equipo, rondas, reparto y catálogo de resultados. */
	public static final String PUEDE_ADMINISTRAR_CONSOLIDACION = "hasAuthority('" + CONSOLIDACION_ADMINISTRAR + "')";

	/** Rondas y su avance: quien las ve o las coordina. */
	public static final String PUEDE_VER_RONDAS = "hasAnyAuthority('" + CONSOLIDACION_VER + "', '"
			+ CONSOLIDACION_ADMINISTRAR + "')";

	/** «Mis asignados»: el consolidador o el coordinador (que incluye todo lo del consolidador). */
	public static final String PUEDE_CONTACTAR = "hasAnyAuthority('" + CONSOLIDACION_CONTACTAR + "', '"
			+ CONSOLIDACION_ADMINISTRAR + "')";

	/** Casos, historial y resumen de una persona: cualquiera de los tres; el servicio aplica el ámbito. */
	public static final String PUEDE_CONSULTAR_CONSOLIDACION = "hasAnyAuthority('" + CONSOLIDACION_VER + "', '"
			+ CONSOLIDACION_ADMINISTRAR + "', '" + CONSOLIDACION_CONTACTAR + "')";

	/** Estadísticas de grupos: todos con {@code grupos.ver}; los del líder con {@code asistencia.grupo}. */
	public static final String PUEDE_VER_ESTADISTICAS_GRUPOS = "hasAnyAuthority('" + GRUPOS_VER + "', '"
			+ ASISTENCIA_GRUPO + "')";

	/** Estadísticas de asistencia: de la iglesia con {@code asistencia.ver}; de su red con {@code asistencia.grupo}. */
	public static final String PUEDE_VER_ESTADISTICAS_ASISTENCIA = "hasAnyAuthority('" + ASISTENCIA_VER + "', '"
			+ ASISTENCIA_GRUPO + "')";

	public static final String PUEDE_VER_ESTADISTICAS_CONSOLIDACION = "hasAnyAuthority('" + CONSOLIDACION_VER
			+ "', '" + CONSOLIDACION_ADMINISTRAR + "')";

	public static final String PUEDE_VER_ESTADISTICAS_SISTEMA = "hasAuthority('" + ESTADISTICAS_SISTEMA + "')";

	private Permisos() {
	}

}
