import { HttpInterceptorFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap, catchError, throwError } from 'rxjs';
import { ApiLogService } from '../services/api-log.service';
import { ToastService } from '../services/toast.service';

export const apiLogInterceptor: HttpInterceptorFn = (req, next) => {
  const apiLog = inject(ApiLogService);
  const toast = inject(ToastService);
  const startTime = Date.now();
  const id = Math.random().toString(36).substring(2) + Date.now().toString(36);

  // Clone body for logging
  const requestBody = req.body ? JSON.parse(JSON.stringify(req.body)) : null;

  return next(req).pipe(
    tap(event => {
      if (event instanceof HttpResponse) {
        apiLog.log({
          id,
          timestamp: new Date(),
          method: req.method,
          url: req.urlWithParams,
          requestBody,
          responseBody: event.body,
          statusCode: event.status,
          duration: Date.now() - startTime,
          error: false,
        });
      }
    }),
    catchError((error: HttpErrorResponse) => {
      apiLog.log({
        id,
        timestamp: new Date(),
        method: req.method,
        url: req.urlWithParams,
        requestBody,
        responseBody: error.error ?? error.message,
        statusCode: error.status,
        duration: Date.now() - startTime,
        error: true,
      });
      toast.error(`${req.method} ${req.url} failed: ${error.status} ${error.statusText}`);
      return throwError(() => error);
    }),
  );
};
