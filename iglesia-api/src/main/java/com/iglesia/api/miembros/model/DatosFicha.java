package com.iglesia.api.miembros.model;

import java.time.LocalDate;

import com.iglesia.api.miembros.entity.Miembro;
import com.iglesia.api.niveles.entity.Nivel;

/**
 * Datos de la ficha ya normalizados y validados, con las referencias resueltas, listos para aplicarlos a un
 * {@link Miembro}. Los textos vienen sin espacios sobrantes y los vacíos como {@code null}.
 */
public record DatosFicha(String nombres, String apellidos, Genero genero, LocalDate fechaNacimiento,
		Short anioNacimientoEstimado, LocalDate fechaIngreso, CondicionMiembro condicion, EstadoCivil estadoCivil,
		String telefono, String correo, String direccion, String distritoCodigo, String ocupacion, String profesion,
		NivelEstudios nivelEstudios, String centroEstudios, String peticion, Nivel nivel, Miembro lider, Long grupoId,
		Miembro invitadoPor, Miembro registradoPor) {
}
