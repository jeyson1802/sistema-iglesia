package com.iglesia.api.common.multiiglesia;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import com.iglesia.api.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica la segunda barrera contra PostgreSQL real: el gestor de transacciones fija la iglesia del
 * JWT y la RLS filtra las filas cuando se opera con el rol de aplicación (no dueño de las tablas).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AislamientoPorIglesiaIntegrationTest {

	private static final String ROL_APLICACION = "SET LOCAL ROLE iglesia_app";

	@Autowired
	private TransactionTemplate transacciones;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void crearTablaDePrueba() {
		jdbc.execute("""
				CREATE TABLE IF NOT EXISTS prueba_aislamiento (
				    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
				    iglesia_id BIGINT NOT NULL)
				""");
		jdbc.execute("DROP POLICY IF EXISTS aislamiento_iglesia ON prueba_aislamiento");
		jdbc.execute("SELECT habilitar_aislamiento_por_iglesia('prueba_aislamiento')");
		jdbc.execute("TRUNCATE prueba_aislamiento");
		jdbc.update("INSERT INTO prueba_aislamiento (iglesia_id) VALUES (1), (1), (2)");
	}

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void fijaLaIglesiaDelTokenEnLaTransaccion() {
		ContextoJwt.autenticarConIglesia(1L);

		String valor = transacciones
			.execute(estado -> jdbc.queryForObject("select current_setting('app.iglesia_id', true)", String.class));

		assertThat(valor).isEqualTo("1");
	}

	@Test
	void laRlsSoloMuestraLasFilasDeLaIglesiaDelToken() {
		ContextoJwt.autenticarConIglesia(1L);
		assertThat(contarComoAplicacion()).isEqualTo(2);

		ContextoJwt.autenticarConIglesia(2L);
		assertThat(contarComoAplicacion()).isEqualTo(1);
	}

	@Test
	void sinIglesiaEnElTokenNoSeVeNada() {
		ContextoJwt.autenticarConIglesia(null);

		assertThat(contarComoAplicacion()).isZero();
	}

	@Test
	void laRlsImpideInsertarFilasDeOtraIglesia() {
		ContextoJwt.autenticarConIglesia(1L);

		assertThatThrownBy(() -> transacciones.executeWithoutResult(estado -> {
			jdbc.execute(ROL_APLICACION);
			jdbc.update("INSERT INTO prueba_aislamiento (iglesia_id) VALUES (2)");
		})).rootCause().hasMessageContaining("row-level security");
	}

	@Test
	void elValorNoSobreviveALaTransaccion() {
		ContextoJwt.autenticarConIglesia(1L);
		transacciones.executeWithoutResult(estado -> jdbc.execute(ROL_APLICACION));
		SecurityContextHolder.clearContext();

		String valor = transacciones.execute(estado -> jdbc
			.queryForObject("select coalesce(current_setting('app.iglesia_id', true), '')", String.class));

		assertThat(valor).isEmpty();
	}

	private Integer contarComoAplicacion() {
		return transacciones.execute(estado -> {
			jdbc.execute(ROL_APLICACION);
			return jdbc.queryForObject("select count(*) from prueba_aislamiento", Integer.class);
		});
	}

}
