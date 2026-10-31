import { of } from 'rxjs';
import { EstadisticasService } from '../services/estadisticas.service';
import { aportesPrueba, asistenciaPrueba, consolidacionPrueba, gruposPrueba, miAsistenciaPrueba, miembrosPrueba, miPerfilPrueba, sistemaPrueba, usoPrueba } from './estadisticas-prueba';

/** Espía del servicio de estadísticas que responde con los datos de prueba. */
export function estadisticasServicePrueba(): jasmine.SpyObj<EstadisticasService> {
    const servicio = jasmine.createSpyObj<EstadisticasService>('EstadisticasService', ['miembros', 'grupos', 'asistencia', 'consolidacion', 'aportes', 'sistema', 'uso', 'miPerfil', 'miAsistencia', 'miCodigoAsistencia']);
    servicio.miembros.and.returnValue(of(miembrosPrueba()));
    servicio.grupos.and.returnValue(of(gruposPrueba()));
    servicio.asistencia.and.returnValue(of(asistenciaPrueba()));
    servicio.consolidacion.and.returnValue(of(consolidacionPrueba()));
    servicio.aportes.and.returnValue(of(aportesPrueba()));
    servicio.sistema.and.returnValue(of(sistemaPrueba()));
    servicio.uso.and.returnValue(of(usoPrueba()));
    servicio.miPerfil.and.returnValue(of(miPerfilPrueba()));
    servicio.miAsistencia.and.returnValue(of(miAsistenciaPrueba()));
    servicio.miCodigoAsistencia.and.returnValue(of({ codigo: '7K2QM9XDR4TB', contenidoQr: 'ASIS:7K2QM9XDR4TB' }));
    return servicio;
}
