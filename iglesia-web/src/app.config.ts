import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, TitleStrategy, withEnabledBlockingInitialNavigation, withInMemoryScrolling } from '@angular/router';
import Aura from '@primeuix/themes/aura';
import { MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { appRoutes } from './app.routes';
import { AparienciaService } from './app/core/apariencia/services/apariencia.service';
import { PreferenciaModoOscuroService } from './app/core/apariencia/services/preferencia-modo-oscuro.service';
import { SesionService } from './app/core/auth/services/sesion.service';
import { authInterceptor } from './app/core/interceptors/auth.interceptor';
import { erroresInterceptor } from './app/core/interceptors/errores.interceptor';
import { FaviconIglesiaService } from './app/core/pestana/favicon-iglesia.service';
import { TituloIglesiaStrategy } from './app/core/pestana/titulo-iglesia.strategy';

export const appConfig: ApplicationConfig = {
    providers: [
        provideRouter(appRoutes, withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' }), withEnabledBlockingInitialNavigation()),
        // errores va después de auth: ve cada respuesta del backend una sola vez, también la del reintento tras refrescar.
        provideHttpClient(withFetch(), withInterceptors([authInterceptor, erroresInterceptor])),
        // MessageService único para el toast global de errores (app-toast-errores-globales en AppComponent).
        MessageService,
        // Recupera la sesión con la cookie de refresco antes de la primera navegación (sobrevive a F5).
        // Modo oscuro de la persona y última apariencia conocida antes de pintar, para no parpadear.
        provideAppInitializer(() => {
            inject(PreferenciaModoOscuroService).aplicarInicial();
            inject(AparienciaService).aplicarRecordada();
            inject(FaviconIglesiaService);
        }),
        provideAppInitializer(() => inject(SesionService).restaurar()),
        // Título de la pestaña «Página · Nombre comercial».
        { provide: TitleStrategy, useClass: TituloIglesiaStrategy },
        provideZonelessChangeDetection(),
        providePrimeNG({ theme: { preset: Aura, options: { darkModeSelector: '.app-dark' } } })
    ]
};
