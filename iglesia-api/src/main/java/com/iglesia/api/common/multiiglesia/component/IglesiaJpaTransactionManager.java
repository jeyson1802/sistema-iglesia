package com.iglesia.api.common.multiiglesia.component;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Segunda barrera de aislamiento: al iniciar cada transacción fija {@code app.iglesia_id} en la
 * conexión (equivalente a {@code SET LOCAL}, se descarta al terminar la transacción) para que las
 * políticas de Row-Level Security de PostgreSQL filtren por la iglesia del token.
 */
public class IglesiaJpaTransactionManager extends JpaTransactionManager {

	private static final String FIJAR_IGLESIA = "select set_config('app.iglesia_id', ?, true)";

	private final transient IglesiaActual iglesiaActual;

	public IglesiaJpaTransactionManager(EntityManagerFactory fabrica, IglesiaActual iglesiaActual) {
		super(fabrica);
		this.iglesiaActual = iglesiaActual;
	}

	@Override
	protected void doBegin(Object transaccion, TransactionDefinition definicion) {
		super.doBegin(transaccion, definicion);
		iglesiaActual.id().ifPresent(this::fijarIglesia);
	}

	private void fijarIglesia(Long iglesiaId) {
		EntityManagerHolder contenedor = (EntityManagerHolder) TransactionSynchronizationManager
			.getResource(obtenerFabrica());
		contenedor.getEntityManager().unwrap(Session.class).doWork(conexion -> {
			try (var sentencia = conexion.prepareStatement(FIJAR_IGLESIA)) {
				sentencia.setString(1, iglesiaId.toString());
				sentencia.execute();
			}
		});
	}

	private EntityManagerFactory obtenerFabrica() {
		EntityManagerFactory fabrica = getEntityManagerFactory();
		if (fabrica == null) {
			throw new IllegalStateException("EntityManagerFactory no configurado");
		}
		return fabrica;
	}

}
