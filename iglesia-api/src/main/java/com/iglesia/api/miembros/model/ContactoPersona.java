package com.iglesia.api.miembros.model;

import java.time.LocalDate;

/**
 * Lo que un consolidador necesita para contactar a una persona: teléfono, dirección, quién la invitó, su líder y la
 * petición con la que llegó. No incluye aportes ni el resto de la ficha.
 *
 * @param ubicacion «Distrito, Provincia», o {@code null} si no tiene distrito
 * @param invitadoPor nombre de quien la invitó, o {@code null}
 * @param lider nombre de su líder, o {@code null}
 */
public record ContactoPersona(Long id, String nombreCompleto, String telefono, String correo, String direccion,
		String ubicacion, CondicionMiembro condicion, LocalDate fechaIngreso, LocalDate fechaNacimiento,
		String invitadoPor, String lider, Long grupoId, String peticion, boolean activo) {
}
