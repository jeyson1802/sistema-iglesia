package com.iglesia.api.common.multiiglesia;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Conecta las dos barreras de aislamiento por iglesia: Hibernate ({@code @TenantId}) y PostgreSQL (RLS). */
@Configuration(proxyBeanMethods = false)
class MultiIglesiaConfig {

	@Bean
	HibernatePropertiesCustomizer resolutorIglesia(ResolutorIglesiaHibernate resolutor) {
		return propiedades -> propiedades.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolutor);
	}

	@Bean
	PlatformTransactionManager transactionManager(EntityManagerFactory fabrica, IglesiaActual iglesiaActual) {
		return new IglesiaJpaTransactionManager(fabrica, iglesiaActual);
	}

}
