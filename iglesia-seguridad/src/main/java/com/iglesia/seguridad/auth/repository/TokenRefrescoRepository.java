package com.iglesia.seguridad.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, Long> {

	@EntityGraph(attributePaths = { "usuario", "usuario.roles" })
	Optional<TokenRefresco> findByHashToken(String hashToken);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update TokenRefresco t set t.revocadoEn = :ahora where t.usuario.id = :usuarioId and t.revocadoEn is null")
	int revocarVigentesDelUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

}
