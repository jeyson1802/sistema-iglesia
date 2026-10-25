package com.iglesia.seguridad.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenRefrescoRequest(@NotBlank @Size(max = 128) String tokenRefresco) {

	@Override
	public String toString() {
		return "TokenRefrescoRequest[tokenRefresco=***]";
	}

}
