package com.iglesia.api.miembros.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.iglesia.api.miembros.constant.LimitesMiembro;
import com.iglesia.api.miembros.model.CondicionMiembro;
import com.iglesia.api.miembros.model.EstadoCivil;
import com.iglesia.api.miembros.model.Genero;
import com.iglesia.api.miembros.model.NivelEstudios;

@Schema(description = """
		Ficha del miembro para registrarlo o editarla. Solo `nombres` y `apellidos` son obligatorios; el resto se \
		completa con el tiempo. Los textos se guardan sin espacios sobrantes y un texto vacío equivale a no indicarlo.""")
public record GuardarMiembroRequest(
		@Schema(description = "Nombres.", example = "Juan Carlos") @NotBlank @Size(
				max = LimitesMiembro.NOMBRES_MAX) String nombres,
		@Schema(description = "Apellidos.", example = "Pérez Torres") @NotBlank @Size(
				max = LimitesMiembro.APELLIDOS_MAX) String apellidos,
		@Schema(description = "Género.", example = "MASCULINO", nullable = true) Genero genero,
		@Schema(description = "Fecha de nacimiento; no puede ser futura. Independiente de `edadAproximada`.",
				example = "1990-05-14", nullable = true) @PastOrPresent LocalDate fechaNacimiento,
		@Schema(description = "Edad aproximada, independiente de `fechaNacimiento` (se pueden enviar las dos aunque "
				+ "no coincidan). Se guarda como año de nacimiento estimado para que siga siendo correcta con el "
				+ "tiempo.", example = "35",
				nullable = true) @Min(0) @Max(LimitesMiembro.EDAD_MAX) Integer edadAproximada,
		@Schema(description = "Fecha de ingreso a la iglesia; no puede ser futura.", example = "2024-01-07",
				nullable = true) @PastOrPresent LocalDate fechaIngreso,
		@Schema(description = "Condición: consolidado, nuevo o recuperado.", example = "NUEVO",
				nullable = true) CondicionMiembro condicion,
		@Schema(description = "Estado civil.", example = "CASADO", nullable = true) EstadoCivil estadoCivil,
		@Schema(description = "Teléfono: dígitos, espacios, `+` y `-`.", example = "+51 987 654 321",
				nullable = true) @Size(max = LimitesMiembro.TELEFONO_MAX) @Pattern(
						regexp = LimitesMiembro.TELEFONO_PATRON,
						message = "solo admite dígitos, espacios, + y -") String telefono,
		@Schema(description = "Correo electrónico. No es único: una familia puede compartirlo.",
				example = "juan.perez@correo.com", nullable = true) @Email @Size(
						max = LimitesMiembro.CORREO_MAX) String correo,
		@Schema(description = "Dirección (calle, número, referencia).", example = "Av. Los Olivos 123",
				nullable = true) @Size(max = LimitesMiembro.DIRECCION_MAX) String direccion,
		@Schema(description = "Código INEI del distrito; departamento y provincia se deducen de él.",
				example = "150101", nullable = true) @Pattern(regexp = LimitesMiembro.DISTRITO_PATRON,
						message = "debe tener 6 dígitos") String distritoCodigo,
		@Schema(description = "Ocupación actual.", example = "Comerciante", nullable = true) @Size(
				max = LimitesMiembro.OCUPACION_MAX) String ocupacion,
		@Schema(description = "Profesión.", example = "Contador", nullable = true) @Size(
				max = LimitesMiembro.PROFESION_MAX) String profesion,
		@Schema(description = "Nivel de educación formal alcanzado.", example = "UNIVERSITARIO",
				nullable = true) NivelEstudios nivelEstudios,
		@Schema(description = "Centro de estudios.", example = "Universidad Nacional de Ingeniería",
				nullable = true) @Size(max = LimitesMiembro.CENTRO_ESTUDIOS_MAX) String centroEstudios,
		@Schema(description = "Petición o necesidad con la que llega.", example = "Oración por su familia",
				nullable = true) @Size(max = LimitesMiembro.PETICION_MAX) String peticion,
		@Schema(description = "Nivel (rango) del miembro; activo al asignarlo.", example = "3",
				nullable = true) Long nivelId,
		@Schema(description = "Quién lo lidera: activo, distinto del propio miembro y fuera de su red. El pastor "
				+ "principal no tiene líder.", example = "12", nullable = true) Long liderId,
		@Schema(description = "Grupo al que pertenece; activo al asignarlo. Puede ser de un líder distinto del suyo.",
				example = "4", nullable = true) Long grupoId,
		@Schema(description = "Miembro que lo invitó; activo y distinto del propio miembro.", example = "15",
				nullable = true) Long invitadoPorId,
		@Schema(description = "Miembro que lo registró; activo y distinto del propio miembro.", example = "12",
				nullable = true) Long registradoPorId) {
}
