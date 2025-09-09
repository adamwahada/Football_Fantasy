# Test du Dashboard Admin Support

## Tests à Effectuer

### 1. Test de Connexion Admin
```bash
# Vérifier si l'utilisateur est admin
GET /api/support/is-admin
Authorization: Bearer <token_admin>

# Réponse attendue
{
  "isAdmin": true
}
```

### 2. Test du Dashboard Principal
```bash
# Récupérer les statistiques du dashboard
GET /api/support/admin/dashboard
Authorization: Bearer <token_admin>

# Réponse attendue
{
  "statistics": {
    "totalTickets": 5,
    "openTickets": 2,
    "inProgressTickets": 1,
    "resolvedTickets": 1,
    "closedTickets": 1,
    "urgentTickets": 1
  },
  "recentTickets": [...]
}
```

### 3. Test de Récupération des Tickets
```bash
# Récupérer tous les tickets
GET /api/support/admin/tickets
Authorization: Bearer <token_admin>

# Récupérer les tickets actifs
GET /api/support/admin/tickets/active
Authorization: Bearer <token_admin>

# Récupérer les tickets urgents
GET /api/support/admin/tickets/urgent
Authorization: Bearer <token_admin>

# Récupérer les tickets fermés
GET /api/support/admin/tickets/closed
Authorization: Bearer <token_admin>
```

### 4. Test de Changement de Statut
```bash
# Changer le statut d'un ticket
PUT /api/support/admin/ticket/TICKET-123456/status
Authorization: Bearer <token_admin>
Content-Type: application/json

{
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "adminNote": "Ticket pris en charge par l'équipe"
}

# Réponse attendue
{
  "ticket": {
    "ticketId": "TICKET-123456",
    "status": "IN_PROGRESS",
    "priority": "HIGH",
    ...
  },
  "message": "Statut du ticket mis à jour avec succès"
}
```

### 5. Test d'Assignation
```bash
# Assigner un ticket à l'admin
POST /api/support/admin/ticket/TICKET-123456/assign
Authorization: Bearer <token_admin>

# Réponse attendue
{
  "ticket": {
    "ticketId": "TICKET-123456",
    "assignedAdminName": "Admin Support",
    "status": "IN_PROGRESS"
  },
  "message": "Ticket assigné avec succès"
}
```

### 6. Test de Recherche
```bash
# Rechercher des tickets
GET /api/support/admin/tickets/search?query=paiement
Authorization: Bearer <token_admin>

# Réponse attendue
{
  "tickets": [...],
  "query": "paiement",
  "count": 2
}
```

### 7. Test des Endpoints Utilitaires
```bash
# Récupérer les statuts disponibles
GET /api/support/statuses

# Récupérer les priorités disponibles
GET /api/support/priorities

# Récupérer les types de support
GET /api/support/types
```

## Tests Frontend

### 1. Test de Navigation
1. Se connecter en tant qu'admin (ID = 1)
2. Aller sur `/admin/dashboard`
3. Vérifier que la page se charge sans erreur
4. Vérifier que les statistiques s'affichent

### 2. Test des Filtres
1. Tester le filtre "Tickets actifs"
2. Tester le filtre "Tickets urgents"
3. Tester le filtre "Tickets fermés"
4. Tester la recherche textuelle

### 3. Test des Actions
1. Cliquer sur "Ouvrir le chat" → Vérifier l'ouverture du chat
2. Cliquer sur "Voir les détails" → Vérifier l'ouverture du modal
3. Cliquer sur "Modifier le statut" → Vérifier l'ouverture du modal
4. Tester le changement de statut avec une note

### 4. Test des Messages Automatiques
1. Changer le statut d'un ticket
2. Aller dans le chat du ticket
3. Vérifier que le message automatique est présent

## Scénarios de Test Complets

### Scénario 1 : Traitement d'un Nouveau Ticket
1. **User** crée un ticket de support
2. **Admin** voit le ticket dans le dashboard
3. **Admin** clique sur "Ouvrir le chat"
4. **Admin** s'assigner le ticket
5. **Admin** change le statut en "En cours"
6. **Admin** résout le problème via le chat
7. **Admin** marque comme résolu
8. **Admin** ferme le ticket
9. **Vérifier** que le chat disparaît côté user

### Scénario 2 : Gestion des Priorités
1. **Admin** voit un ticket urgent
2. **Admin** change la priorité en "URGENT"
3. **Admin** ajoute une note explicative
4. **Vérifier** que le message automatique est envoyé
5. **Vérifier** que le ticket apparaît dans "Tickets urgents"

### Scénario 3 : Recherche et Filtrage
1. **Admin** utilise la recherche "paiement"
2. **Admin** filtre par statut "OPEN"
3. **Admin** filtre par priorité "HIGH"
4. **Vérifier** que les résultats sont corrects

## Erreurs à Tester

### 1. Erreurs d'Authentification
- Tester sans token → 401 Unauthorized
- Tester avec token non-admin → 403 Forbidden

### 2. Erreurs de Validation
- Changer le statut avec une transition invalide
- Envoyer des données manquantes

### 3. Erreurs de Ressource
- Tester avec un ticket inexistant
- Tester avec un ID de ticket invalide

## Vérifications Post-Test

### 1. Base de Données
- Vérifier que les statuts sont mis à jour
- Vérifier que les messages sont créés
- Vérifier que les dates sont correctes

### 2. Chat
- Vérifier que les messages automatiques sont présents
- Vérifier que les utilisateurs reçoivent les notifications
- Vérifier que l'historique est conservé

### 3. Interface
- Vérifier que les statistiques se mettent à jour
- Vérifier que les filtres fonctionnent
- Vérifier que les modals se ferment correctement

## Commandes de Test Rapides

```bash
# Test complet avec curl
curl -X GET "http://localhost:8080/api/support/admin/dashboard" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json"

# Test de changement de statut
curl -X PUT "http://localhost:8080/api/support/admin/ticket/TICKET-123456/status" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "RESOLVED",
    "adminNote": "Problème résolu avec succès"
  }'
```

## Résultats Attendus

✅ **Succès** : Tous les endpoints répondent correctement
✅ **Succès** : L'interface se charge sans erreur
✅ **Succès** : Les actions fonctionnent et mettent à jour la base de données
✅ **Succès** : Les messages automatiques sont envoyés
✅ **Succès** : Les utilisateurs reçoivent les notifications

❌ **Échec** : Erreurs 500, 404, ou 403
❌ **Échec** : Interface qui ne se charge pas
❌ **Échec** : Actions qui ne fonctionnent pas
❌ **Échec** : Messages automatiques manquants

---

**Note** : Assurez-vous d'avoir des tickets de test dans la base de données avant de commencer les tests.
