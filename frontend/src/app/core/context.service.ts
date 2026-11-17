import { Injectable, computed, signal } from '@angular/core';

export const CONTEXTOS = ['REAL_ESTATE', 'HOME', 'PERSONAL'] as const;
export type ContextoId = (typeof CONTEXTOS)[number];

export const CONTEXTO_HEADER = 'X-Context-Id';

const STORAGE_KEY = 'gestion.contexto';

@Injectable({ providedIn: 'root' })
export class ContextService {
  private readonly contextoActual = signal<ContextoId>(this.leerInicial());

  readonly contexto = this.contextoActual.asReadonly();
  readonly esInmobiliario = computed(() => this.contextoActual() === 'REAL_ESTATE');
  readonly esHogar = computed(() => this.contextoActual() === 'HOME');
  readonly esPersonal = computed(() => this.contextoActual() === 'PERSONAL');

  cambiar(contexto: ContextoId): void {
    this.contextoActual.set(contexto);
    try {
      localStorage.setItem(STORAGE_KEY, contexto);
    } catch {
      // El contexto sigue en memoria si el almacenamiento no está disponible.
    }
  }

  private leerInicial(): ContextoId {
    try {
      const guardado = localStorage.getItem(STORAGE_KEY);
      if (guardado && (CONTEXTOS as readonly string[]).includes(guardado)) {
        return guardado as ContextoId;
      }
    } catch {
      // Sin almacenamiento, se usa el contexto por defecto.
    }
    return 'REAL_ESTATE';
  }
}
