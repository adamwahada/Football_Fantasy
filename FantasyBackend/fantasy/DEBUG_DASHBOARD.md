# Guide de Débogage - Dashboard Admin

## Problème Identifié
L'interface apparaît puis disparaît avec l'erreur : "Erreur lors de la récupération du dashboard"

## Étapes de Débogage

### 1. Test de Connectivité de Base
```bash
# Testez d'abord l'endpoint de test
GET /api/support/test
Authorization: Bearer <votre_token_admin>

# Réponse attendue
{
  "status": "OK",
  "userId": 1,
  "isAdmin": true,
  "timestamp": "2024-01-15T10:30:00"
}
```

### 2. Test de Vérification Admin
```bash
# Vérifiez que vous êtes bien admin
GET /api/support/is-admin
Authorization: Bearer <votre_token_admin>

# Réponse attendue
{
  "isAdmin": true
}
```

### 3. Test des Tickets
```bash
# Testez la récupération des tickets
GET /api/support/admin/tickets
Authorization: Bearer <votre_token_admin>

# Réponse attendue
[]  # Liste vide si pas de tickets, ou liste des tickets
```

### 4. Test du Dashboard
```bash
# Testez le dashboard complet
GET /api/support/admin/dashboard
Authorization: Bearer <votre_token_admin>

# Réponse attendue
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

## Vérifications à Faire

### 1. Vérifiez les Logs Spring Boot
Regardez dans la console Spring Boot pour voir les logs détaillés :
- `Récupération du dashboard pour l'admin: X`
- `Statistiques récupérées: {...}`
- `Tickets récents récupérés: X`
- `Réponse dashboard préparée avec succès`

### 2. Vérifiez l'ID Utilisateur
Assurez-vous que l'utilisateur connecté a bien l'ID = 1 :
```bash
# Dans les logs, vous devriez voir :
# "Récupération du dashboard pour l'admin: 1"
```

### 3. Vérifiez la Base de Données
Connectez-vous à votre base de données et vérifiez :
```sql
-- Vérifiez qu'il y a des tickets
SELECT COUNT(*) FROM support_tickets;

-- Vérifiez qu'il y a des chat_rooms de type SUPPORT
SELECT COUNT(*) FROM chat_rooms WHERE type = 'SUPPORT';

-- Vérifiez l'utilisateur admin
SELECT id, username, email FROM users WHERE id = 1;
```

### 4. Vérifiez les Erreurs Frontend
Ouvrez la console du navigateur (F12) et regardez :
- Les erreurs JavaScript
- Les requêtes HTTP qui échouent
- Les réponses des API

## Solutions Possibles

### Solution 1 : Problème de Base de Données
Si la base de données est vide ou mal configurée :
```sql
-- Créez un ticket de test
INSERT INTO support_tickets (ticket_id, subject, description, support_type, status, priority, user_id, created_at, updated_at)
VALUES ('TICKET-TEST-001', 'Test Ticket', 'Ticket de test', 'GENERAL', 'OPEN', 'MEDIUM', 1, NOW(), NOW());
```

### Solution 2 : Problème d'Authentification
Si l'utilisateur n'est pas reconnu comme admin :
- Vérifiez que l'ID utilisateur = 1
- Vérifiez que le token JWT est valide
- Vérifiez que l'utilisateur existe dans la base de données

### Solution 3 : Problème de Configuration
Si les endpoints ne répondent pas :
- Vérifiez que le serveur Spring Boot est démarré
- Vérifiez que le port 8080 est accessible
- Vérifiez que CORS est configuré correctement

## Commandes de Test Rapides

### Test avec curl
```bash
# Test de base
curl -X GET "http://localhost:8080/api/support/test" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"

# Test du dashboard
curl -X GET "http://localhost:8080/api/support/admin/dashboard" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

### Test avec Postman
1. Créez une nouvelle requête GET
2. URL : `http://localhost:8080/api/support/test`
3. Headers : `Authorization: Bearer YOUR_TOKEN`
4. Envoyez la requête

## Messages d'Erreur Courants

### "Access denied - Admin only"
- L'utilisateur n'a pas l'ID = 1
- Le token JWT est invalide
- L'utilisateur n'existe pas dans la base de données

### "User not found"
- L'utilisateur n'existe pas dans la table `users`
- Le `keycloak_id` ne correspond pas

### "Chat room not found"
- La table `chat_rooms` est vide
- Les relations entre tables sont cassées

### Erreur 500
- Problème dans la requête SQL
- Problème de configuration de la base de données
- Exception non gérée dans le code

## Logs à Surveiller

Dans la console Spring Boot, vous devriez voir :
```
INFO  - Récupération du dashboard pour l'admin: 1
INFO  - Statistiques récupérées: SupportDashboardStatsDTO(...)
INFO  - Tickets récents récupérés: 0
INFO  - Réponse dashboard préparée avec succès
```

Si vous voyez des erreurs, notez-les et vérifiez les solutions correspondantes.

## Contact
Si le problème persiste, fournissez :
1. Les logs complets de Spring Boot
2. La réponse de l'endpoint `/api/support/test`
3. Les erreurs de la console du navigateur
4. La structure de votre base de données
