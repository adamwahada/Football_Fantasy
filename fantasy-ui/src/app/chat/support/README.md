# Système de Support - Football Fantasy

## Vue d'ensemble

Ce module implémente un système complet de support client pour l'application Football Fantasy, permettant aux utilisateurs de créer des tickets de support et de communiquer avec l'équipe d'assistance.

## Architecture

### Composants

1. **SupportPageComponent** (`support-page/`)
   - Page principale du support
   - Gère l'affichage du formulaire ou de la liste des tickets
   - Navigation entre les différentes vues

2. **SupportTicketFormComponent** (`support-ticket-form/`)
   - Formulaire de création de ticket
   - Validation des champs
   - Sélection du type de support
   - Conseils contextuels

3. **SupportButtonComponent** (`support-button/`)
   - Bouton d'accès rapide au support
   - Intégré dans l'interface de chat existante

### Services

1. **SupportService** (`service/support.service.ts`)
   - Gestion des appels API
   - Méthodes utilitaires pour l'affichage
   - Gestion des types et statuts

### Modèles

1. **support.models.ts**
   - Interfaces TypeScript
   - Enums pour les types et statuts
   - DTOs pour les requêtes/réponses

## Fonctionnalités

### Pour l'Utilisateur

1. **Création de ticket**
   - Sélection du type de problème (Paiement, Technique, Compte, Général)
   - Saisie du sujet et description
   - Validation en temps réel
   - Conseils contextuels selon le type

2. **Gestion des tickets**
   - Vue d'ensemble de tous les tickets
   - Statut en temps réel (Ouvert, En cours, Résolu, Fermé)
   - Accès direct au chat de support

3. **Interface intuitive**
   - Design moderne et responsive
   - Animations fluides
   - Feedback visuel immédiat

### Types de Support

- **💳 Paiement** : Problèmes de facturation, remboursements, transactions
- **🔧 Technique** : Bugs, erreurs, problèmes d'interface
- **👤 Compte/Profil** : Connexion, paramètres, données personnelles
- **❓ Général** : Questions diverses, informations

## Intégration

### Routes

```typescript
{ 
  path: 'support', 
  loadChildren: () => import('./chat/support/support.module').then(m => m.SupportModule)
}
```

### Utilisation dans le Chat

```html
<app-support-button (supportClicked)="onSupportClick()"></app-support-button>
```

## API Endpoints

### Backend (Java Spring Boot)

- `POST /api/support/ticket` - Créer un ticket
- `GET /api/support/my-tickets` - Récupérer les tickets utilisateur
- `GET /api/support/types` - Types de support disponibles
- `GET /api/support/is-admin` - Vérifier le rôle admin

## Workflow

1. **Utilisateur** : Clique sur "Support" dans le chat
2. **Formulaire** : Remplit les informations du problème
3. **Création** : Ticket créé automatiquement
4. **Chat** : Redirection vers le chat de support
5. **Admin** : Reçoit notification et peut répondre
6. **Résolution** : Admin marque comme résolu

## Design System

### Couleurs
- **Primaire** : `#3498db` (Bleu)
- **Succès** : `#2ecc71` (Vert)
- **Erreur** : `#e74c3c` (Rouge)
- **Support** : `#ff6b6b` (Rouge support)

### Typographie
- **Famille** : 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif
- **Titres** : 600-700 weight
- **Corps** : 400 weight

### Responsive
- **Desktop** : Layout en grille, cartes larges
- **Tablet** : Adaptation des colonnes
- **Mobile** : Layout vertical, boutons compacts

## Développement

### Installation

1. Assurez-vous que le backend est démarré
2. Vérifiez les URLs dans `support.service.ts`
3. Importez le module dans `app.routes.ts`

### Tests

```bash
# Tests unitaires
ng test --include=**/support/**/*.spec.ts

# Tests e2e
ng e2e --specs=support.e2e-spec.ts
```

### Build

```bash
ng build --configuration=production
```

## Prochaines étapes

1. **Dashboard Admin** : Interface de gestion des tickets
2. **Notifications** : Système de notifications en temps réel
3. **Statistiques** : Métriques de performance du support
4. **FAQ** : Base de connaissances intégrée
5. **Chatbot** : Assistant automatique pour les questions simples

## Support

Pour toute question sur l'implémentation, contactez l'équipe de développement.
