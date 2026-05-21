import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { GoogleLoginProvider, SocialAuthServiceConfig, SocialAuthService, SOCIAL_AUTH_CONFIG } from '@abacritt/angularx-social-login';

import { routes } from './app.routes';
import { apiInterceptor } from './interceptors/api.interceptor';
import { credentialsInterceptor } from './interceptors/credentials.interceptor';
import { refreshTokenInterceptor } from './interceptors/refresh-token.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withInMemoryScrolling({
      anchorScrolling: 'enabled',
      scrollPositionRestoration: 'enabled'
    })),
    provideHttpClient(withInterceptors([apiInterceptor, credentialsInterceptor, refreshTokenInterceptor])),
    SocialAuthService,
    {
      provide: SOCIAL_AUTH_CONFIG,
      useValue: {
        autoLogin: false,
        providers: [
          {
            id: GoogleLoginProvider.PROVIDER_ID,
            provider: new GoogleLoginProvider(
              (import.meta as any).env.NG_APP_GOOGLE_CLIENT_ID
            )
          }
        ],
        onError: (err) => {
          console.error(err);
        }
      } as SocialAuthServiceConfig,
    }
  ]
};

