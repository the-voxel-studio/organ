import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';

export const credentialsInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
    const credentialsReq = req.clone({
        withCredentials: true
    });
    return next(credentialsReq);
};