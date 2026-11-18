import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Wallet, Categoria, Transaccion } from './models';
import { ContextoId } from './context.service';

@Injectable({ providedIn: 'root' })
export class FinanzasService {
  private readonly baseUrl = 'http://localhost:8080/api/finanzas';

  constructor(private readonly http: HttpClient) {}

  // Wallets
  getWallets(): Observable<Wallet[]> {
    return this.http.get<Wallet[]>(`${this.baseUrl}/wallets`);
  }

  createWallet(wallet: Record<string, unknown>): Observable<Wallet> {
    return this.http.post<Wallet>(`${this.baseUrl}/wallets`, wallet);
  }

  deleteWallet(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/wallets/${id}`);
  }

  // Categorías
  getCategorias(): Observable<Categoria[]> {
    return this.http.get<Categoria[]>(`${this.baseUrl}/categorias`);
  }

  createCategoria(categoria: Record<string, unknown>): Observable<Categoria> {
    return this.http.post<Categoria>(`${this.baseUrl}/categorias`, categoria);
  }

  actualizarCategoria(id: number, categoria: Record<string, unknown>): Observable<Categoria> {
    return this.http.put<Categoria>(`${this.baseUrl}/categorias/${id}`, categoria);
  }

  deleteCategoria(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categorias/${id}`);
  }

  // Transacciones
  getTransacciones(): Observable<Transaccion[]> {
    return this.http.get<Transaccion[]>(`${this.baseUrl}/transacciones`);
  }

  createTransaccion(transaccion: Record<string, unknown>): Observable<Transaccion> {
    return this.http.post<Transaccion>(`${this.baseUrl}/transacciones`, transaccion);
  }

  actualizarTransaccion(id: number, transaccion: Record<string, unknown>): Observable<Transaccion> {
    return this.http.put<Transaccion>(`${this.baseUrl}/transacciones/${id}`, transaccion);
  }

  deleteTransaccion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/transacciones/${id}`);
  }

  realizarTraspaso(request: {
    walletOrigenId: number;
    contextoDestino: ContextoId;
    importe: number;
    fecha: string;
    descripcion?: string;
  }): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/transacciones/traspaso`, request);
  }
}
