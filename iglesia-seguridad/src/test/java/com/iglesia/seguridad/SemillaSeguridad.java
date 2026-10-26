package com.iglesia.seguridad;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Siembra por SQL usuarios y roles personalizados para las pruebas de integración, sin pasar por la API.
 * Cada prueba usa su propia iglesia para no chocar con los datos de las demás.
 */
public class SemillaSeguridad {

	public static final String CONTRASENA = "ClavePrueba123";

	private final JdbcTemplate jdbc;

	private final PasswordEncoder codificador;

	public SemillaSeguridad(JdbcTemplate jdbc, PasswordEncoder codificador) {
		this.jdbc = jdbc;
		this.codificador = codificador;
	}

	public long idRolBase(String codigo) {
		return jdbc.queryForObject("SELECT id FROM rol WHERE codigo = ?", Long.class, codigo);
	}

	public long crearRolPersonalizado(long iglesiaId, String nombre, String rolBase, boolean activo,
			String... permisos) {
		long id = jdbc.queryForObject("""
				INSERT INTO rol (iglesia_id, nombre, rol_base_id, activo, creado_en, actualizado_en)
				VALUES (?, ?, ?, ?, now(), now()) RETURNING id
				""", Long.class, iglesiaId, nombre, idRolBase(rolBase), activo);
		for (String permiso : permisos) {
			jdbc.update("INSERT INTO rol_permiso (rol_id, permiso_codigo) VALUES (?, ?)", id, permiso);
		}
		return id;
	}

	/** Crea un usuario activo con {@link #CONTRASENA} y le asigna esos roles. */
	public long crearUsuario(String correo, Long iglesiaId, Long miembroId, long... rolIds) {
		long id = jdbc.queryForObject("""
				INSERT INTO usuario (correo, hash_contrasena, iglesia_id, miembro_id, creado_en, actualizado_en)
				VALUES (?, ?, ?, ?, now(), now()) RETURNING id
				""", Long.class, correo, codificador.encode(CONTRASENA), iglesiaId, miembroId);
		for (long rolId : rolIds) {
			jdbc.update("INSERT INTO usuario_rol (usuario_id, rol_id) VALUES (?, ?)", id, rolId);
		}
		return id;
	}

}
