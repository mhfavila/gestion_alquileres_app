import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { CONTEXTO_HEADER, ContextService } from './context.service';

export const contextInterceptor: HttpInterceptorFn = (req, next) => {
  const contexto = inject(ContextService).contexto();
  const esApi = req.url.includes('/api/finanzas') || req.url.includes('/api/inmobiliaria');
  const request = esApi
    ? req.clone({ setHeaders: { [CONTEXTO_HEADER]: contexto } })
    : req;

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
