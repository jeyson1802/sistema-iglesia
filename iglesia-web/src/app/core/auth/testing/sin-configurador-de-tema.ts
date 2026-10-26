import { Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { MetadataOverride } from '@angular/core/testing';
import { AppFloatingConfigurator } from '@/app/layout/component/app.floatingconfigurator';

/**
 * Override para pruebas: quita el configurador de tema de Sakai (lee la URL y lanza transiciones de tema),
 * que no es parte de lo que se prueba.
 */
export const SIN_CONFIGURADOR_DE_TEMA: MetadataOverride<Component> = {
    remove: { imports: [AppFloatingConfigurator] },
    add: { schemas: [CUSTOM_ELEMENTS_SCHEMA] }
};
