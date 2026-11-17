export type TipoTransaccion = 'INGRESO' | 'GASTO';
export type TipoInmueble = 'CHALET' | 'PISO' | 'LOCAL' | 'GARAJE';
export type Contexto = 'REAL_ESTATE' | 'HOME' | 'PERSONAL';

export interface Wallet {
  id: number;
  nombre: string;
  contexto: Contexto;
}

export interface Categoria {
  id: number;
  nombre: string;
  contexto: Contexto;
}

export interface Transaccion {
  id?: number;
  importe: number;
  tipo: TipoTransaccion;
  fecha: string;
  descripcion?: string;
  contexto: Contexto;
  walletId: number;
  categoriaId: number;
  inmuebleId?: number;
  contratoId?: number;
  urlComprobante?: string;
  ocrProcesado: boolean;
  traspasoParId?: number;
}

export interface Inmueble {
  id?: number;
  tipo: TipoInmueble;
  direccion: string;
  referencia?: string;
  metros?: number;
  contexto: Contexto;
}

export interface Inquilino {
  id?: number;
  nombre: string;
  email?: string;
  telefono?: string;
  documento?: string;
  contexto: Contexto;
}

export interface Contrato {
  id?: number;
  inmuebleId: number;
  inquilinoId: number;
  fechaInicio: string;
  fechaFin?: string;
  importeMensual: number;
  fianza: number;
  contexto: Contexto;
}
