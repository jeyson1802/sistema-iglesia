package com.iglesia.seguridad.rol.entity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.test.util.ReflectionTestUtils;

import com.iglesia.seguridad.rol.model.RolBase;

/**
 * Roles y permisos para pruebas unitarias. Los roles base solo los crea Flyway, por eso se arman aquí
 * con el mismo id que reciben en la migración, sin agregar fábricas al código de producción.
 */
public final class RolesPrueba {

	private RolesPrueba() {
	}

	public static Rol base(RolBase codigo, String... permisos) {
		Rol rol = new Rol();
		ReflectionTestUtils.setField(rol, "id", (long) codigo.ordinal() + 1);
		ReflectionTestUtils.setField(rol, "codigo", codigo);
		ReflectionTestUtils.setField(rol, "nombre", codigo.name());
		ReflectionTestUtils.setField(rol, "activo", true);
		ReflectionTestUtils.setField(rol, "permisos", permisos(permisos));
		return rol;
	}

	public static Set<Rol> bases(RolBase... codigos) {
		return Arrays.stream(codigos).map(RolesPrueba::base).collect(Collectors.toSet());
	}

	public static Rol personalizado(long id, long iglesiaId, String nombre, RolBase rolBase, String... permisos) {
		Rol rol = new Rol();
		ReflectionTestUtils.setField(rol, "id", id);
		ReflectionTestUtils.setField(rol, "iglesiaId", iglesiaId);
		ReflectionTestUtils.setField(rol, "nombre", nombre);
		ReflectionTestUtils.setField(rol, "rolBase", base(rolBase));
		ReflectionTestUtils.setField(rol, "activo", true);
		ReflectionTestUtils.setField(rol, "permisos", permisos(permisos));
		return rol;
	}

	public static Rol desactivado(Rol rol) {
		ReflectionTestUtils.setField(rol, "activo", false);
		return rol;
	}

	public static Permiso permiso(String codigo) {
		Permiso permiso = new Permiso();
		ReflectionTestUtils.setField(permiso, "codigo", codigo);
		return permiso;
	}

	private static Set<Permiso> permisos(String... codigos) {
		return Arrays.stream(codigos).map(RolesPrueba::permiso).collect(Collectors.toCollection(HashSet::new));
	}

}
