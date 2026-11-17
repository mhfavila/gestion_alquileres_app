import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ContextoId, ContextService } from './core/context.service';
import { FinanzasService } from './core/finanzas.service';
import { InmobiliariaService } from './core/inmobiliaria.service';
import { Wallet, Categoria, Transaccion, Inmueble, Inquilino, Contrato, TipoTransaccion, TipoInmueble } from './core/models';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
    <main class="min-h-screen bg-slate-950 text-slate-100">
      <header class="mx-auto max-w-6xl px-6 py-8">
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

      <div class="mx-auto max-w-6xl px-6 pb-12">
        <!-- Sección Finanzas (visible en todos los contextos) -->
        <section class="mb-12">
          <h2 class="mb-4 text-xl font-semibold text-emerald-400">Finanzas</h2>
          
          <!-- Wallets -->
          <div class="mb-6 grid grid-cols-1 gap-4 md:grid-cols-3">
            @for (wallet of wallets(); track wallet.id) {
              <div class="rounded-lg bg-slate-800 p-4">
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <h3 class="font-medium">{{ wallet.nombre }}</h3>
                    <p class="text-sm text-slate-400">{{ wallet.contexto }}</p>
                    <p class="mt-2 text-2xl font-bold"
                       [class.text-emerald-400]="calcularSaldo(wallet.id) >= 0"
                       [class.text-red-400]="calcularSaldo(wallet.id) < 0">
                      {{ calcularSaldo(wallet.id).toFixed(2) }}€
                    </p>
                    <p class="mt-1 text-xs text-slate-400">
                      Operativo: {{ calcularSaldoOperativo(wallet.id).toFixed(2) }}€
                    </p>
                  </div>
                  <button
                    type="button"
                    (click)="eliminarWallet(wallet.id)"
                    class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400">
                    🗑️
                  </button>
                </div>
              </div>
            }
            <!-- Formulario nueva wallet -->
            <div class="rounded-lg bg-slate-800 p-4">
              <h3 class="mb-3 font-medium">Nueva wallet</h3>
              <form (ngSubmit)="crearWallet()" class="space-y-2">
                <input
                  type="text"
                  [ngModel]="nuevaWallet().nombre"
                  (ngModelChange)="actualizarWallet('nombre', $event)"
                  placeholder="Nombre de la wallet"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                  required />
                <button
                  type="submit"
                  class="w-full rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                  Crear wallet
                </button>
              </form>
            </div>
          </div>

          <!-- Categorías -->
          <div class="mb-6">
            <div class="mb-3 flex items-center justify-between">
              <h3 class="font-medium">Categorías</h3>
              <button
                type="button"
                (click)="mostrarFormCategoria.set(!mostrarFormCategoria())"
                class="rounded bg-slate-700 px-3 py-1 text-xs hover:bg-slate-600">
                {{ mostrarFormCategoria() ? 'Ocultar' : 'Nueva categoría' }}
              </button>
            </div>
            <div class="mb-4 flex flex-wrap gap-2">
              @for (cat of categorias(); track cat.id) {
                <span class="inline-flex items-center gap-2 rounded-full bg-slate-700 px-3 py-1 text-sm">
                  {{ cat.nombre }}
                  <button
                    type="button"
                    (click)="eliminarCategoria(cat.id)"
                    class="text-red-400 hover:text-red-300">
                    ×
                  </button>
                </span>
              }
            </div>
            @if (mostrarFormCategoria()) {
              <form (ngSubmit)="crearCategoria()" class="rounded-lg bg-slate-800 p-4">
                <div class="flex gap-2">
                  <input
                    type="text"
                    [ngModel]="nuevaCategoria().nombre"
                    (ngModelChange)="actualizarCategoria('nombre', $event)"
                    placeholder="Nombre de categoría"
                    class="flex-1 rounded bg-slate-700 px-3 py-2 text-sm"
                    required />
                  <button
                    type="submit"
                    class="rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                    Crear
                  </button>
                </div>
              </form>
            }
          </div>

          <!-- Traspaso entre contextos -->
          <div class="mb-6 rounded-lg bg-slate-800 p-4">
            <h3 class="mb-3 font-medium">Traspasar entre cuentas</h3>
            <form (ngSubmit)="realizarTraspaso()" class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-5">
              <div>
                <label class="mb-1 block text-xs text-slate-400">Wallet origen</label>
                <select
                  [ngModel]="traspaso().walletOrigenId"
                  (ngModelChange)="actualizarTraspaso('walletOrigenId', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  required>
                  <option value="" disabled>Selecciona wallet...</option>
                  @for (wallet of wallets(); track wallet.id) {
                    <option [value]="wallet.id">{{ wallet.nombre }}</option>
                  }
                </select>
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Contexto destino</label>
                <select
                  [ngModel]="traspaso().contextoDestino"
                  (ngModelChange)="actualizarTraspaso('contextoDestino', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  required>
                  @for (opcion of contextosDestino(); track opcion.id) {
                    <option [value]="opcion.id">{{ opcion.etiqueta }}</option>
                  }
                </select>
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Importe (€)</label>
                <input
                  type="number"
                  [ngModel]="traspaso().importe"
                  (ngModelChange)="actualizarTraspaso('importe', $event)"
                  placeholder="0.00"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  step="0.01"
                  required
                  min="0.01" />
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Fecha</label>
                <input
                  type="date"
                  [ngModel]="traspaso().fecha"
                  (ngModelChange)="actualizarTraspaso('fecha', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  required />
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Concepto (opcional)</label>
                <input
                  type="text"
                  [ngModel]="traspaso().descripcion"
                  (ngModelChange)="actualizarTraspaso('descripcion', $event)"
                  placeholder="Descripción"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white" />
              </div>

              <div class="flex items-end">
                <button
                  type="submit"
                  class="w-full rounded bg-blue-500 px-4 py-2 text-sm font-medium text-white hover:bg-blue-400">
                  Traspasar fondos
                </button>
              </div>
            </form>
          </div>

          <!-- Formulario nueva transacción -->
          <div class="mb-6 rounded-lg bg-slate-800 p-4">
            <h3 class="mb-3 font-medium">Nueva transacción</h3>
            <form (ngSubmit)="crearTransaccion()" class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
              <div>
                <label class="mb-1 block text-xs text-slate-400">Wallet</label>
                <select
                  [ngModel]="nuevaTransaccion().walletId"
                  (ngModelChange)="actualizarTransaccion('walletId', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  required>
                  <option value="" disabled>Selecciona Wallet...</option>
                  @for (wallet of wallets(); track wallet.id) {
                    <option [value]="wallet.id">{{ wallet.nombre }}</option>
                  }
                </select>
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Categoría</label>
                <select
                  [ngModel]="nuevaTransaccion().categoriaId"
                  (ngModelChange)="actualizarTransaccion('categoriaId', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  required>
                  <option value="" disabled>Selecciona Categoría...</option>
                  @for (cat of categorias(); track cat.id) {
                    <option [value]="cat.id">{{ cat.nombre }}</option>
                  }
                </select>
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Tipo</label>
                <select
                  [ngModel]="nuevaTransaccion().tipo"
                  (ngModelChange)="actualizarTransaccion('tipo', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  required>
                  <option value="GASTO">Gasto</option>
                  <option value="INGRESO">Ingreso</option>
                </select>
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Importe (€)</label>
                <input
                  type="number"
                  [ngModel]="nuevaTransaccion().importe"
                  (ngModelChange)="actualizarTransaccion('importe', $event)"
                  placeholder="0.00"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                  step="0.01"
                  required
                  min="0.01" />
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Fecha</label>
                <input
                  type="date"
                  [ngModel]="nuevaTransaccion().fecha"
                  (ngModelChange)="actualizarTransaccion('fecha', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                  required />
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Concepto / Descripción</label>
                <input
                  type="text"
                  [ngModel]="nuevaTransaccion().descripcion"
                  (ngModelChange)="actualizarTransaccion('descripcion', $event)"
                  placeholder="Descripción opcional"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
              </div>

              @if (esInmobiliario()) {
                <div>
                  <label class="mb-1 block text-xs text-slate-400">Inmueble (Opcional)</label>
                  <select
                    [ngModel]="nuevaTransaccion().inmuebleId"
                    (ngModelChange)="actualizarTransaccion('inmuebleId', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white">
                    <option value="">Sin inmueble</option>
                    @for (inm of inmuebles(); track inm.id) {
                      <option [value]="inm.id">{{ inm.direccion }}</option>
                    }
                  </select>
                </div>
              }

              <div class="flex items-end">
                <button
                  type="submit"
                  class="w-full rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                  Añadir transacción
                </button>
              </div>
            </form>
          </div>

          <!-- Transacciones recientes -->
          <div class="rounded-lg bg-slate-800">
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-slate-700">
                    <th class="px-4 py-3 text-left">Fecha</th>
                    <th class="px-4 py-3 text-left">Descripción</th>
                    <th class="px-4 py-3 text-left">Categoría</th>
                    <th class="px-4 py-3 text-right">Importe</th>
                    <th class="px-4 py-3 text-left">Tipo</th>
                  </tr>
                </thead>
                <tbody>
                  @for (trans of transacciones(); track trans.id) {
                    <tr class="border-b border-slate-700">
                      <td class="px-4 py-3">{{ trans.fecha }}</td>
                      <td class="px-4 py-3">
                        {{ trans.descripcion || '-' }}
                        @if (trans.traspasoParId) {
                          <span class="ml-2 rounded bg-blue-500 px-2 py-0.5 text-xs text-white">Traspaso</span>
                        }
                      </td>
                      <td class="px-4 py-3">{{ categoriaNombre(trans.categoriaId) }}</td>
                      <td class="px-4 py-3 text-right font-medium"
                          [class.text-emerald-400]="trans.tipo === 'INGRESO'"
                          [class.text-red-400]="trans.tipo === 'GASTO'">
                        {{ trans.importe.toFixed(2) }}€
                      </td>
                      <td class="px-4 py-3">
                        <span class="rounded-full px-2 py-1 text-xs"
                              [class.bg-emerald-500]="trans.tipo === 'INGRESO'"
                              [class.bg-red-500]="trans.tipo === 'GASTO'">
                          {{ trans.tipo }}
                        </span>
                      </td>
                      <td class="px-4 py-3">
                        <button
                          type="button"
                          (click)="trans.id && eliminarTransaccion(trans.id)"
                          class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400">
                          🗑️
                        </button>
                      </td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="5" class="px-4 py-8 text-center text-slate-400">
                        No hay transacciones
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <!-- Sección Inmobiliaria (solo REAL_ESTATE) -->
        @if (esInmobiliario()) {
          <section>
            <h2 class="mb-4 text-xl font-semibold text-emerald-400">Inmobiliaria</h2>

            <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
              <!-- Inmuebles -->
              <div class="rounded-lg bg-slate-800 p-4">
                <h3 class="mb-3 font-medium">Inmuebles</h3>
                <form (ngSubmit)="crearInmueble()" class="mb-4 space-y-2">
                  <select
                    [ngModel]="nuevoInmueble().tipo"
                    (ngModelChange)="actualizarInmueble('tipo', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                    required>
                    <option value="PISO">Piso</option>
                    <option value="CHALET">Chalet</option>
                    <option value="LOCAL">Local</option>
                    <option value="GARAJE">Garaje</option>
                  </select>
                  <input
                    type="text"
                    [ngModel]="nuevoInmueble().direccion"
                    (ngModelChange)="actualizarInmueble('direccion', $event)"
                    placeholder="Dirección"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    required />
                  <input
                    type="text"
                    [ngModel]="nuevoInmueble().referencia"
                    (ngModelChange)="actualizarInmueble('referencia', $event)"
                    placeholder="Referencia"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <input
                    type="number"
                    [ngModel]="nuevoInmueble().metros"
                    (ngModelChange)="actualizarInmueble('metros', $event)"
                    placeholder="Metros"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    min="1" />
                  <button
                    type="submit"
                    class="w-full rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                    Añadir inmueble
                  </button>
                </form>
                <div class="space-y-2">
                  @for (inm of inmuebles(); track inm.id) {
                    <div class="rounded bg-slate-700 p-2 text-sm">
                      <div class="flex items-start justify-between">
                        <div class="flex-1">
                          <p class="font-medium">{{ inm.direccion }}</p>
                          <p class="text-xs text-slate-400">{{ inm.tipo }} - {{ inm.metros }}m² @if (inm.referencia) { - {{ inm.referencia }} }</p>
                        </div>
                        <button
                          type="button"
                          (click)="inm.id && eliminarInmueble(inm.id)"
                          class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400">
                          🗑️
                        </button>
                      </div>
                    </div>
                  } @empty {
                    <p class="text-xs text-slate-400">No hay inmuebles</p>
                  }
                </div>
              </div>

              <!-- Inquilinos -->
              <div class="rounded-lg bg-slate-800 p-4">
                <h3 class="mb-3 font-medium">Inquilinos</h3>
                <form (ngSubmit)="crearInquilino()" class="mb-4 space-y-2">
                  <input
                    type="text"
                    [ngModel]="nuevoInquilino().nombre"
                    (ngModelChange)="actualizarInquilino('nombre', $event)"
                    placeholder="Nombre"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    required />
                  <input
                    type="email"
                    [ngModel]="nuevoInquilino().email"
                    (ngModelChange)="actualizarInquilino('email', $event)"
                    placeholder="Email"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <input
                    type="tel"
                    [ngModel]="nuevoInquilino().telefono"
                    (ngModelChange)="actualizarInquilino('telefono', $event)"
                    placeholder="Teléfono"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <input
                    type="text"
                    [ngModel]="nuevoInquilino().documento"
                    (ngModelChange)="actualizarInquilino('documento', $event)"
                    placeholder="Documento"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <button
                    type="submit"
                    class="w-full rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                    Añadir inquilino
                  </button>
                </form>
                <div class="space-y-2">
                  @for (inq of inquilinos(); track inq.id) {
                    <div class="rounded bg-slate-700 p-2 text-sm">
                      <div class="flex items-start justify-between">
                        <div class="flex-1">
                          <p class="font-medium">{{ inq.nombre }}</p>
                          <p class="text-xs text-slate-400">{{ inq.email }} @if (inq.telefono) { | {{ inq.telefono }} } @if (inq.documento) { | {{ inq.documento }} }</p>
                        </div>
                        <button
                          type="button"
                          (click)="inq.id && eliminarInquilino(inq.id)"
                          class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400">
                          🗑️
                        </button>
                      </div>
                    </div>
                  } @empty {
                    <p class="text-xs text-slate-400">No hay inquilinos</p>
                  }
                </div>
              </div>

              <!-- Contratos -->
              <div class="rounded-lg bg-slate-800 p-4">
                <h3 class="mb-3 font-medium">Contratos</h3>
                <form (ngSubmit)="crearContrato()" class="mb-4 space-y-2">
                  <select
                    [ngModel]="nuevoContrato().inmuebleId"
                    (ngModelChange)="actualizarContrato('inmuebleId', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                    required>
                    <option value="0">Inmueble</option>
                    @for (inm of inmuebles(); track inm.id) {
                      <option [value]="inm.id">{{ inm.direccion }}</option>
                    }
                  </select>
                  <select
                    [ngModel]="nuevoContrato().inquilinoId"
                    (ngModelChange)="actualizarContrato('inquilinoId', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                    required>
                    <option value="0">Inquilino</option>
                    @for (inq of inquilinos(); track inq.id) {
                      <option [value]="inq.id">{{ inq.nombre }}</option>
                    }
                  </select>
                  <input
                    type="date"
                    [ngModel]="nuevoContrato().fechaInicio"
                    (ngModelChange)="actualizarContrato('fechaInicio', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    required />
                  <input
                    type="date"
                    [ngModel]="nuevoContrato().fechaFin"
                    (ngModelChange)="actualizarContrato('fechaFin', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <input
                    type="number"
                    [ngModel]="nuevoContrato().importeMensual"
                    (ngModelChange)="actualizarContrato('importeMensual', $event)"
                    placeholder="Importe mensual"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    step="0.01"
                    required
                    min="0.01" />
                  <input
                    type="number"
                    [ngModel]="nuevoContrato().fianza"
                    (ngModelChange)="actualizarContrato('fianza', $event)"
                    placeholder="Fianza"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    step="0.01"
                    min="0" />
                  <button
                    type="submit"
                    class="w-full rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                    Añadir contrato
                  </button>
                </form>
                <div class="space-y-2">
                  @for (cont of contratos(); track cont.id) {
                    <div class="rounded bg-slate-700 p-2 text-sm">
                      <div class="flex items-start justify-between">
                        <div class="flex-1">
                          <p class="font-medium">{{ cont.importeMensual }}€/mes - Fianza: {{ cont.fianza }}€</p>
                          <p class="text-xs text-slate-400">{{ cont.fechaInicio }} @if (cont.fechaFin) { - {{ cont.fechaFin }} } @else { - Indefinido }</p>
                          <p class="text-xs text-slate-500">Inmueble: {{ inmuebleNombre(cont.inmuebleId) }} | Inquilino: {{ inquilinoNombre(cont.inquilinoId) }}</p>
                        </div>
                        <button
                          type="button"
                          (click)="cont.id && eliminarContrato(cont.id)"
                          class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400">
                          🗑️
                        </button>
                      </div>
                    </div>
                  } @empty {
                    <p class="text-xs text-slate-400">No hay contratos</p>
                  }
                </div>
              </div>
            </div>
          </section>
        }
      </div>
    </main>
  `,
  imports: [FormsModule],
})
export class AppComponent {
  private readonly context = inject(ContextService);
  private readonly finanzasService = inject(FinanzasService);
  private readonly inmobiliariaService = inject(InmobiliariaService);

  readonly contexto = this.context.contexto;
  readonly esInmobiliario = this.context.esInmobiliario;
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

  readonly contextosDestino = computed(() => {
    const actual = this.contexto();
    return this.opciones.filter((op) => op.id !== actual);
  });

  // Finanzas
  readonly wallets = signal<Wallet[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly transacciones = signal<Transaccion[]>([]);
  readonly nuevaTransaccion = signal<Partial<Transaccion>>({
    tipo: 'GASTO',
    fecha: new Date().toISOString().split('T')[0],
    importe: 0,
    ocrProcesado: false,
  });
  readonly nuevaWallet = signal<Partial<Wallet>>({});
  readonly nuevaCategoria = signal<Partial<Categoria>>({});
  readonly mostrarFormCategoria = signal(false);
  readonly traspaso = signal<{
    walletOrigenId?: number;
    contextoDestino?: ContextoId;
    importe?: number;
    fecha: string;
    descripcion?: string;
  }>({
    fecha: new Date().toISOString().split('T')[0],
  });

  // Inmobiliaria
  readonly inmuebles = signal<Inmueble[]>([]);
  readonly inquilinos = signal<Inquilino[]>([]);
  readonly contratos = signal<Contrato[]>([]);
  readonly nuevoInmueble = signal<Partial<Inmueble>>({ tipo: 'PISO' });
  readonly nuevoInquilino = signal<Partial<Inquilino>>({});
  readonly nuevoContrato = signal<Partial<Contrato>>({ fianza: 0 });

  ngOnInit(): void {
    this.cargarDatos();
    effect(() => {
      this.context.contexto();
      this.cargarDatos();
    });
  }

  cambiar(contexto: ContextoId): void {
    this.context.cambiar(contexto);
  }

  private cargarDatos(): void {
    this.finanzasService.getWallets().subscribe((data) => {
      this.wallets.set(data);
      if (data.length > 0 && !this.nuevaTransaccion().walletId) {
        this.actualizarTransaccion('walletId', data[0].id);
      }
    });
    this.finanzasService.getCategorias().subscribe((data) => {
      this.categorias.set(data);
      if (data.length > 0 && !this.nuevaTransaccion().categoriaId) {
        this.actualizarTransaccion('categoriaId', data[0].id);
      }
    });
    this.finanzasService.getTransacciones().subscribe((data) => this.transacciones.set(data));

    if (this.esInmobiliario()) {
      this.inmobiliariaService.getInmuebles().subscribe((data) => this.inmuebles.set(data));
      this.inmobiliariaService.getInquilinos().subscribe((data) => this.inquilinos.set(data));
      this.inmobiliariaService.getContratos().subscribe((data) => this.contratos.set(data));
    } else {
      this.inmuebles.set([]);
      this.inquilinos.set([]);
      this.contratos.set([]);
    }
  }

  // Finanzas
  actualizarTransaccion(campo: keyof Transaccion, valor: any): void {
    this.nuevaTransaccion.update((t) => ({ ...t, [campo]: valor }));
  }

  crearTransaccion(): void {
    const trans = this.nuevaTransaccion();
    if (!trans.walletId || !trans.categoriaId || !trans.importe || !trans.fecha) return;

    const payload: Omit<Transaccion, 'id'> = {
      importe: trans.importe,
      tipo: trans.tipo as TipoTransaccion,
      fecha: trans.fecha!,
      descripcion: trans.descripcion,
      contexto: this.contexto(),
      walletId: trans.walletId,
      categoriaId: trans.categoriaId,
      inmuebleId: trans.inmuebleId,
      contratoId: trans.contratoId,
      urlComprobante: trans.urlComprobante,
      ocrProcesado: false,
    };

    this.finanzasService.createTransaccion(payload).subscribe(() => {
      this.cargarDatos();
      this.nuevaTransaccion.set({
        tipo: 'GASTO',
        fecha: new Date().toISOString().split('T')[0],
        importe: 0,
        ocrProcesado: false,
      });
    });
  }

  categoriaNombre(id: number): string {
    return this.categorias().find((c) => c.id === id)?.nombre || '-';
  }

  inmuebleNombre(id: number): string {
    return this.inmuebles().find((i) => i.id === id)?.direccion || 'Desconocido';
  }

  inquilinoNombre(id: number): string {
    return this.inquilinos().find((i) => i.id === id)?.nombre || 'Desconocido';
  }

  calcularSaldo(walletId: number): number {
    return this.transacciones()
      .filter((t) => t.walletId === walletId)
      .reduce((acc, t) => {
        return acc + (t.tipo === 'INGRESO' ? t.importe : -t.importe);
      }, 0);
  }

  calcularSaldoOperativo(walletId: number): number {
    return this.transacciones()
      .filter((t) => t.walletId === walletId)
      .filter((t) => {
        const cat = this.categorias().find((c) => c.id === t.categoriaId);
        return cat?.nombre !== 'Traspaso Interno';
      })
      .reduce((acc, t) => {
        return acc + (t.tipo === 'INGRESO' ? t.importe : -t.importe);
      }, 0);
  }

  actualizarTraspaso(campo: string, valor: any): void {
    this.traspaso.update((t) => ({ ...t, [campo]: valor }));
  }

  realizarTraspaso(): void {
    const traspaso = this.traspaso();
    if (!traspaso.walletOrigenId || !traspaso.contextoDestino || !traspaso.importe || !traspaso.fecha) return;

    this.finanzasService.realizarTraspaso({
      walletOrigenId: traspaso.walletOrigenId,
      contextoDestino: traspaso.contextoDestino,
      importe: traspaso.importe,
      fecha: traspaso.fecha,
      descripcion: traspaso.descripcion,
    }).subscribe(() => {
      this.cargarDatos();
      this.traspaso.set({
        fecha: new Date().toISOString().split('T')[0],
      });
    });
  }

  eliminarWallet(id: number): void {
    if (confirm('¿Estás seguro de eliminar esta wallet?')) {
      this.finanzasService.deleteWallet(id).subscribe(() => {
        this.cargarDatos();
      });
    }
  }

  eliminarCategoria(id: number): void {
    if (confirm('¿Estás seguro de eliminar esta categoría?')) {
      this.finanzasService.deleteCategoria(id).subscribe(() => {
        this.cargarDatos();
      });
    }
  }

  eliminarTransaccion(id: number): void {
    if (confirm('¿Estás seguro de eliminar esta transacción? Si es un traspaso, se eliminará también la transacción espejo.')) {
      this.finanzasService.deleteTransaccion(id).subscribe(() => {
        this.cargarDatos();
      });
    }
  }

  eliminarInmueble(id: number): void {
    if (confirm('¿Estás seguro de eliminar este inmueble?')) {
      this.inmobiliariaService.deleteInmueble(id).subscribe(() => {
        this.cargarDatos();
      });
    }
  }

  eliminarInquilino(id: number): void {
    if (confirm('¿Estás seguro de eliminar este inquilino?')) {
      this.inmobiliariaService.deleteInquilino(id).subscribe(() => {
        this.cargarDatos();
      });
    }
  }

  eliminarContrato(id: number): void {
    if (confirm('¿Estás seguro de eliminar este contrato?')) {
      this.inmobiliariaService.deleteContrato(id).subscribe(() => {
        this.cargarDatos();
      });
    }
  }

  actualizarWallet(campo: keyof Wallet, valor: any): void {
    this.nuevaWallet.update((w) => ({ ...w, [campo]: valor }));
  }

  crearWallet(): void {
    const wallet = this.nuevaWallet();
    if (!wallet.nombre) return;

    const payload: Omit<Wallet, 'id'> = {
      nombre: wallet.nombre!,
      contexto: this.contexto(),
    };

    this.finanzasService.createWallet(payload).subscribe(() => {
      this.cargarDatos();
      this.nuevaWallet.set({});
    });
  }

  actualizarCategoria(campo: keyof Categoria, valor: any): void {
    this.nuevaCategoria.update((c) => ({ ...c, [campo]: valor }));
  }

  crearCategoria(): void {
    const cat = this.nuevaCategoria();
    if (!cat.nombre) return;

    const payload: Omit<Categoria, 'id'> = {
      nombre: cat.nombre!,
      contexto: this.contexto(),
    };

    this.finanzasService.createCategoria(payload).subscribe(() => {
      this.cargarDatos();
      this.nuevaCategoria.set({});
      this.mostrarFormCategoria.set(false);
    });
  }

  // Inmobiliaria
  actualizarInmueble(campo: keyof Inmueble, valor: any): void {
    this.nuevoInmueble.update((i) => ({ ...i, [campo]: valor }));
  }

  crearInmueble(): void {
    const inm = this.nuevoInmueble();
    if (!inm.tipo || !inm.direccion) return;

    const payload: Omit<Inmueble, 'id'> = {
      tipo: inm.tipo as TipoInmueble,
      direccion: inm.direccion!,
      referencia: inm.referencia,
      metros: inm.metros,
      contexto: this.contexto(),
    };

    this.inmobiliariaService.createInmueble(payload).subscribe(() => {
      this.cargarDatos();
      this.nuevoInmueble.set({ tipo: 'PISO' });
    });
  }

  actualizarInquilino(campo: keyof Inquilino, valor: any): void {
    this.nuevoInquilino.update((i) => ({ ...i, [campo]: valor }));
  }

  crearInquilino(): void {
    const inq = this.nuevoInquilino();
    if (!inq.nombre) return;

    const payload: Omit<Inquilino, 'id'> = {
      nombre: inq.nombre!,
      email: inq.email,
      telefono: inq.telefono,
      documento: inq.documento,
      contexto: this.contexto(),
    };

    this.inmobiliariaService.createInquilino(payload).subscribe(() => {
      this.cargarDatos();
      this.nuevoInquilino.set({});
    });
  }

  actualizarContrato(campo: keyof Contrato, valor: any): void {
    this.nuevoContrato.update((c) => ({ ...c, [campo]: valor }));
  }

  crearContrato(): void {
    const cont = this.nuevoContrato();
    if (!cont.inmuebleId || !cont.inquilinoId || !cont.fechaInicio || !cont.importeMensual) return;

    const payload: Omit<Contrato, 'id'> = {
      inmuebleId: cont.inmuebleId!,
      inquilinoId: cont.inquilinoId!,
      fechaInicio: cont.fechaInicio!,
      fechaFin: cont.fechaFin,
      importeMensual: cont.importeMensual!,
      fianza: cont.fianza || 0,
      contexto: this.contexto(),
    };

    this.inmobiliariaService.createContrato(payload).subscribe(() => {
      this.cargarDatos();
      this.nuevoContrato.set({ fianza: 0 });
    });
  }
}
