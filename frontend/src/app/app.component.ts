import { Component, computed, inject } from '@angular/core';
import { ContextoId, ContextService } from './core/context.service';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
    <main class="min-h-screen bg-slate-950 text-slate-100">
      <header class="mx-auto max-w-3xl px-6 py-8">
        <p class="text-xs uppercase tracking-widest text-slate-400">Contexto activo</p>
        <h1 class="mt-1 text-2xl font-semibold">{{ titulo() }}</h1>
        <p class="mt-2 text-sm text-slate-400">{{ descripcion() }}</p>
        <nav class="mt-6 flex flex-wrap gap-2" aria-label="Cambiar contexto">
          @for (opcion of opciones; track opcion.id) {
            <button
              type="button"
              class="rounded-full px-4 py-1.5 text-sm font-medium"
              [class.bg-emerald-400]="contexto() === opcion.id"
              [class.text-slate-950]="contexto() === opcion.id"
              [class.bg-slate-800]="contexto() !== opcion.id"
              [class.text-slate-100]="contexto() !== opcion.id"
              [attr.aria-pressed]="contexto() === opcion.id"
              (click)="cambiar(opcion.id)">
              {{ opcion.etiqueta }}
            </button>
          }
        </nav>
      </header>
    </main>
  `,
})
export class AppComponent {
  private readonly context = inject(ContextService);

  readonly contexto = this.context.contexto;
  readonly opciones: { id: ContextoId; etiqueta: string }[] = [
    { id: 'REAL_ESTATE', etiqueta: 'Cartera' },
    { id: 'HOME', etiqueta: 'Casa' },
    { id: 'PERSONAL', etiqueta: 'Personal' },
  ];

  readonly titulo = computed(() => {
    switch (this.contexto()) {
      case 'REAL_ESTATE':
        return 'Comunidad de bienes';
      case 'HOME':
        return 'Economía de casa';
      case 'PERSONAL':
        return 'Economía personal';
    }
  });

  readonly descripcion = computed(() => {
    switch (this.contexto()) {
      case 'REAL_ESTATE':
        return 'Inmuebles, contratos y movimientos de la cartera.';
      case 'HOME':
        return 'Ingresos y gastos de la cuenta conjunta.';
      case 'PERSONAL':
        return 'Ocio y gastos personales.';
    }
  });

  cambiar(contexto: ContextoId): void {
    this.context.cambiar(contexto);
  }
}
