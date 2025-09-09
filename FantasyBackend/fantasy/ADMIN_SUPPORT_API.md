# API Support Admin - Documentation Complète

## Vue d'ensemble

Cette API permet à l'admin (ID = 1) de gérer complètement les tickets de support avec toutes les fonctionnalités de changement de statut, priorité, et gestion avancée.

## Endpoints Admin

### 1. Dashboard et Statistiques

#### `GET /api/support/admin/dashboard`
- **Description**: Dashboard principal avec statistiques
- **Réponse**: Statistiques de base + tickets récents

#### `GET /api/support/admin/statistics/detailed`
- **Description**: Statistiques détaillées avec répartition par statut/priorité
- **Réponse**: 
```json
{
  "basicStats": { ... },
  "statusCounts": { "OPEN": 5, "IN_PROGRESS": 3, ... },
  "priorityCounts": { "URGENT": 2, "HIGH": 4, ... },
  "recentTickets": [ ... ],
  "generatedAt": "2024-01-15T10:30:00"
}
```

### 2. Gestion des Tickets par Statut

#### `GET /api/support/admin/tickets/active`
- **Description**: Tickets actifs (OPEN + IN_PROGRESS) triés par priorité
- **Réponse**: Liste des tickets actifs

#### `GET /api/support/admin/tickets/closed`
- **Description**: Tickets fermés (RESOLVED + CLOSED)
- **Réponse**: Liste des tickets fermés

#### `GET /api/support/admin/tickets/urgent`
- **Description**: Tickets urgents (URGENT + HIGH priority)
- **Réponse**: Liste des tickets urgents

#### `GET /api/support/admin/tickets/status/{status}`
- **Description**: Tickets par statut spécifique
- **Paramètres**: `status` = OPEN, IN_PROGRESS, RESOLVED, CLOSED
- **Exemple**: `/api/support/admin/tickets/status/OPEN`

#### `GET /api/support/admin/tickets/priority/{priority}`
- **Description**: Tickets par priorité
- **Paramètres**: `priority` = LOW, MEDIUM, HIGH, URGENT
- **Exemple**: `/api/support/admin/tickets/priority/URGENT`

### 3. Changement de Statut et Priorité

#### `PUT /api/support/admin/ticket/{ticketId}/status`
- **Description**: Changer le statut et/ou la priorité d'un ticket
- **Body**:
```json
{
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "adminNote": "Ticket pris en charge par l'équipe"
}
```
- **Réponse**: Ticket mis à jour + message automatique dans le chat

#### `POST /api/support/admin/ticket/{ticketId}/assign`
- **Description**: Assigner un ticket à l'admin (passe automatiquement en IN_PROGRESS)
- **Réponse**: Ticket assigné + message automatique

### 4. Recherche et Filtrage

#### `GET /api/support/admin/tickets/search?query={query}`
- **Description**: Rechercher dans les tickets (sujet, description, ID)
- **Exemple**: `/api/support/admin/tickets/search?query=paiement`

### 5. Endpoints Utilitaires

#### `GET /api/support/statuses`
- **Description**: Liste des statuts disponibles
- **Réponse**:
```json
[
  { "value": "OPEN", "displayName": "Ouvert" },
  { "value": "IN_PROGRESS", "displayName": "En cours" },
  { "value": "RESOLVED", "displayName": "Résolu" },
  { "value": "CLOSED", "displayName": "Fermé" }
]
```

#### `GET /api/support/priorities`
- **Description**: Liste des priorités disponibles
- **Réponse**:
```json
[
  { "value": "LOW", "displayName": "Basse" },
  { "value": "MEDIUM", "displayName": "Moyenne" },
  { "value": "HIGH", "displayName": "Élevée" },
  { "value": "URGENT", "displayName": "Urgente" }
]
```

#### `GET /api/support/is-admin`
- **Description**: Vérifier si l'utilisateur connecté est admin
- **Réponse**: `{ "isAdmin": true/false }`

## Transitions de Statut Autorisées

```
OPEN → IN_PROGRESS, CLOSED
IN_PROGRESS → RESOLVED, CLOSED, OPEN
RESOLVED → CLOSED, OPEN
CLOSED → OPEN (réouverture)
```

## Messages Automatiques

Lors du changement de statut, un message automatique est envoyé dans le chat :

- **OPEN**: "🔄 Le ticket a été rouvert."
- **IN_PROGRESS**: "⚡ Le ticket est maintenant en cours de traitement."
- **RESOLVED**: "✅ Le ticket a été marqué comme résolu."
- **CLOSED**: "🔒 Le ticket a été fermé."

Si une note admin est fournie, elle est ajoutée au message.

## Gestion des Permissions

- **Admin ID**: 1 (hardcodé)
- **Vérification**: Tous les endpoints admin vérifient `isUserAdmin(userId)`
- **Erreur 403**: Si l'utilisateur n'est pas admin

## Exemples d'Utilisation

### 1. Changer le statut d'un ticket
```bash
PUT /api/support/admin/ticket/TICKET-123456/status
{
  "status": "IN_PROGRESS",
  "priority": "HIGH",
  "adminNote": "Problème identifié, en cours de résolution"
}
```

### 2. Marquer comme résolu
```bash
PUT /api/support/admin/ticket/TICKET-123456/status
{
  "status": "RESOLVED",
  "adminNote": "Problème résolu, paiement traité"
}
```

### 3. Fermer un ticket
```bash
PUT /api/support/admin/ticket/TICKET-123456/status
{
  "status": "CLOSED"
}
```

### 4. Récupérer les tickets urgents
```bash
GET /api/support/admin/tickets/urgent
```

### 5. Rechercher des tickets
```bash
GET /api/support/admin/tickets/search?query=paiement
```

## Fonctionnalités Avancées

### Tri Automatique
- **Tickets actifs**: Triés par priorité (URGENT → HIGH → MEDIUM → LOW) puis par date
- **Tickets fermés**: Triés par date de mise à jour (plus récents en premier)

### Statistiques Temps Réel
- Compteurs par statut et priorité
- Tickets récents (7 derniers jours)
- Temps de résolution moyen

### Messages Contextuels
- Chaque changement de statut génère un message dans le chat
- L'utilisateur est notifié automatiquement
- Historique complet des actions admin

## Gestion des Erreurs

### Codes de Réponse
- **200**: Succès
- **400**: Erreur de validation (statut invalide, etc.)
- **403**: Accès refusé (pas admin)
- **404**: Ticket non trouvé

### Messages d'Erreur
```json
{
  "error": "Invalid status transition from CLOSED to IN_PROGRESS"
}
```

## Intégration Frontend

### Dashboard Admin
1. Appeler `/admin/dashboard` pour les stats de base
2. Appeler `/admin/tickets/active` pour les tickets à traiter
3. Appeler `/admin/tickets/urgent` pour les priorités

### Gestion des Tickets
1. Liste des tickets avec filtres par statut/priorité
2. Boutons d'action pour changer le statut
3. Formulaire avec note admin optionnelle
4. Messages automatiques visibles dans le chat

### Recherche
1. Barre de recherche avec endpoint `/search`
2. Filtres par statut et priorité
3. Tri automatique par priorité

Cette API est maintenant complètement fonctionnelle pour la gestion des tickets de support par l'admin !
