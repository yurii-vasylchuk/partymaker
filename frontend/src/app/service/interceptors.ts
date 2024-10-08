import {HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {AuthenticationService} from './authentication.service';
import {environment} from '../environment/environment';

export const addAuthorizationHeaderInterceptor: HttpInterceptorFn = (req, next) => {
  let service = inject(AuthenticationService);
  if (service.jwt != null) {
    return next(req.clone({headers: req.headers.append('Authorization', `Bearer ${service.jwt}`)}));
  }
  return next(req);
};

export const apiUrlInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.url.startsWith('/api') && environment.apiUrl != null) {
    return next(request.clone({
      url: `${environment.apiUrl}${request.url}`,
    }));
  } else {
    return next(request);
  }
};
