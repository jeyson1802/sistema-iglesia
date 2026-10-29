package com.iglesia.api.iglesia.component;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.iglesia.api.iglesia.model.TipoImagen;

/**
 * Lee y escribe la imagen de {@code logo_iglesia}, que tiene RLS. Quien la usa puede ser un usuario de esa iglesia o el
 * administrador del sistema operando sobre cualquier iglesia del catálogo, así que cada operación fija
 * {@code app.iglesia_id} en la iglesia del logo y al terminar restaura el valor que tenía la transacción (el que fijó
 * {@code IglesiaJpaTransactionManager} a partir del token), para no alterar el resto de la petición.
 * <p>
 * Corre dentro de la transacción del servicio que la llama: {@code set_config(..., true)} solo dura esa transacción.
 */
@Component
public class AlmacenLogoIglesia {

	private static final String LEER_IGLESIA = "select current_setting('app.iglesia_id', true)";

	private static final String FIJAR_IGLESIA = "select set_config('app.iglesia_id', ?, true)";

	private static final String GUARDAR = """
			insert into logo_iglesia (iglesia_id, contenido, tipo) values (?, ?, ?)
			on conflict (iglesia_id) do update set contenido = excluded.contenido, tipo = excluded.tipo""";

	private static final String LEER = "select contenido, tipo from logo_iglesia where iglesia_id = ?";

	private static final String BORRAR = "delete from logo_iglesia where iglesia_id = ?";

	private final JdbcTemplate jdbc;

	AlmacenLogoIglesia(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void guardar(Long iglesiaId, byte[] contenido, TipoImagen tipo) {
		enIglesia(iglesiaId, () -> jdbc.update(GUARDAR, iglesiaId, contenido, tipo.tipoMime()));
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Optional<ImagenGuardada> leer(Long iglesiaId) {
		return enIglesia(iglesiaId, () -> {
			List<ImagenGuardada> filas = jdbc.query(LEER,
					(fila, numero) -> new ImagenGuardada(fila.getBytes("contenido"), fila.getString("tipo")), iglesiaId);
			return filas.stream().findFirst();
		});
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void borrar(Long iglesiaId) {
		enIglesia(iglesiaId, () -> jdbc.update(BORRAR, iglesiaId));
	}

	private <T> T enIglesia(Long iglesiaId, Operacion<T> operacion) {
		String anterior = jdbc.queryForObject(LEER_IGLESIA, String.class);
		fijar(iglesiaId.toString());
		try {
			return operacion.ejecutar();
		}
		finally {
			fijar(anterior == null ? "" : anterior);
		}
	}

	private void fijar(String valor) {
		jdbc.queryForObject(FIJAR_IGLESIA, String.class, valor);
	}

	@FunctionalInterface
	private interface Operacion<T> {

		T ejecutar();

	}

	/** Contenido y tipo MIME tal como están en la tabla. */
	public record ImagenGuardada(byte[] contenido, String tipo) {
	}

}
