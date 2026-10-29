package com.iglesia.api.grupos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.iglesia.api.grupos.constant.LimitesGrupo;

@Schema(description = "Nombre con el que la iglesia llama a sus grupos, en singular y plural.")
public record GuardarConfiguracionIglesiaRequest(
		@Schema(description = "Singular, p. ej. «Célula» o «Hogar de paz».", example = "Hogar de paz") @NotBlank @Size(
				max = LimitesGrupo.NOMBRE_TIPO_MAX) String nombreGrupoSingular,
		@Schema(description = "Plural, p. ej. «Células» u «Hogares de paz».", example = "Hogares de paz") @NotBlank @Size(
				max = LimitesGrupo.NOMBRE_TIPO_MAX) String nombreGrupoPlural) {
}
