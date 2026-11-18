import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ContextoId, ContextService } from './core/context.service';
import { FinanzasService } from './core/finanzas.service';
import { InmobiliariaService } from './core/inmobiliaria.service';
import { Wallet, Categoria, Transaccion, Inmueble, Inquilino, Contrato } from './core/models';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `
    <main class="min-h-screen bg-slate-950 text-slate-100">
      <header class="mx-auto max-w-6xl px-6 py-8">
        <p class="text-xs uppercase tracking-widest text-slate-400">Contexto activo</p>
        <h1 class="mt-1 text-2xl font-semibold">{{ titulo() }}</h1>
        <p class="mt-2 text-sm text-slate-400">{{ descripcion() }}</p>
        <div class="mt-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <nav class="flex flex-wrap gap-2" aria-label="Cambiar contexto">
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

          <!-- Barra de filtro de periodo temporal global -->
          <div class="flex flex-wrap items-center gap-2 rounded-lg bg-slate-900 px-3 py-2 border border-slate-800" aria-label="Filtro de periodo temporal">
            <span class="text-xs font-medium uppercase tracking-wider text-slate-400">Periodo:</span>
            <select
              name="modoPeriodo"
              [ngModel]="modoPeriodo()"
              (ngModelChange)="modoPeriodo.set($event)"
              class="rounded bg-slate-800 px-3 py-1.5 text-sm text-slate-100 border border-slate-700">
              <option value="MENSUAL">Mes actual / Mensual</option>
              <option value="ANUAL">Anual</option>
              <option value="HISTORICO">Todo el histórico</option>
            </select>

            @if (modoPeriodo() === 'MENSUAL') {
              <input
                type="month"
                name="mesSeleccionado"
                [ngModel]="mesSeleccionado()"
                (ngModelChange)="mesSeleccionado.set($event)"
                class="rounded bg-slate-800 px-3 py-1.5 text-sm text-slate-100 border border-slate-700" />
            }

            @if (modoPeriodo() === 'ANUAL') {
              <select
                name="anioSeleccionado"
                [ngModel]="anioSeleccionado()"
                (ngModelChange)="anioSeleccionado.set($event)"
                class="rounded bg-slate-800 px-3 py-1.5 text-sm text-slate-100 border border-slate-700">
                @for (anio of aniosDisponibles(); track anio) {
                  <option [value]="anio">{{ anio }}</option>
                }
              </select>
            }
          </div>
        </div>
      </header>

      <div class="mx-auto max-w-6xl px-6 pb-12">
        <!-- Sección Finanzas (visible en todos los contextos) -->
        <section class="mb-12">
          <h2 class="mb-4 text-xl font-semibold text-emerald-400">Finanzas</h2>
          
          <!-- Wallet principal (una por contexto) -->
          <div class="mb-6">
            @for (wallet of wallets(); track wallet.id) {
              <div class="rounded-lg bg-slate-800 p-4">
                <div class="flex items-start justify-between">
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <h3 class="font-medium">{{ wallet.nombre }}</h3>
                      @if ($first) {
                        <span class="rounded-full bg-emerald-500/20 px-2 py-0.5 text-xs text-emerald-300">Wallet principal</span>
                      }
                    </div>
                    <p class="text-sm text-slate-400">{{ wallet.contexto }}</p>
                    <p class="mt-2 text-2xl font-bold"
                       [class.text-emerald-400]="calcularSaldo(wallet.id) >= 0"
                       [class.text-red-400]="calcularSaldo(wallet.id) < 0">
                      {{ calcularSaldo(wallet.id).toFixed(2) }}€
                    </p>
                    <p class="mt-1 text-xs text-slate-400">
                      Operativo ({{ etiquetaPeriodo() }}): {{ calcularSaldoOperativo(wallet.id).toFixed(2) }}€
                    </p>
                  </div>
                </div>
              </div>
            } @empty {
              <div class="rounded-lg bg-slate-800 p-4 text-sm text-slate-400">
                No hay wallet configurada para este contexto.
              </div>
            }
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
                <span class="inline-flex items-center gap-2 rounded-full bg-slate-700 px-3 py-1 text-sm"
                      title="Total acumulado de la categoría">
                  {{ cat.nombre }}
                  <span class="rounded-full bg-slate-900 px-2 py-0.5 text-xs"
                        [class.text-emerald-300]="totalCategoria(cat.id) >= 0"
                        [class.text-red-300]="totalCategoria(cat.id) < 0">
                    {{ totalCategoria(cat.id).toFixed(2) }}€
                  </span>
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
                    name="categoriaNombre"
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
            <form (ngSubmit)="realizarTraspaso()" class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
              <div>
                <label class="mb-1 block text-xs text-slate-400">Wallet origen</label>
                <div class="rounded bg-slate-700 px-3 py-2 text-sm text-slate-300">
                  {{ wallets().at(0)?.nombre || 'Sin wallet' }}
                </div>
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Contexto destino</label>
                <select
                  [ngModel]="traspaso().contextoDestino"
                  name="contextoDestino"
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
                  name="importeTraspaso"
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
                  name="fechaTraspaso"
                  (ngModelChange)="actualizarTraspaso('fecha', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  required />
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Concepto (opcional)</label>
                <input
                  type="text"
                  [ngModel]="traspaso().descripcion"
                  name="descripcionTraspaso"
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
                <div class="rounded bg-slate-700 px-3 py-2 text-sm text-slate-300">
                  {{ wallets().at(0)?.nombre || 'Sin wallet' }}
                </div>
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Categoría</label>
                <select
                  [ngModel]="nuevaTransaccion().categoriaId"
                  name="txCategoriaId"
                  (ngModelChange)="actualizarTransaccion('categoriaId', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  [disabled]="editandoTraspaso()"
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
                  name="txTipo"
                  (ngModelChange)="actualizarTransaccion('tipo', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                  [disabled]="editandoTraspaso()"
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
                  name="txImporte"
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
                  name="txFecha"
                  (ngModelChange)="actualizarTransaccion('fecha', $event)"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                  required />
              </div>

              <div>
                <label class="mb-1 block text-xs text-slate-400">Concepto / Descripción</label>
                <input
                  type="text"
                  [ngModel]="nuevaTransaccion().descripcion"
                  name="txDescripcion"
                  (ngModelChange)="actualizarTransaccion('descripcion', $event)"
                  placeholder="Descripción opcional"
                  class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
              </div>

              @if (esInmobiliario()) {
                <div>
                  <label class="mb-1 block text-xs text-slate-400">Contrato (opcional)</label>
                  <select
                    [ngModel]="nuevaTransaccion().contratoId ?? ''"
                    name="txContratoId"
                    (ngModelChange)="onContratoSeleccionado($event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                    [disabled]="editandoTraspaso()">
                    <option value="">Sin contrato</option>
                    @for (cont of contratos(); track cont.id) {
                      <option [value]="cont.id">
                        {{ inmuebleNombre(cont.inmuebleId) }} — {{ inquilinoNombre(cont.inquilinoId) }} ({{ cont.importeMensual }}€)
                      </option>
                    }
                  </select>
                </div>
                <div>
                  <label class="mb-1 block text-xs text-slate-400">Inmueble (Opcional)</label>
                  <select
                    [ngModel]="nuevaTransaccion().inmuebleId ?? ''"
                    name="txInmuebleId"
                    (ngModelChange)="actualizarTransaccion('inmuebleId', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm text-white"
                    [disabled]="editandoTraspaso()">
                    <option value="">Sin inmueble</option>
                    @for (inm of inmuebles(); track inm.id) {
                      <option [value]="inm.id">{{ inm.direccion }}</option>
                    }
                  </select>
                </div>
              }

              @if (editandoTraspaso()) {
                <div class="col-span-full rounded bg-amber-500/10 px-3 py-2 text-xs text-amber-300">
                  Editando traspaso vinculado: el importe y la fecha se actualizarán automáticamente en ambos contextos.
                </div>
              }
              <div class="flex items-end gap-2">
                <button
                  type="submit"
                  class="w-full rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                  {{ editandoTransaccionId() ? 'Guardar cambios' : 'Añadir transacción' }}
                </button>
                @if (editandoTransaccionId()) {
                  <button
                    type="button"
                    (click)="cancelarEdicionTransaccion()"
                    class="rounded bg-slate-700 px-4 py-2 text-sm font-medium text-slate-200 hover:bg-slate-600">
                    Cancelar
                  </button>
                }
              </div>
            </form>
          </div>

          <!-- Transacciones recientes -->
          <div class="rounded-lg bg-slate-800">
            <div class="flex items-center justify-between border-b border-slate-700 px-4 py-3">
              <h3 class="font-medium text-sm">Transacciones ({{ etiquetaPeriodo() }})</h3>
              <span class="rounded-full bg-slate-700 px-2.5 py-0.5 text-xs text-slate-300">
                {{ transaccionesFiltradas().length }} {{ transaccionesFiltradas().length === 1 ? 'movimiento' : 'movimientos' }}
              </span>
            </div>
            <div class="overflow-x-auto">
              <table class="w-full text-sm">
                <thead>
                  <tr class="border-b border-slate-700">
                    <th class="px-4 py-3 text-left">Fecha</th>
                    <th class="px-4 py-3 text-left">Descripción</th>
                    <th class="px-4 py-3 text-left">Categoría</th>
                    <th class="px-4 py-3 text-right">Importe</th>
                    <th class="px-4 py-3 text-left">Tipo</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  @for (trans of transaccionesFiltradas(); track trans.id) {
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
                      <td class="px-4 py-3 text-right">
                        <button
                          type="button"
                          (click)="trans.id && editarTransaccion(trans)"
                          class="mr-1 rounded bg-amber-500 px-2 py-1 text-xs text-slate-950 hover:bg-amber-400"
                          [title]="esTraspaso(trans) ? 'Editar traspaso (se actualizará en ambos contextos)' : 'Editar transacción'">
                          ✏️
                        </button>
                        <button
                          type="button"
                          (click)="trans.id && eliminarTransaccion(trans.id)"
                          class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400"
                          title="Eliminar transacción">
                          🗑️
                        </button>
                      </td>
                    </tr>
                  } @empty {
                    <tr>
                      <td colspan="6" class="px-4 py-8 text-center text-slate-400">
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
                    name="inmuebleTipo"
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
                    name="inmuebleDireccion"
                    (ngModelChange)="actualizarInmueble('direccion', $event)"
                    placeholder="Dirección"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    required />
                  <input
                    type="text"
                    [ngModel]="nuevoInmueble().referencia"
                    name="inmuebleReferencia"
                    (ngModelChange)="actualizarInmueble('referencia', $event)"
                    placeholder="Referencia"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <input
                    type="number"
                    [ngModel]="nuevoInmueble().metros"
                    name="inmuebleMetros"
                    (ngModelChange)="actualizarInmueble('metros', $event)"
                    placeholder="Metros"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    min="1" />
                  <div class="flex gap-2">
                    <button
                      type="submit"
                      class="flex-1 rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                      {{ editandoInmuebleId() ? 'Guardar cambios' : 'Añadir inmueble' }}
                    </button>
                    @if (editandoInmuebleId()) {
                      <button
                        type="button"
                        (click)="cancelarEdicionInmueble()"
                        class="rounded bg-slate-700 px-3 py-2 text-sm text-slate-200 hover:bg-slate-600">
                        Cancelar
                      </button>
                    }
                  </div>
                </form>
                <div class="space-y-2">
                  @for (inm of inmuebles(); track inm.id) {
                    <div class="rounded bg-slate-700 p-2 text-sm">
                      <div class="flex items-start justify-between">
                        <div class="flex-1">
                          <p class="font-medium">{{ inm.direccion }}</p>
                          <p class="text-xs text-slate-400">{{ inm.tipo }} - {{ inm.metros }}m² @if (inm.referencia) { - {{ inm.referencia }} }</p>
                          <div class="mt-1 flex gap-3 text-xs">
                            <span class="text-emerald-300">+{{ ingresosInmueble(inm.id ?? 0).toFixed(2) }}€</span>
                            <span class="text-red-300">-{{ gastosInmueble(inm.id ?? 0).toFixed(2) }}€</span>
                            <span class="font-medium"
                                  [class.text-emerald-300]="rendimientoInmueble(inm.id ?? 0) >= 0"
                                  [class.text-red-300]="rendimientoInmueble(inm.id ?? 0) < 0">
                              Neto: {{ rendimientoInmueble(inm.id ?? 0).toFixed(2) }}€
                            </span>
                          </div>
                        </div>
                        <div class="flex gap-1">
                          <button
                            type="button"
                            (click)="inm.id && editarInmueble(inm)"
                            class="rounded bg-amber-500 px-2 py-1 text-xs text-slate-950 hover:bg-amber-400"
                            title="Editar inmueble">
                            ✏️
                          </button>
                          <button
                            type="button"
                            (click)="inm.id && eliminarInmueble(inm.id)"
                            class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400"
                            title="Eliminar inmueble">
                            🗑️
                          </button>
                        </div>
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
                    name="inquilinoNombre"
                    (ngModelChange)="actualizarInquilino('nombre', $event)"
                    placeholder="Nombre"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    required />
                  <input
                    type="email"
                    [ngModel]="nuevoInquilino().email"
                    name="inquilinoEmail"
                    (ngModelChange)="actualizarInquilino('email', $event)"
                    placeholder="Email"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <input
                    type="tel"
                    [ngModel]="nuevoInquilino().telefono"
                    name="inquilinoTelefono"
                    (ngModelChange)="actualizarInquilino('telefono', $event)"
                    placeholder="Teléfono"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <input
                    type="text"
                    [ngModel]="nuevoInquilino().documento"
                    name="inquilinoDocumento"
                    (ngModelChange)="actualizarInquilino('documento', $event)"
                    placeholder="Documento"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <div class="flex gap-2">
                    <button
                      type="submit"
                      class="flex-1 rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                      {{ editandoInquilinoId() ? 'Guardar cambios' : 'Añadir inquilino' }}
                    </button>
                    @if (editandoInquilinoId()) {
                      <button
                        type="button"
                        (click)="cancelarEdicionInquilino()"
                        class="rounded bg-slate-700 px-3 py-2 text-sm text-slate-200 hover:bg-slate-600">
                        Cancelar
                      </button>
                    }
                  </div>
                </form>
                <div class="space-y-2">
                  @for (inq of inquilinos(); track inq.id) {
                    <div class="rounded bg-slate-700 p-2 text-sm">
                      <div class="flex items-start justify-between">
                        <div class="flex-1">
                          <p class="font-medium">{{ inq.nombre }}</p>
                          <p class="text-xs text-slate-400">{{ inq.email }} @if (inq.telefono) { | {{ inq.telefono }} } @if (inq.documento) { | {{ inq.documento }} }</p>
                        </div>
                        <div class="flex gap-1">
                          <button
                            type="button"
                            (click)="inq.id && editarInquilino(inq)"
                            class="rounded bg-amber-500 px-2 py-1 text-xs text-slate-950 hover:bg-amber-400"
                            title="Editar inquilino">
                            ✏️
                          </button>
                          <button
                            type="button"
                            (click)="inq.id && eliminarInquilino(inq.id)"
                            class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400"
                            title="Eliminar inquilino">
                            🗑️
                          </button>
                        </div>
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
                    name="contratoInmuebleId"
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
                    name="contratoInquilinoId"
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
                    name="contratoFechaInicio"
                    (ngModelChange)="actualizarContrato('fechaInicio', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    required />
                  <input
                    type="date"
                    [ngModel]="nuevoContrato().fechaFin"
                    name="contratoFechaFin"
                    (ngModelChange)="actualizarContrato('fechaFin', $event)"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm" />
                  <input
                    type="number"
                    [ngModel]="nuevoContrato().importeMensual"
                    name="contratoImporteMensual"
                    (ngModelChange)="actualizarContrato('importeMensual', $event)"
                    placeholder="Importe mensual"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    step="0.01"
                    required
                    min="0.01" />
                  <input
                    type="number"
                    [ngModel]="nuevoContrato().fianza"
                    name="contratoFianza"
                    (ngModelChange)="actualizarContrato('fianza', $event)"
                    placeholder="Fianza"
                    class="w-full rounded bg-slate-700 px-3 py-2 text-sm"
                    step="0.01"
                    min="0" />
                  <div class="flex gap-2">
                    <button
                      type="submit"
                      class="flex-1 rounded bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 hover:bg-emerald-400">
                      {{ editandoContratoId() ? 'Guardar cambios' : 'Añadir contrato' }}
                    </button>
                    @if (editandoContratoId()) {
                      <button
                        type="button"
                        (click)="cancelarEdicionContrato()"
                        class="rounded bg-slate-700 px-3 py-2 text-sm text-slate-200 hover:bg-slate-600">
                        Cancelar
                      </button>
                    }
                  </div>
                </form>
                <div class="space-y-2">
                  @for (cont of contratos(); track cont.id) {
                    <div class="rounded bg-slate-700 p-2 text-sm">
                      <div class="flex items-start justify-between">
                        <div class="flex-1">
                          <p class="font-medium">{{ cont.importeMensual }}€/mes - Fianza: {{ cont.fianza }}€</p>
                          <p class="text-xs text-slate-400">{{ cont.fechaInicio }} @if (cont.fechaFin) { - {{ cont.fechaFin }} } @else { - Indefinido }</p>
                          <p class="text-xs text-slate-500">Inmueble: {{ inmuebleNombre(cont.inmuebleId) }} | Inquilino: {{ inquilinoNombre(cont.inquilinoId) }}</p>
                          @if (contratoCobradoEsteMes(cont)) {
                            <span class="mt-1 inline-block rounded-full bg-emerald-500/20 px-2 py-0.5 text-xs text-emerald-300">
                              COBRADO ESTE MES ({{ mesReferenciaCobro() }})
                            </span>
                          } @else {
                            <span class="mt-1 inline-block rounded-full bg-amber-500/20 px-2 py-0.5 text-xs text-amber-300">
                              PENDIENTE DE COBRO ({{ mesReferenciaCobro() }})
                            </span>
                          }
                          @if (!contratoCobradoEsteMes(cont) && cont.id) {
                            <button
                              type="button"
                              (click)="cobrarMes(cont)"
                              class="ml-2 mt-1 rounded bg-emerald-500 px-2 py-0.5 text-xs font-medium text-slate-950 hover:bg-emerald-400">
                              Cobrar mes (+{{ cont.importeMensual }} €)
                            </button>
                          }
                        </div>
                        <div class="flex gap-1">
                          <button
                            type="button"
                            (click)="cont.id && editarContrato(cont)"
                            class="rounded bg-amber-500 px-2 py-1 text-xs text-slate-950 hover:bg-amber-400"
                            title="Editar contrato">
                            ✏️
                          </button>
                          <button
                            type="button"
                            (click)="cont.id && eliminarContrato(cont.id)"
                            class="rounded bg-red-500 px-2 py-1 text-xs text-white hover:bg-red-400"
                            title="Eliminar contrato">
                            🗑️
                          </button>
                        </div>
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

  // Filtro de periodo temporal global
  readonly modoPeriodo = signal<'MENSUAL' | 'ANUAL' | 'HISTORICO'>('MENSUAL');
  readonly mesSeleccionado = signal<string>(new Date().toISOString().slice(0, 7));
  readonly anioSeleccionado = signal<string>(String(new Date().getFullYear()));

  readonly aniosDisponibles = computed(() => {
    const base = new Set<string>(['2025', '2026', '2027', String(new Date().getFullYear())]);
    for (const t of this.transacciones()) {
      if (t.fecha && t.fecha.length >= 4) {
        base.add(t.fecha.slice(0, 4));
      }
    }
    return Array.from(base).sort();
  });

  readonly transaccionesFiltradas = computed(() => {
    const todas = this.transacciones();
    const modo = this.modoPeriodo();
    if (modo === 'MENSUAL') {
      const mes = this.mesSeleccionado();
      return mes ? todas.filter((t) => t.fecha.startsWith(mes)) : todas;
    }
    if (modo === 'ANUAL') {
      const anio = this.anioSeleccionado();
      return anio ? todas.filter((t) => t.fecha.startsWith(anio)) : todas;
    }
    return todas;
  });

  readonly etiquetaPeriodo = computed(() => {
    const modo = this.modoPeriodo();
    if (modo === 'MENSUAL') return this.mesSeleccionado() || 'Mensual';
    if (modo === 'ANUAL') return `Año ${this.anioSeleccionado()}`;
    return 'Histórico';
  });

  readonly mesReferenciaCobro = computed(() => {
    if (this.modoPeriodo() === 'MENSUAL' && this.mesSeleccionado()) {
      return this.mesSeleccionado();
    }
    const ahora = new Date();
    return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`;
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

  // Modo edición (IDs en edición o null)
  readonly editandoTransaccionId = signal<number | null>(null);
  readonly editandoInmuebleId = signal<number | null>(null);
  readonly editandoInquilinoId = signal<number | null>(null);
  readonly editandoContratoId = signal<number | null>(null);

  readonly editandoTraspaso = computed(() => {
    const id = this.editandoTransaccionId();
    if (!id) return false;
    return this.transacciones().some((t) => t.id === id && !!t.traspasoParId);
  });

  esTraspaso(trans: Transaccion): boolean {
    return !!trans.traspasoParId;
  }

  constructor() {
    effect(() => {
      this.context.contexto();
      this.cargarDatos();
    }, { allowSignalWrites: true });
  }

  ngOnInit(): void {
    this.cargarDatos();
  }

  cambiar(contexto: ContextoId): void {
    this.context.cambiar(contexto);
  }

  private cargarDatos(): void {
    this.finanzasService.getWallets().subscribe((data) => {
      this.wallets.set(data);
      if (data.length > 0) {
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

  onContratoSeleccionado(valor: any): void {
    const id = valor === '' || valor === null ? null : Number(valor);
    this.nuevaTransaccion.update((t) => ({ ...t, contratoId: id ?? undefined }));
    if (id) {
      const contrato = this.contratos().find((c) => c.id === id);
      if (contrato) {
        this.nuevaTransaccion.update((t) => ({
          ...t,
          inmuebleId: contrato.inmuebleId,
          importe: Number(contrato.importeMensual),
        }));
      }
    }
  }

  editarTransaccion(trans: Transaccion): void {
    this.nuevaTransaccion.set({
      ...trans,
      contratoId: trans.contratoId ?? undefined,
    });
    this.editandoTransaccionId.set(trans.id ?? null);
    this.mostrarFormCategoria.set(false);
  }

  cancelarEdicionTransaccion(): void {
    this.editandoTransaccionId.set(null);
    this.nuevaTransaccion.set({
      tipo: 'GASTO',
      fecha: new Date().toISOString().split('T')[0],
      importe: 0,
      ocrProcesado: false,
    });
  }

  crearTransaccion(): void {
    const trans = this.nuevaTransaccion();
    const walletId = trans.walletId || this.wallets()[0]?.id;
    const categoriaId = trans.categoriaId;
    if (!walletId || !categoriaId || !trans.importe || !trans.fecha) return;

    const payload: Record<string, unknown> = {
      importe: trans.importe,
      tipo: trans.tipo,
      fecha: trans.fecha,
      descripcion: trans.descripcion,
      contexto: this.contexto(),
      wallet: { id: walletId },
      categoria: { id: categoriaId },
      inmueble: trans.inmuebleId ? { id: trans.inmuebleId } : null,
      contrato: trans.contratoId ? { id: trans.contratoId } : null,
      urlComprobante: trans.urlComprobante,
      ocrProcesado: false,
    };

    const editId = this.editandoTransaccionId();
    console.log(editId ? `Enviando actualización transacción ID ${editId}:` : 'Enviando datos:', payload);
    const peticion = editId
      ? this.finanzasService.actualizarTransaccion(editId, payload)
      : this.finanzasService.createTransaccion(payload);

    peticion.subscribe({
      next: (res) => {
        console.log('Respuesta recibida:', res);
        this.editandoTransaccionId.set(null);
        this.cargarDatos();
        this.nuevaTransaccion.set({
          tipo: 'GASTO',
          fecha: new Date().toISOString().split('T')[0],
          importe: 0,
          ocrProcesado: false,
        });
      },
      error: (err) => console.error(editId ? 'Error al actualizar transacción:' : 'Error al crear transacción:', err),
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
    return this.transaccionesFiltradas()
      .filter((t) => t.walletId === walletId)
      .filter((t) => {
        const cat = this.categorias().find((c) => c.id === t.categoriaId);
        return cat?.nombre !== 'Traspaso Interno';
      })
      .reduce((acc, t) => {
        return acc + (t.tipo === 'INGRESO' ? t.importe : -t.importe);
      }, 0);
  }

  totalCategoria(categoriaId: number): number {
    return this.transaccionesFiltradas()
      .filter((t) => t.categoriaId === categoriaId)
      .reduce((acc, t) => acc + (t.tipo === 'INGRESO' ? t.importe : -t.importe), 0);
  }

  private transaccionesInmueble(inmuebleId: number) {
    return this.transaccionesFiltradas().filter((t) => t.inmuebleId === inmuebleId);
  }

  ingresosInmueble(inmuebleId: number): number {
    return this.transaccionesInmueble(inmuebleId)
      .filter((t) => t.tipo === 'INGRESO')
      .reduce((acc, t) => acc + t.importe, 0);
  }

  gastosInmueble(inmuebleId: number): number {
    return this.transaccionesInmueble(inmuebleId)
      .filter((t) => t.tipo === 'GASTO')
      .reduce((acc, t) => acc + t.importe, 0);
  }

  rendimientoInmueble(inmuebleId: number): number {
    return this.ingresosInmueble(inmuebleId) - this.gastosInmueble(inmuebleId);
  }

  contratoCobradoEsteMes(contrato: Contrato): boolean {
    if (!contrato.id) return false;
    const prefijo = this.mesReferenciaCobro();
    return this.transacciones().some(
      (t) => t.tipo === 'INGRESO' && t.contratoId === contrato.id && t.fecha.startsWith(prefijo)
    );
  }

  cobrarMes(contrato: Contrato): void {
    const walletId = this.wallets()[0]?.id;
    if (!walletId || !contrato.id) return;

    const categoria =
      this.categorias().find((c) => c.nombre.toLowerCase() === 'alquiler mensual') ||
      this.categorias()[0];
    if (!categoria) {
      console.error('No hay categorías disponibles para registrar el cobro');
      return;
    }

    const ahora = new Date();
    const mesActual = `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}`;
    const hoy = ahora.toISOString().split('T')[0];
    const mesRef = this.mesReferenciaCobro();
    const fechaCobro =
      this.modoPeriodo() === 'MENSUAL' && mesRef !== mesActual ? `${mesRef}-01` : hoy;

    const payload: Record<string, unknown> = {
      importe: Number(contrato.importeMensual),
      tipo: 'INGRESO',
      fecha: fechaCobro,
      descripcion: `Alquiler mensual (${mesRef}) - ${this.inquilinoNombre(contrato.inquilinoId)} (${this.inmuebleNombre(contrato.inmuebleId)})`,
      contexto: this.contexto(),
      wallet: { id: walletId },
      categoria: { id: categoria.id },
      inmueble: { id: contrato.inmuebleId },
      contrato: { id: contrato.id },
      ocrProcesado: false,
    };

    console.log('Enviando cobro de contrato:', payload);
    this.finanzasService.createTransaccion(payload).subscribe({
      next: (res) => {
        console.log('Respuesta recibida:', res);
        this.cargarDatos();
      },
      error: (err) => console.error('Error al cobrar mes del contrato:', err),
    });
  }

  actualizarTraspaso(campo: string, valor: any): void {
    this.traspaso.update((t) => ({ ...t, [campo]: valor }));
  }

  realizarTraspaso(): void {
    const traspaso = this.traspaso();
    const walletOrigenId = this.wallets()[0]?.id;
    if (!walletOrigenId || !traspaso.contextoDestino || !traspaso.importe || !traspaso.fecha) return;

    console.log('Enviando traspaso:', {
      walletOrigenId,
      contextoDestino: traspaso.contextoDestino,
      importe: traspaso.importe,
      fecha: traspaso.fecha,
      descripcion: traspaso.descripcion,
    });
    this.finanzasService.realizarTraspaso({
      walletOrigenId,
      contextoDestino: traspaso.contextoDestino,
      importe: traspaso.importe,
      fecha: traspaso.fecha,
      descripcion: traspaso.descripcion,
    }).subscribe({
      next: () => {
        console.log('Respuesta recibida: traspaso completado');
        this.cargarDatos();
        this.traspaso.set({
          fecha: new Date().toISOString().split('T')[0],
        });
      },
      error: (err) => console.error('Error al realizar traspaso:', err),
    });
  }

  eliminarWallet(id: number): void {
    console.log('Eliminando wallet ID:', id);
    if (confirm('¿Estás seguro de eliminar esta wallet?')) {
      this.finanzasService.deleteWallet(id).subscribe({
        next: () => {
          console.log('Wallet eliminada:', id);
          this.cargarDatos();
        },
        error: (err) => console.error('Error al eliminar wallet:', err),
      });
    }
  }

  eliminarCategoria(id: number): void {
    console.log('Eliminando categoría ID:', id);
    if (confirm('¿Estás seguro de eliminar esta categoría?')) {
      this.finanzasService.deleteCategoria(id).subscribe({
        next: () => {
          console.log('Categoría eliminada:', id);
          this.cargarDatos();
        },
        error: (err) => console.error('Error al eliminar categoría:', err),
      });
    }
  }

  eliminarTransaccion(id: number): void {
    console.log('Eliminando transacción ID:', id);
    if (confirm('¿Estás seguro de eliminar esta transacción? Si es un traspaso, se eliminará también la transacción espejo.')) {
      this.finanzasService.deleteTransaccion(id).subscribe({
        next: () => {
          console.log('Transacción eliminada:', id);
          this.cargarDatos();
        },
        error: (err) => console.error('Error al eliminar transacción:', err),
      });
    }
  }

  eliminarInmueble(id: number): void {
    console.log('Eliminando inmueble ID:', id);
    if (confirm('¿Estás seguro de eliminar este inmueble?')) {
      this.inmobiliariaService.deleteInmueble(id).subscribe({
        next: () => {
          console.log('Inmueble eliminado:', id);
          this.cargarDatos();
        },
        error: (err) => console.error('Error al eliminar inmueble:', err),
      });
    }
  }

  eliminarInquilino(id: number): void {
    console.log('Eliminando inquilino ID:', id);
    if (confirm('¿Estás seguro de eliminar este inquilino?')) {
      this.inmobiliariaService.deleteInquilino(id).subscribe({
        next: () => {
          console.log('Inquilino eliminado:', id);
          this.cargarDatos();
        },
        error: (err) => console.error('Error al eliminar inquilino:', err),
      });
    }
  }

  eliminarContrato(id: number): void {
    console.log('Eliminando contrato ID:', id);
    if (confirm('¿Estás seguro de eliminar este contrato?')) {
      this.inmobiliariaService.deleteContrato(id).subscribe({
        next: () => {
          console.log('Contrato eliminado:', id);
          this.cargarDatos();
        },
        error: (err) => console.error('Error al eliminar contrato:', err),
      });
    }
  }

  actualizarWallet(campo: keyof Wallet, valor: any): void {
    this.nuevaWallet.update((w) => ({ ...w, [campo]: valor }));
  }

  crearWallet(): void {
    const wallet = this.nuevaWallet();
    if (!wallet.nombre) return;

    const payload: Record<string, unknown> = {
      nombre: wallet.nombre!,
      contexto: this.contexto(),
    };

    console.log('Enviando datos:', payload);
    this.finanzasService.createWallet(payload).subscribe({
      next: (res) => {
        console.log('Respuesta recibida:', res);
        this.cargarDatos();
        this.nuevaWallet.set({});
      },
      error: (err) => console.error('Error al crear wallet:', err),
    });
  }

  actualizarCategoria(campo: keyof Categoria, valor: any): void {
    this.nuevaCategoria.update((c) => ({ ...c, [campo]: valor }));
  }

  crearCategoria(): void {
    const cat = this.nuevaCategoria();
    if (!cat.nombre) return;

    const payload: Record<string, unknown> = {
      nombre: cat.nombre!,
      contexto: this.contexto(),
    };

    console.log('Enviando datos:', payload);
    this.finanzasService.createCategoria(payload).subscribe({
      next: (res) => {
        console.log('Respuesta recibida:', res);
        this.cargarDatos();
        this.nuevaCategoria.set({});
        this.mostrarFormCategoria.set(false);
      },
      error: (err) => console.error('Error al crear categoría:', err),
    });
  }

  // Inmobiliaria
  actualizarInmueble(campo: keyof Inmueble, valor: any): void {
    this.nuevoInmueble.update((i) => ({ ...i, [campo]: valor }));
  }

  editarInmueble(inm: Inmueble): void {
    this.nuevoInmueble.set({ ...inm });
    this.editandoInmuebleId.set(inm.id ?? null);
  }

  cancelarEdicionInmueble(): void {
    this.editandoInmuebleId.set(null);
    this.nuevoInmueble.set({ tipo: 'PISO' });
  }

  crearInmueble(): void {
    const inm = this.nuevoInmueble();
    if (!inm.tipo || !inm.direccion) return;

    const payload: Record<string, unknown> = {
      tipo: inm.tipo,
      direccion: inm.direccion!,
      referencia: inm.referencia,
      metros: inm.metros,
      contexto: this.contexto(),
    };

    const editId = this.editandoInmuebleId();
    console.log(editId ? `Enviando actualización inmueble ID ${editId}:` : 'Enviando datos:', payload);
    const peticion = editId
      ? this.inmobiliariaService.actualizarInmueble(editId, payload)
      : this.inmobiliariaService.createInmueble(payload);

    peticion.subscribe({
      next: (res) => {
        console.log('Respuesta recibida:', res);
        this.editandoInmuebleId.set(null);
        this.cargarDatos();
        this.nuevoInmueble.set({ tipo: 'PISO' });
      },
      error: (err) => console.error(editId ? 'Error al actualizar inmueble:' : 'Error al crear inmueble:', err),
    });
  }

  actualizarInquilino(campo: keyof Inquilino, valor: any): void {
    this.nuevoInquilino.update((i) => ({ ...i, [campo]: valor }));
  }

  editarInquilino(inq: Inquilino): void {
    this.nuevoInquilino.set({ ...inq });
    this.editandoInquilinoId.set(inq.id ?? null);
  }

  cancelarEdicionInquilino(): void {
    this.editandoInquilinoId.set(null);
    this.nuevoInquilino.set({});
  }

  crearInquilino(): void {
    const inq = this.nuevoInquilino();
    if (!inq.nombre) return;

    const payload: Record<string, unknown> = {
      nombre: inq.nombre!,
      email: inq.email,
      telefono: inq.telefono,
      documento: inq.documento,
      contexto: this.contexto(),
    };

    const editId = this.editandoInquilinoId();
    console.log(editId ? `Enviando actualización inquilino ID ${editId}:` : 'Enviando datos:', payload);
    const peticion = editId
      ? this.inmobiliariaService.actualizarInquilino(editId, payload)
      : this.inmobiliariaService.createInquilino(payload);

    peticion.subscribe({
      next: (res) => {
        console.log('Respuesta recibida:', res);
        this.editandoInquilinoId.set(null);
        this.cargarDatos();
        this.nuevoInquilino.set({});
      },
      error: (err) => console.error(editId ? 'Error al actualizar inquilino:' : 'Error al crear inquilino:', err),
    });
  }

  actualizarContrato(campo: keyof Contrato, valor: any): void {
    this.nuevoContrato.update((c) => ({ ...c, [campo]: valor }));
  }

  editarContrato(cont: Contrato): void {
    this.nuevoContrato.set({
      inmuebleId: cont.inmuebleId,
      inquilinoId: cont.inquilinoId,
      fechaInicio: cont.fechaInicio,
      fechaFin: cont.fechaFin,
      importeMensual: cont.importeMensual,
      fianza: cont.fianza,
    });
    this.editandoContratoId.set(cont.id ?? null);
  }

  cancelarEdicionContrato(): void {
    this.editandoContratoId.set(null);
    this.nuevoContrato.set({ fianza: 0 });
  }

  crearContrato(): void {
    const cont = this.nuevoContrato();
    if (!cont.inmuebleId || !cont.inquilinoId || !cont.fechaInicio || !cont.importeMensual) return;

    const payload: Record<string, unknown> = {
      inmueble: { id: cont.inmuebleId },
      inquilino: { id: cont.inquilinoId },
      fechaInicio: cont.fechaInicio,
      fechaFin: cont.fechaFin,
      importeMensual: cont.importeMensual,
      fianza: cont.fianza || 0,
      contexto: this.contexto(),
    };

    const editId = this.editandoContratoId();
    console.log(editId ? `Enviando actualización contrato ID ${editId}:` : 'Enviando datos:', payload);
    const peticion = editId
      ? this.inmobiliariaService.actualizarContrato(editId, payload)
      : this.inmobiliariaService.createContrato(payload);

    peticion.subscribe({
      next: (res) => {
        console.log('Respuesta recibida:', res);
        this.editandoContratoId.set(null);
        this.cargarDatos();
        this.nuevoContrato.set({ fianza: 0 });
      },
      error: (err) => console.error(editId ? 'Error al actualizar contrato:' : 'Error al crear contrato:', err),
    });
  }
}
