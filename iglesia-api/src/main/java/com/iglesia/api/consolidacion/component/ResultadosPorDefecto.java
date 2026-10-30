package com.iglesia.api.consolidacion.component;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.iglesia.api.iglesia.model.IglesiaCreada;

/**
 * Da a cada iglesia nueva los resultados de consolidación iniciales, los mismos que {@code V11__consolidacion.sql} dio
 * a las existentes. Son filas normales: la iglesia puede renombrarlos, ordenarlos o desactivarlos.
 * <p>
 * Corre dentro de la transacción del alta, sin iglesia en el token: fija {@code app.iglesia_id} de la iglesia nueva
 * solo para esta inserción (la RLS la exige) y luego lo deja vacío, como {@code CategoriasPorDefecto}.
 */
@Component
class ResultadosPorDefecto {

	static final List<String> INICIALES = List.of("Se integró a un grupo", "Volverá a asistir",
			"Necesita otra visita", "No le interesa por ahora", "Se congrega en otra iglesia", "No se pudo ubicar");

	private static final String FIJAR_IGLESIA = "select set_config('app.iglesia_id', ?, true)";

	private static final String SIN_IGLESIA = "";

	private static final String INSERTAR = "insert into resultado_consolidacion (iglesia_id, nombre, orden) values (?, ?, ?)";

	private final JdbcTemplate jdbc;

	ResultadosPorDefecto(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@EventListener
	void alCrearIglesia(IglesiaCreada evento) {
		fijarIglesia(evento.iglesiaId().toString());
		for (int posicion = 0; posicion < INICIALES.size(); posicion++) {
			jdbc.update(INSERTAR, evento.iglesiaId(), INICIALES.get(posicion), posicion + 1);
		}
		fijarIglesia(SIN_IGLESIA);
	}

	private void fijarIglesia(String valor) {
		jdbc.queryForObject(FIJAR_IGLESIA, String.class, valor);
	}

}
