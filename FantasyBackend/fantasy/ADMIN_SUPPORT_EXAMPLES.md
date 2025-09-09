# Exemples d'Utilisation - API Support Admin

## Scénario Complet : Gestion d'un Ticket de Support

### 1. Création du Ticket (Côté User)
```bash
POST /api/support/ticket
{
  "supportType": "PAYMENT",
  "subject": "Problème de paiement",
  "description": "Je n'arrive pas à effectuer mon paiement pour la ligue premium",
  "priority": "HIGH"
}
```

**Réponse**:
```json
{
  "ticket": {
    "ticketId": "TICKET-123456",
    "subject": "Problème de paiement",
    "status": "OPEN",
    "priority": "HIGH",
    "chatRoomId": "uuid-chat-room"
  },
  "message": "Ticket créé avec succès ! Vous pouvez maintenant discuter avec notre équipe."
}
```

### 2. Dashboard Admin - Vue d'ensemble
```bash
GET /api/support/admin/dashboard
```

**Réponse**:
```json
{
  "statistics": {
    "totalTickets": 15,
    "openTickets": 3,
    "inProgressTickets": 2,
    "resolvedTickets": 8,
    "closedTickets": 2,
    "urgentTickets": 1
  },
  "recentTickets": [
    {
      "ticketId": "TICKET-123456",
      "subject": "Problème de paiement",
      "status": "OPEN",
      "priority": "HIGH",
      "userName": "John Doe",
      "createdAt": "2024-01-15T10:30:00"
    }
  ]
}
```

### 3. Admin prend en charge le ticket
```bash
POST /api/support/admin/ticket/TICKET-123456/assign
```

**Réponse**:
```json
{
  "ticket": {
    "ticketId": "TICKET-123456",
    "status": "IN_PROGRESS",
    "assignedAdminName": "Admin Support"
  },
  "message": "Ticket assigné avec succès"
}
```

**Message automatique dans le chat**:
```
⚡ Le ticket est maintenant en cours de traitement.

📝 Note de l'équipe : Ticket assigné à l'équipe de support
```

### 4. Admin change la priorité et ajoute une note
```bash
PUT /api/support/admin/ticket/TICKET-123456/status
{
  "status": "IN_PROGRESS",
  "priority": "URGENT",
  "adminNote": "Problème de paiement identifié, en cours de résolution avec le service financier"
}
```

**Message automatique dans le chat**:
```
⚡ Le ticket est maintenant en cours de traitement.

📝 Note de l'équipe : Problème de paiement identifié, en cours de résolution avec le service financier
```

### 5. Admin marque comme résolu
```bash
PUT /api/support/admin/ticket/TICKET-123456/status
{
  "status": "RESOLVED",
  "adminNote": "Problème résolu : paiement traité avec succès. L'utilisateur a maintenant accès à la ligue premium."
}
```

**Message automatique dans le chat**:
```
✅ Le ticket a été marqué comme résolu.

📝 Note de l'équipe : Problème résolu : paiement traité avec succès. L'utilisateur a maintenant accès à la ligue premium.
```

### 6. Admin ferme le ticket
```bash
PUT /api/support/admin/ticket/TICKET-123456/status
{
  "status": "CLOSED"
}
```

**Message automatique dans le chat**:
```
🔒 Le ticket a été fermé.
```

## Exemples de Requêtes par Catégorie

### Tickets Actifs
```bash
GET /api/support/admin/tickets/active
```

**Réponse**:
```json
{
  "tickets": [
    {
      "ticketId": "TICKET-123456",
      "subject": "Problème de paiement",
      "status": "IN_PROGRESS",
      "priority": "URGENT",
      "userName": "John Doe",
      "assignedAdminName": "Admin Support",
      "createdAt": "2024-01-15T10:30:00",
      "unreadMessagesCount": 0
    }
  ],
  "count": 1,
  "type": "active"
}
```

### Tickets Urgents
```bash
GET /api/support/admin/tickets/urgent
```

### Tickets par Statut
```bash
GET /api/support/admin/tickets/status/OPEN
GET /api/support/admin/tickets/status/IN_PROGRESS
GET /api/support/admin/tickets/status/RESOLVED
GET /api/support/admin/tickets/status/CLOSED
```

### Tickets par Priorité
```bash
GET /api/support/admin/tickets/priority/URGENT
GET /api/support/admin/tickets/priority/HIGH
GET /api/support/admin/tickets/priority/MEDIUM
GET /api/support/admin/tickets/priority/LOW
```

### Recherche
```bash
GET /api/support/admin/tickets/search?query=paiement
GET /api/support/admin/tickets/search?query=TICKET-123456
GET /api/support/admin/tickets/search?query=compte
```

## Statistiques Détaillées

```bash
GET /api/support/admin/statistics/detailed
```

**Réponse**:
```json
{
  "basicStats": {
    "totalTickets": 15,
    "openTickets": 3,
    "inProgressTickets": 2,
    "resolvedTickets": 8,
    "closedTickets": 2,
    "urgentTickets": 1
  },
  "statusCounts": {
    "OPEN": 3,
    "IN_PROGRESS": 2,
    "RESOLVED": 8,
    "CLOSED": 2
  },
  "priorityCounts": {
    "LOW": 2,
    "MEDIUM": 8,
    "HIGH": 4,
    "URGENT": 1
  },
  "recentTickets": [
    {
      "ticketId": "TICKET-123456",
      "subject": "Problème de paiement",
      "status": "CLOSED",
      "priority": "URGENT",
      "userName": "John Doe",
      "createdAt": "2024-01-15T10:30:00",
      "resolvedAt": "2024-01-15T14:45:00"
    }
  ],
  "generatedAt": "2024-01-15T15:00:00"
}
```

## Endpoints Utilitaires

### Vérifier si Admin
```bash
GET /api/support/is-admin
```

**Réponse**:
```json
{
  "isAdmin": true
}
```

### Types de Support
```bash
GET /api/support/types
```

**Réponse**:
```json
[
  {
    "type": "PAYMENT",
    "displayName": "Paiement"
  },
  {
    "type": "TECHNICAL",
    "displayName": "Problème technique"
  },
  {
    "type": "ACCOUNT",
    "displayName": "Compte/Profil"
  },
  {
    "type": "GENERAL",
    "displayName": "Question générale"
  }
]
```

### Statuts Disponibles
```bash
GET /api/support/statuses
```

**Réponse**:
```json
[
  { "value": "OPEN", "displayName": "Ouvert" },
  { "value": "IN_PROGRESS", "displayName": "En cours" },
  { "value": "RESOLVED", "displayName": "Résolu" },
  { "value": "CLOSED", "displayName": "Fermé" }
]
```

### Priorités Disponibles
```bash
GET /api/support/priorities
```

**Réponse**:
```json
[
  { "value": "LOW", "displayName": "Basse" },
  { "value": "MEDIUM", "displayName": "Moyenne" },
  { "value": "HIGH", "displayName": "Élevée" },
  { "value": "URGENT", "displayName": "Urgente" }
]
```

## Gestion des Erreurs

### Erreur 403 - Pas Admin
```bash
PUT /api/support/admin/ticket/TICKET-123456/status
{
  "status": "RESOLVED"
}
```

**Réponse**:
```json
{
  "error": "Accès refusé - Admin uniquement"
}
```

### Erreur 400 - Transition Invalide
```bash
PUT /api/support/admin/ticket/TICKET-123456/status
{
  "status": "IN_PROGRESS"
}
```

**Réponse** (si le ticket est déjà CLOSED):
```json
{
  "error": "Invalid status transition from CLOSED to IN_PROGRESS"
}
```

### Erreur 404 - Ticket Non Trouvé
```bash
PUT /api/support/admin/ticket/TICKET-INEXISTANT/status
{
  "status": "RESOLVED"
}
```

**Réponse**:
```json
{
  "error": "Ticket not found"
}
```

## Workflow Complet Admin

1. **Connexion Admin** → Vérifier `/is-admin`
2. **Dashboard** → Voir `/admin/dashboard`
3. **Tickets Actifs** → Lister `/admin/tickets/active`
4. **Tickets Urgents** → Prioriser `/admin/tickets/urgent`
5. **Prendre en Charge** → Assigner `/admin/ticket/{id}/assign`
6. **Traiter** → Changer statut `/admin/ticket/{id}/status`
7. **Résoudre** → Marquer résolu `/admin/ticket/{id}/status`
8. **Fermer** → Fermer ticket `/admin/ticket/{id}/status`
9. **Statistiques** → Voir `/admin/statistics/detailed`

Tous les changements génèrent automatiquement des messages dans le chat pour informer l'utilisateur !
