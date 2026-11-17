import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Inmueble, Inquilino, Contrato } from './models';

@Injectable({ providedIn: 'root' })
export class InmobiliariaService {
  private readonly baseUrl = 'http://localhost:8080/api/inmobiliaria';

  constructor(private readonly http: HttpClient) {}

  // Inmuebles
  getInmuebles(): Observable<Inmueble[]> {
    return this.http.get<Inmueble[]>(`${this.baseUrl}/inmuebles`);
  }

  createInmueble(inmueble: Omit<Inmueble, 'id'>): Observable<Inmueble> {
    return this.http.post<Inmueble>(`${this.baseUrl}/inmuebles`, inmueble);
  }

  deleteInmueble(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/inmuebles/${id}`);
  }

  // Inquilinos
  getInquilinos(): Observable<Inquilino[]> {
    return this.http.get<Inquilino[]>(`${this.baseUrl}/inquilinos`);
  }

  createInquilino(inquilino: Omit<Inquilino, 'id'>): Observable<Inquilino> {
    return this.http.post<Inquilino>(`${this.baseUrl}/inquilinos`, inquilino);
  }

  deleteInquilino(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/inquilinos/${id}`);
  }

  // Contratos
  getContratos(): Observable<Contrato[]> {
    return this.http.get<Contrato[]>(`${this.baseUrl}/contratos`);
  }

  createContrato(contrato: Omit<Contrato, 'id'>): Observable<Contrato> {
    return this.http.post<Contrato>(`${this.baseUrl}/contratos`, contrato);
  }

  deleteContrato(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/contratos/${id}`);
  }
}
