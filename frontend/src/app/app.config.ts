import {ApplicationConfig, provideExperimentalZonelessChangeDetection} from '@angular/core';
import {provideRouter, withComponentInputBinding} from '@angular/router';

import {routes} from './app.routes';
import {provideHttpClient, withFetch, withInterceptors, withNoXsrfProtection} from '@angular/common/http';
import {addAuthorizationHeaderInterceptor, apiUrlInterceptor} from './service/interceptors';

export const appConfig: ApplicationConfig = {
  providers: [
    provideExperimentalZonelessChangeDetection(),
    provideHttpClient(
      withFetch(),
      withNoXsrfProtection(),
      withInterceptors([
        addAuthorizationHeaderInterceptor,
        apiUrlInterceptor,
      ]),
    ),
    provideRouter(
      routes,
      withComponentInputBinding(),
    ),
  ],
};
