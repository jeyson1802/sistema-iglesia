package com.iglesia.api.aportes.component;

import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.iglesia.api.iglesia.model.IglesiaCreada;

/**
 * Da a cada iglesia nueva sus categorías de aporte iniciales (Diezmo, que exige aportante, y Ofrenda, que admite
 * anónimos), las mismas que {@code V7__aportes.sql} dio a las existentes. Son filas normales: la iglesia puede
 * renombrarlas o desactivarlas.
 * <p>
 * Corre dentro de la transacción del alta, que hace el administrador del sistema sin iglesia en el token: por eso fija
 * {@code app.iglesia_id} de la iglesia nueva solo para esta inserción (la RLS la exige) y luego lo deja vacío.
 */
@Component
class CategoriasPorDefecto {

	private static final List<CategoriaInicial> INICIALES = List.of(new CategoriaInicial("Diezmo", true, 1),
			new CategoriaInicial("Ofrenda", false, 2));

	private static final String FIJAR_IGLESIA = "select set_config('app.iglesia_id', ?, true)";

	private static final String SIN_IGLESIA = "";

	private static final String INSERTAR = """
			insert into categoria_aporte (iglesia_id, nombre, exige_aportante, orden) values (?, ?, ?, ?)""";

	private final JdbcTemplate jdbc;

	CategoriasPorDefecto(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@EventListener
	void alCrearIglesia(IglesiaCreada evento) {
		fijarIglesia(evento.iglesiaId().toString());
		for (CategoriaInicial categoria : INICIALES) {
			jdbc.update(INSERTAR, evento.iglesiaId(), categoria.nombre(), categoria.exigeAportante(),
					categoria.orden());
		}
		fijarIglesia(SIN_IGLESIA);
	}

	private void fijarIglesia(String valor) {
		jdbc.queryForObject(FIJAR_IGLESIA, String.class, valor);
	}

	private record CategoriaInicial(String nombre, boolean exigeAportante, int orden) {
	}

}
