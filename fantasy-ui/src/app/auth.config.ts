import { AuthConfig } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {
  issuer: 'http://localhost:8180/realms/football-fantasy',
  redirectUri: window.location.origin,
  clientId: 'angular-client', 
  responseType: 'code',
  scope: 'openid profile email',
  showDebugInformation: true,
  requireHttps: false 
};