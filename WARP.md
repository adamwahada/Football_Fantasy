# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Repository overview
- Frontend: Angular 19 app in fantasy-ui
- Dev infrastructure: Docker Compose stack in docker for Keycloak (port 8180), Postgres, MySQL, and phpMyAdmin

Common commands
All commands below are shown from the repo root using --prefix. You can also cd fantasy-ui and run the npm scripts directly.

- Install dependencies (frontend)
  - npm --prefix fantasy-ui ci

- Start Angular dev server (http://localhost:4200)
  - npm --prefix fantasy-ui run start

- Build Angular app
  - npm --prefix fantasy-ui run build
  - Development watch build: npm --prefix fantasy-ui run watch

- Run unit tests (Karma/Jasmine)
  - npm --prefix fantasy-ui test
  - Run a single spec file (example): npx --prefix fantasy-ui ng test --include=src/app/admin/admin-dashboard-funds/admin-dashboard-funds.component.spec.ts

- Spin up local services (Keycloak, Postgres, MySQL, phpMyAdmin)
  - Pre-step (first time): copy docker/keycloak.env.example to docker/keycloak.env and adjust values as needed
  - Start: docker compose -f docker/docker-compose.yml up -d
  - Stop: docker compose -f docker/docker-compose.yml down
  - Services:
    - Keycloak: http://localhost:8180 (realm: football-fantasy; clientId: angular-client — configured in code)
    - Postgres: localhost:5432 (container name keycloak_db)
    - MySQL: localhost:3306 (container name mysql_fantasy)
    - phpMyAdmin: http://localhost:8081

Notes
- Linting: No lint target is configured in angular.json and no lint script exists in package.json. If linting is desired, add Angular ESLint to this workspace.
- Backend: The UI calls a backend at http://localhost:9090/fantasy/api. Ensure your backend is running and CORS is configured accordingly.

Architecture and structure
High-level
- Angular standalone app bootstrap. The application bootstraps in src/main.ts using bootstrapApplication with provideRouter(routes), provideHttpClient(withInterceptors([AuthInterceptor])), and a pre-boot KeycloakService initialization.
- Authentication and authorization via Keycloak. A custom KeycloakService wraps keycloak-js, initializes against http://localhost:8180 (realm football-fantasy, client angular-client), and exposes token/role helpers.
- HTTP authentication interceptor. AuthInterceptor attaches Bearer tokens to any request whose URL contains /api/ (except the registration endpoint /api/user/register). On 401/403 it attempts token refresh and retries once, otherwise triggers login.
- Route organization. Routes are defined in src/app/app.routes.ts and composed from feature-specific route groups:
  - /admin: AdminLayoutComponent with canActivate authGuard and roles ['ROLE_ADMIN']
  - /user: UserLayoutComponent with canActivate authGuard and roles ['ROLE_ADMIN', 'ROLE_USER'], plus gameweek access guards for state-specific routes
  - /signin, /register, /unauthorized, /landingpage, and a TestComponent route
- Guards. authGuard checks login status and role membership, normalizing ROLE_ prefixes. Additional guards in gameweek/user-gameweek/... enforce access to current/finished gameweeks.
- i18n. ngx-translate is configured in main.ts with a CustomTranslateLoader that pulls JSON from assets/i18n/{lang}.json. Default language is en; AppComponent allows switching among ['en','fr','ar'] and persists to localStorage.

Key modules/services
- AuthService (src/app/core/services/auth.service.ts)
  - Bridges Keycloak identity with the app backend. Maintains a BehaviorSubject<CurrentUser> (app DB user) and provides initialization flows, role checks, and registration (POST http://localhost:9090/fantasy/api/user/register). Redirects users after login based on roles to /admin/allgameweek or /user/user-gameweek-list.

- GameweekService (src/app/gameweek/gameweek.service.ts)
  - Handles CRUD for gameweeks and gameweek-match associations. Provides admin utilities such as validation/unvalidation and seeding/updating matches. Endpoints are under http://localhost:9090/fantasy/api/gameweeks and related admin/seed paths.

- MatchService (src/app/match/match.service.ts)
  - CRUD for matches and retrieval with team icons. Base URL http://localhost:9090/fantasy/api/matches.

- TeamService (src/app/match/team.service.ts)
  - Retrieves team and league icons and provides helpers to construct full icon URLs from relative paths (base http://localhost:9090/fantasy).

- PredictionService (src/app/gameweek/prediction.service.ts)
  - Submits predictions and joins sessions with robust error handling that propagates structured backend errors (e.g., 422 with details). Base URL http://localhost:9090/fantasy/api/predictions.

- SessionParticipationService (src/app/gameweek/session-participation.service.ts)
  - Manages joining/leaving sessions, updating prediction progress, and fetching user/session stats under http://localhost:9090/fantasy/api/session-participation (and admin endpoints).

Bootstrap and config entry points
- src/main.ts
  - Initializes Keycloak before bootstrapping. Registers router, HTTP client + interceptor, and TranslateModule with CustomTranslateLoader.
- src/app/app.config.ts
  - Provides HttpClient with the AuthInterceptor (alternative provider configuration used by Angular standalone apps).
- src/environments/environment.ts
  - Provides environment flags and a recaptchaSiteKey placeholder (ensure a valid key if the feature is used).

Feature structure (selected)
- Admin area: src/app/admin/* with AdminLayout, sidebar, and dashboards; routes in src/app/admin/admin.routes.ts.
- User area: src/app/user/* with UserLayout and dashboard views; routes in src/app/user/user.routes.ts guarded by role and gameweek access guards.
- Core: src/app/core/* with guards, interceptors, and shared auth utilities.
- Shared/i18n/assets: assets/i18n/{en,fr}.json via CustomTranslateLoader; additional assets under assets/images.

Important from README
- The fantasy-ui/README.md documents:
  - Dev server: ng serve → http://localhost:4200 with live reload
  - Build: ng build → outputs to dist/
  - Unit tests: ng test (Karma)
  - Code scaffolding: ng generate component ...

Infrastructure details (docker/)
- docker/docker-compose.yml defines services for:
  - postgres (keycloak_db) with volume postgres_data
  - keycloak (quay.io/keycloak/keycloak:24.0.1) on host port 8180; requires docker/keycloak.env and mounts for providers/themes (../mytheme expected to exist one level up if used)
  - mysql (mysql_fantasy) and phpmyadmin on port 8081 (PMA_HOST=mysql)
- docker/keycloak.env.example is provided; copy to keycloak.env before starting.
