package com.iglesia.seguridad.usuario.mapper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.iglesia.seguridad.common.seguridad.model.UsuarioAutenticado;
import com.iglesia.seguridad.rol.entity.Permiso;
import com.iglesia.seguridad.rol.entity.Rol;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.usuario.dto.MiUsuarioResponse;
import com.iglesia.seguridad.usuario.dto.PerfilResponse;
import com.iglesia.seguridad.usuario.dto.RolAsignadoResponse;
import com.iglesia.seguridad.usuario.dto.UsuarioResponse;
import com.iglesia.seguridad.usuario.entity.Usuario;

@Component
public class UsuarioMapper {

	/** Jerarquía del rol base (el mayor primero) y, dentro de ella, por nombre. */
	private static final Comparator<Rol> POR_JERARQUIA_Y_NOMBRE = Comparator.comparing(Rol::rolBaseEfectivo)
		.thenComparing(Rol::getNombre, String.CASE_INSENSITIVE_ORDER);

	public UsuarioResponse aRespuesta(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getCorreo(), usuario.getIglesiaId(), usuario.getMiembroId(),
				usuario.isActivo(), usuario.isDebeCambiarContrasena(), ordenar(usuario.getRoles(), this::aRolAsignado),
				usuario.getCreadoEn());
	}

	/**
	 * La iglesia es la del token (la del usuario o, para el administrador del sistema, a la que entró). Dentro de
	 * una iglesia, su perfil lleva los permisos del token, que incluyen los de un administrador de iglesia.
	 */
	public MiUsuarioResponse aMiUsuario(Usuario usuario, UsuarioAutenticado actor) {
		Function<Rol, PerfilResponse> aPerfil = actor.actuandoEnIglesia() ? rol -> aPerfilActuando(rol, actor)
				: this::aPerfil;
		return new MiUsuarioResponse(usuario.getId(), usuario.getCorreo(), actor.iglesiaId(), usuario.getMiembroId(),
				usuario.isDebeCambiarContrasena(), actor.actuandoEnIglesia(), ordenar(usuario.getRolesActivos(), aPerfil));
	}

	private RolAsignadoResponse aRolAsignado(Rol rol) {
		return new RolAsignadoResponse(rol.getId(), rol.getNombre(), rol.rolBaseEfectivo());
	}

	private PerfilResponse aPerfil(Rol rol) {
		List<String> permisos = rol.getPermisos().stream().map(Permiso::getCodigo).sorted().toList();
		return new PerfilResponse(rol.getId(), rol.getNombre(), rol.rolBaseEfectivo(), permisos);
	}

	private PerfilResponse aPerfilActuando(Rol rol, UsuarioAutenticado actor) {
		if (rol.rolBaseEfectivo() != RolBase.ADMIN_SISTEMA) {
			return aPerfil(rol);
		}
		return new PerfilResponse(rol.getId(), rol.getNombre(), rol.rolBaseEfectivo(),
				actor.permisos().stream().sorted().toList());
	}

	private static <T> List<T> ordenar(Collection<Rol> roles, Function<Rol, T> convertir) {
		return roles.stream().sorted(POR_JERARQUIA_Y_NOMBRE).map(convertir).toList();
	}

}
