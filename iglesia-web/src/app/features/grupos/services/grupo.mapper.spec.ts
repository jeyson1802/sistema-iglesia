import { RESPUESTA_OLIVOS } from '../testing/grupos-prueba';
import { aGrupo } from './grupo.mapper';

describe('aGrupo', () => {
    it('deja la hora como HH:mm', () => {
        expect(aGrupo(RESPUESTA_OLIVOS).horaReunion).toBe('19:30');
    });

    it('sin hora la deja nula', () => {
        expect(aGrupo({ ...RESPUESTA_OLIVOS, horaReunion: null }).horaReunion).toBeNull();
    });
});
