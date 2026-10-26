package com.iglesia.seguridad.auth.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.iglesia.seguridad.usuario.entity.Usuario;

/** Token de refresco de un solo uso. Solo se persiste el hash del valor entregado al cliente. */
@Entity
@Table(name = "token_refresco")
public class TokenRefresco {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, updatable = false)
	private Usuario usuario;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "hash_token", nullable = false, updatable = false, length = 64)
	private String hashToken;

	@Column(name = "emitido_en", nullable = false, updatable = false)
	private Instant emitidoEn;

	@Column(name = "expira_en", nullable = false, updatable = false)
	private Instant expiraEn;

	@Column(name = "revocado_en")
	private Instant revocadoEn;

	protected TokenRefresco() {
	}

	public TokenRefresco(Usuario usuario, String hashToken, Instant emitidoEn, Instant expiraEn) {
		this.usuario = usuario;
		this.hashToken = hashToken;
		this.emitidoEn = emitidoEn;
		this.expiraEn = expiraEn;
	}

	public boolean estaRevocado() {
		return revocadoEn != null;
	}

	public boolean estaVigente(Instant ahora) {
		return !estaRevocado() && ahora.isBefore(expiraEn);
	}

	public void revocar(Instant ahora) {
		if (!estaRevocado()) {
			revocadoEn = ahora;
		}
	}

	public Usuario getUsuario() {
		return usuario;
	}

}
