package com.iglesia.api.miembros.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.iglesia.api.PruebasNegocio;
import com.iglesia.api.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V5 contra PostgreSQL real: la RLS aísla las cuatro tablas nuevas para el rol de aplicación, las FK compuestas
 * impiden referenciar filas de otra iglesia y la base respalda las reglas del pastor principal.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AislamientoRegistroMiembrosIntegrationTest {

	private static final String COMO_APLICACION = "SET LOCAL ROLE iglesia_app";

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private TransactionTemplate transacciones;

	@ParameterizedTest
	@ValueSource(strings = { "nivel", "miembro", "grupo", "configuracion_iglesia" })
	void laRlsSoloMuestraLasFilasDeLaIglesiaFijada(String tabla) {
		long uno = PruebasNegocio.nuevaIglesia(jdbc);
		long dos = PruebasNegocio.nuevaIglesia(jdbc);
		insertar(tabla, uno);
		insertar(tabla, dos);

		assertThat(contarComoAplicacion(tabla, uno, dos)).isEqualTo(1);
		assertThat(contarComoAplicacion(tabla, dos, uno)).isEqualTo(1);
	}

	@Test
	void unMiembroNoPuedeTenerComoLiderNiNivelAFilasDeOtraIglesia() {
		long uno = PruebasNegocio.nuevaIglesia(jdbc);
		long dos = PruebasNegocio.nuevaIglesia(jdbc);
		long nivelAjeno = insertar("nivel", dos);
		long liderAjeno = insertar("miembro", dos);

		assertThatThrownBy(() -> jdbc.update(
				"insert into miembro (iglesia_id, nombres, apellidos, lider_id) values (?, 'A', 'B', ?)", uno,
				liderAjeno))
			.hasMessageContaining("foreign key");
		assertThatThrownBy(() -> jdbc.update(
				"insert into miembro (iglesia_id, nombres, apellidos, nivel_id) values (?, 'A', 'B', ?)", uno,
				nivelAjeno))
			.hasMessageContaining("foreign key");
	}

	@Test
	void soloHayUnPastorPrincipalPorIglesiaYNoTieneLider() {
		long iglesia = PruebasNegocio.nuevaIglesia(jdbc);
		long lider = insertar("miembro", iglesia);
		jdbc.update("insert into miembro (iglesia_id, nombres, apellidos, es_pastor_principal) values (?, 'P', 'A', true)",
				iglesia);

		assertThatThrownBy(() -> jdbc.update(
				"insert into miembro (iglesia_id, nombres, apellidos, es_pastor_principal) values (?, 'P', 'B', true)",
				iglesia))
			.hasMessageContaining("ux_miembro_pastor_principal");
		assertThatThrownBy(() -> jdbc.update("update miembro set es_pastor_principal = true, lider_id = ? "
				+ "where iglesia_id = ? and id <> ? and not es_pastor_principal", lider, iglesia, lider))
			.hasMessageContaining("violates");
	}

	private long insertar(String tabla, long iglesia) {
		return switch (tabla) {
			case "nivel" -> jdbc.queryForObject(
					"insert into nivel (iglesia_id, nombre, orden) values (?, 'Nivel', 1) returning id", Long.class,
					iglesia);
			case "miembro" -> jdbc.queryForObject(
					"insert into miembro (iglesia_id, nombres, apellidos) values (?, 'A', 'B') returning id",
					Long.class, iglesia);
			case "grupo" -> jdbc.queryForObject(
					"insert into grupo (iglesia_id, nombre) values (?, 'Grupo') returning id", Long.class, iglesia);
			default -> {
				jdbc.update("insert into configuracion_iglesia values (?, 'Célula', 'Células')", iglesia);
				yield iglesia;
			}
		};
	}

	/** Cuenta, como rol de aplicación con la iglesia fijada, las filas de las dos iglesias de la prueba. */
	private Integer contarComoAplicacion(String tabla, long fijada, long otra) {
		return transacciones.execute(estado -> {
			jdbc.execute(COMO_APLICACION);
			jdbc.queryForObject("select set_config('app.iglesia_id', ?, true)", String.class, String.valueOf(fijada));
			return jdbc.queryForObject("select count(*) from " + tabla + " where iglesia_id in (?, ?)", Integer.class,
					fijada, otra);
		});
	}

}
