import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CONTEXTO_HEADER, ContextService } from './context.service';

export const contextInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.includes('/api/finanzas') && !req.url.includes('/api/inmobiliaria')) {
    return next(req);
  }
  const contexto = inject(ContextService).contexto();
  return next(req.clone({ setHeaders: { [CONTEXTO_HEADER]: contexto } }));
};
