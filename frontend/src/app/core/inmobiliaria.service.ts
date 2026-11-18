import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Inmueble, Inquilino, Contrato } from './models';

@Injectable({ providedIn: 'root' })
export class InmobiliariaService {
  private readonly baseUrl = '/api/inmobiliaria';

  constructor(private readonly http: HttpClient) {}

  // Inmuebles
  getInmuebles(): Observable<Inmueble[]> {
    return this.http.get<Inmueble[]>(`${this.baseUrl}/inmuebles`);
  }

  createInmueble(inmueble: Record<string, unknown>): Observable<Inmueble> {
    return this.http.post<Inmueble>(`${this.baseUrl}/inmuebles`, inmueble);
  }

  actualizarInmueble(id: number, inmueble: Record<string, unknown>): Observable<Inmueble> {
    return this.http.put<Inmueble>(`${this.baseUrl}/inmuebles/${id}`, inmueble);
  }

  deleteInmueble(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/inmuebles/${id}`);
  }

  // Inquilinos
  getInquilinos(): Observable<Inquilino[]> {
    return this.http.get<Inquilino[]>(`${this.baseUrl}/inquilinos`);
  }

  createInquilino(inquilino: Record<string, unknown>): Observable<Inquilino> {
    return this.http.post<Inquilino>(`${this.baseUrl}/inquilinos`, inquilino);
  }

  actualizarInquilino(id: number, inquilino: Record<string, unknown>): Observable<Inquilino> {
    return this.http.put<Inquilino>(`${this.baseUrl}/inquilinos/${id}`, inquilino);
  }

  deleteInquilino(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/inquilinos/${id}`);
  }

  // Contratos
  getContratos(): Observable<Contrato[]> {
    return this.http.get<Contrato[]>(`${this.baseUrl}/contratos`);
  }

  createContrato(contrato: Record<string, unknown>): Observable<Contrato> {
    return this.http.post<Contrato>(`${this.baseUrl}/contratos`, contrato);
  }

  actualizarContrato(id: number, contrato: Record<string, unknown>): Observable<Contrato> {
    return this.http.put<Contrato>(`${this.baseUrl}/contratos/${id}`, contrato);
  }

  deleteContrato(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/contratos/${id}`);
  }
}
