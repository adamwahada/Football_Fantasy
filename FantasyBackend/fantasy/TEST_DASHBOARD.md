# Test du Dashboard Admin - Script de Vérification

## Instructions de Test

### 1. Redémarrez le Serveur Spring Boot
```bash
# Arrêtez le serveur (Ctrl+C)
# Puis redémarrez
mvn spring-boot:run
```

### 2. Testez les Endpoints dans l'Ordre

#### Étape 1 : Test de Base
```bash
GET /api/support/test
```
**Résultat attendu** : 
```json
{
  "status": "OK",
  "userId": 1,
  "isAdmin": true,
  "timestamp": "2024-01-15T10:30:00"
}
```

#### Étape 2 : Vérification Admin
```bash
GET /api/support/is-admin
```
**Résultat attendu** :
```json
{
  "isAdmin": true
}
```

#### Étape 3 : Test des Tickets
```bash
GET /api/support/admin/tickets
```
**Résultat attendu** :
```json
[]  // Liste vide si pas de tickets
```

#### Étape 4 : Test du Dashboard
```bash
GET /api/support/admin/dashboard
```
**Résultat attendu** :
```json
{
  "statistics": {
    "totalTickets": 0,
    "openTickets": 0,
    "inProgressTickets": 0,
    "resolvedTickets": 0,
    "closedTickets": 0,
    "urgentTickets": 0
  },
  "recentTickets": []
}
```

### 3. Test Frontend

#### Étape 1 : Accès au Dashboard
1. Connectez-vous en tant qu'admin (ID = 1)
2. Allez sur `/admin/dashboard`
3. Vérifiez que la page se charge sans erreur

#### Étape 2 : Vérification de l'Interface
1. Vérifiez que les statistiques s'affichent (même si à 0)
2. Vérifiez que le tableau des tickets s'affiche
3. Vérifiez que les filtres fonctionnent
4. Vérifiez que les boutons d'action sont présents

### 4. Création d'un Ticket de Test

Si vous voulez tester avec des données réelles :

#### Créez un ticket via l'API
```bash
POST /api/support/ticket
Content-Type: application/json

{
  "supportType": "GENERAL",
  "subject": "Test Dashboard",
  "description": "Ticket de test pour le dashboard admin",
  "priority": "MEDIUM"
}
```

#### Puis testez le dashboard
```bash
GET /api/support/admin/dashboard
```

**Résultat attendu** :
```json
{
  "statistics": {
    "totalTickets": 1,
    "openTickets": 1,
    "inProgressTickets": 0,
    "resolvedTickets": 0,
    "closedTickets": 0,
    "urgentTickets": 0
  },
  "recentTickets": [
    {
      "ticketId": "TICKET-XXXXXX",
      "subject": "Test Dashboard",
      "status": "OPEN",
      "priority": "MEDIUM",
      ...
    }
  ]
}
```

## Vérifications des Logs

Dans la console Spring Boot, vous devriez voir :

### Succès
```
INFO  - Récupération du dashboard pour l'admin: 1
INFO  - Statistiques récupérées: SupportDashboardStatsDTO(...)
INFO  - Tickets récents récupérés: 0
INFO  - Réponse dashboard préparée avec succès
```

### Erreur
```
ERROR - Erreur lors de la récupération du dashboard: 
java.lang.Exception: [détails de l'erreur]
```

## Solutions aux Problèmes Courants

### Problème 1 : "Access denied"
**Cause** : L'utilisateur n'a pas l'ID = 1
**Solution** : Vérifiez que vous êtes connecté avec le bon compte admin

### Problème 2 : "User not found"
**Cause** : L'utilisateur n'existe pas dans la base de données
**Solution** : Vérifiez la table `users` et l'ID utilisateur

### Problème 3 : Erreur 500
**Cause** : Problème dans la requête SQL ou la logique métier
**Solution** : Vérifiez les logs détaillés et la base de données

### Problème 4 : Interface qui disparaît
**Cause** : Erreur JavaScript ou problème de chargement
**Solution** : Vérifiez la console du navigateur (F12)

## Commandes de Debug

### Vérification de la Base de Données
```sql
-- Vérifiez l'utilisateur admin
SELECT * FROM users WHERE id = 1;

-- Vérifiez les tickets
SELECT * FROM support_tickets;

-- Vérifiez les chat rooms
SELECT * FROM chat_rooms WHERE type = 'SUPPORT';
```

### Vérification des Logs
```bash
# Dans la console Spring Boot, cherchez :
grep "dashboard" logs/application.log
grep "ERROR" logs/application.log
```

## Résultat Final Attendu

✅ **Succès** : 
- L'endpoint `/api/support/test` répond correctement
- L'endpoint `/api/support/admin/dashboard` répond avec des statistiques
- L'interface Angular se charge et affiche le dashboard
- Les statistiques s'affichent (même si à 0)
- Aucune erreur dans les logs Spring Boot
- Aucune erreur dans la console du navigateur

❌ **Échec** :
- Erreurs 500, 404, ou 403
- Interface qui ne se charge pas
- Erreurs dans les logs
- Messages d'erreur dans la console du navigateur

---

**Note** : Si tous les tests passent, le dashboard admin est fonctionnel ! 🎉
