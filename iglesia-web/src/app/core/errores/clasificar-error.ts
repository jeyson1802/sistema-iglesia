import { HttpErrorResponse } from '@angular/common/http';

const ERROR_DE_SERVIDOR_MINIMO = 500;
/** `status` que Angular asigna cuando no hubo respuesta (sin red, CORS, servidor caído). */
const SIN_RESPUESTA = 0;

/** 5xx: fallo inesperado del backend, con código de seguimiento para soporte. */
export const esErrorInesperado = (error: unknown): error is HttpErrorResponse => error instanceof HttpErrorResponse && error.status >= ERROR_DE_SERVIDOR_MINIMO;

/** No se pudo hablar con el servidor. */
export const esErrorDeConexion = (error: unknown): error is HttpErrorResponse => error instanceof HttpErrorResponse && error.status === SIN_RESPUESTA;

/** Errores que avisa el toast global; las páginas no deben repetir el mensaje. Los 4xx los maneja cada página. */
export const esErrorGlobal = (error: unknown): boolean => esErrorInesperado(error) || esErrorDeConexion(error);
