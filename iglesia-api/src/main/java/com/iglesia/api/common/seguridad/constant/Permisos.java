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

	private Permisos() {
	}

}
