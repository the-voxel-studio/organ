import { HttpInterceptorFn } from '@angular/common/http';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('/api')) {
    const apiReq = req.clone({
      url: `http://localhost:8001${req.url}`
    });
    return next(apiReq);
  }
  return next(req);
};
