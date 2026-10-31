package com.iglesia.seguridad.actividad.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import com.iglesia.seguridad.actividad.repository.ActividadUsuarioRepository;
import com.iglesia.seguridad.rol.model.RolBase;
import com.iglesia.seguridad.usuario.entity.Usuario;

/**
 * Anota que el usuario usó el sistema hoy (una fila por usuario y día). Lo llama el inicio de sesión y la
 * renovación del token; si falla, se avisa con {@code WARN} y la sesión sigue, porque medir el uso nunca debe
 * impedir entrar.
 */
@Service
public class RegistroActividadService {

	private static final Logger LOG = LoggerFactory.getLogger(RegistroActividadService.class);

	private final ActividadUsuarioRepository actividades;

	private final Clock reloj;

	private final TransactionTemplate transaccionPropia;

	RegistroActividadService(ActividadUsuarioRepository actividades, Clock reloj,
			PlatformTransactionManager transacciones) {
		this.actividades = actividades;
		this.reloj = reloj;
		this.transaccionPropia = new TransactionTemplate(transacciones);
		this.transaccionPropia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/**
	 * Corre en su propia transacción y atrapa la falla por fuera de ella, para que no deshaga ni marque para
	 * deshacer la sesión que se está emitiendo.
	 */
	public void registrar(Usuario usuario) {
		Long usuarioId = usuario.getId();
		Long iglesiaId = usuario.getIglesiaId();
		String rolBase = rolPrincipal(usuario).name();
		try {
			transaccionPropia.executeWithoutResult(estado -> actividades.registrar(usuarioId, hoy(), iglesiaId, rolBase));
		}
		catch (DataAccessException | TransactionException ex) {
			LOG.warn("No se pudo registrar la actividad usuarioId={}: {}", usuario.getId(), ex.getMessage());
		}
	}

	/** El rol base de mayor alcance (el orden del enum va de mayor a menor alcance). */
	static RolBase rolPrincipal(Usuario usuario) {
		return usuario.getRolesBaseActivos().stream().min(Comparator.naturalOrder()).orElse(RolBase.MIEMBRO);
	}

	private LocalDate hoy() {
		return LocalDate.ofInstant(reloj.instant(), ZoneId.systemDefault());
	}

}
