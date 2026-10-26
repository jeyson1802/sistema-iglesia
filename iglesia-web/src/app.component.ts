import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ToastErroresGlobalesComponent } from './app/core/errores/toast-errores-globales/toast-errores-globales';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [RouterModule, ToastErroresGlobalesComponent],
    template: `<router-outlet></router-outlet><app-toast-errores-globales />`
})
export class AppComponent {}
