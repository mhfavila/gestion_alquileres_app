import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { CONTEXTO_HEADER, PIN_HEADER, PIN_STORAGE_KEY, ContextService } from './context.service';

export const contextInterceptor: HttpInterceptorFn = (req, next) => {
  const contexto = inject(ContextService).contexto();
  const esApi = req.url.includes('/api/finanzas') || req.url.includes('/api/inmobiliaria');

  let pin = '';
  try {
    pin = localStorage.getItem(PIN_STORAGE_KEY) || '';
  } catch {
    pin = '';
  }

  const headers: Record<string, string> = {
    [CONTEXTO_HEADER]: contexto,
  };
  if (pin) {
    headers[PIN_HEADER] = pin;
  }

  const request = esApi ? req.clone({ setHeaders: headers }) : req;

  console.log('HTTP Request:', request.method, request.url, 'Context Header:', request.headers.get(CONTEXTO_HEADER));

  return next(request).pipe(
    catchError((err) => {
      console.error('HTTP Error:', err);
      if (err?.error) {
        console.error('HTTP Error Body:', err.error);
      }
      return throwError(() => err);
    })
  );
};
