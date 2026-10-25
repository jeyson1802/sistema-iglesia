package com.iglesia.seguridad;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL real en contenedor y un par de claves RSA efímero, para no versionar claves. */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	private static final int BITS_CLAVE = 2048;

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
	}

	@Bean
	DynamicPropertyRegistrar clavesJwtDePrueba() {
		KeyPair claves = generarClaves();
		Path publica = escribirPem("PUBLIC KEY", claves.getPublic().getEncoded());
		Path privada = escribirPem("PRIVATE KEY", claves.getPrivate().getEncoded());
		return registro -> {
			registro.add("seguridad.jwt.clave-publica", () -> publica.toUri().toString());
			registro.add("seguridad.jwt.clave-privada", () -> privada.toUri().toString());
		};
	}

	private static KeyPair generarClaves() {
		try {
			KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
			generador.initialize(BITS_CLAVE);
			return generador.generateKeyPair();
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static Path escribirPem(String tipo, byte[] contenido) {
		String pem = "-----BEGIN " + tipo + "-----\n" + Base64.getMimeEncoder().encodeToString(contenido)
				+ "\n-----END " + tipo + "-----\n";
		try {
			Path archivo = Files.createTempFile("clave-jwt-", ".pem");
			archivo.toFile().deleteOnExit();
			return Files.writeString(archivo, pem);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
