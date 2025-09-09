# Dashboard Support Admin - Guide d'Utilisation

## Vue d'ensemble

Le Dashboard Support Admin est une interface complète permettant à l'administrateur (ID = 1) de gérer tous les tickets de support de l'application Fantasy Football.

## Accès au Dashboard

1. **Connexion Admin** : Connectez-vous avec un compte admin (ID = 1)
2. **Navigation** : Allez dans `/admin/dashboard` ou utilisez la sidebar admin
3. **Menu** : Cliquez sur "Support" → "Dashboard Support"

## Fonctionnalités Principales

### 📊 Statistiques en Temps Réel
- **Total des tickets** : Nombre total de tickets créés
- **Tickets ouverts** : Tickets en attente de traitement
- **En cours** : Tickets pris en charge par l'admin
- **Tickets urgents** : Tickets avec priorité URGENT ou HIGH
- **Résolus** : Tickets marqués comme résolus
- **Fermés** : Tickets fermés définitivement

### 🔍 Filtrage et Recherche
- **Filtre par statut** : Tous, Actifs, Urgents, Fermés
- **Filtre par priorité** : Basse, Moyenne, Élevée, Urgente
- **Recherche textuelle** : Dans le sujet, description, ID ticket, nom utilisateur

### 📋 Gestion des Tickets

#### Actions Disponibles
1. **Ouvrir le chat** : Accéder directement à la conversation avec l'utilisateur
2. **Voir les détails** : Informations complètes du ticket
3. **Modifier le statut** : Changer le statut et la priorité
4. **S'assigner** : Prendre en charge un ticket non assigné

#### Changement de Statut
- **OPEN** → **IN_PROGRESS** : Prendre en charge le ticket
- **IN_PROGRESS** → **RESOLVED** : Marquer comme résolu
- **RESOLVED** → **CLOSED** : Fermer définitivement
- **CLOSED** → **OPEN** : Réouvrir si nécessaire

#### Messages Automatiques
Chaque changement de statut génère automatiquement un message dans le chat :
- 🔄 "Le ticket a été rouvert"
- ⚡ "Le ticket est maintenant en cours de traitement"
- ✅ "Le ticket a été marqué comme résolu"
- 🔒 "Le ticket a été fermé"

## Interface Utilisateur

### Tableau des Tickets
- **ID** : Identifiant unique du ticket (ex: TICKET-123456)
- **Sujet** : Titre du problème
- **Utilisateur** : Nom et email de l'utilisateur
- **Statut** : Badge coloré selon le statut
- **Priorité** : Badge coloré selon la priorité
- **Date** : Date de création
- **Actions** : Boutons d'action rapide

### Modals
1. **Modal de changement de statut** :
   - Sélection du nouveau statut
   - Modification de la priorité
   - Note optionnelle pour l'utilisateur

2. **Modal de détails** :
   - Informations complètes du ticket
   - Historique des modifications
   - Accès direct au chat

## Workflow Recommandé

### 1. Vérification Quotidienne
1. Ouvrir le dashboard
2. Vérifier les tickets urgents
3. Examiner les tickets actifs
4. Traiter les nouveaux tickets

### 2. Traitement d'un Ticket
1. **Cliquer sur "Ouvrir le chat"** pour voir la conversation
2. **Lire le problème** de l'utilisateur
3. **S'assigner le ticket** si nécessaire
4. **Changer le statut** en "En cours"
5. **Résoudre le problème** via le chat
6. **Marquer comme résolu** avec une note explicative
7. **Fermer le ticket** une fois confirmé par l'utilisateur

### 3. Gestion des Priorités
- **URGENT** : Problèmes bloquants (paiement, accès)
- **HIGH** : Problèmes importants (fonctionnalités)
- **MEDIUM** : Problèmes standards
- **LOW** : Questions générales

## Codes de Couleur

### Statuts
- 🟡 **Ouvert** : Jaune (en attente)
- 🔵 **En cours** : Bleu (en traitement)
- 🟢 **Résolu** : Vert (problème résolu)
- 🔴 **Fermé** : Rouge (fermé définitivement)

### Priorités
- 🔴 **Urgente** : Rouge (critique)
- 🟠 **Élevée** : Orange (importante)
- 🔵 **Moyenne** : Bleu (standard)
- 🟢 **Basse** : Vert (faible)

## Raccourcis et Conseils

### Raccourcis Clavier
- **F5** : Actualiser le dashboard
- **Ctrl+F** : Rechercher dans les tickets
- **Échap** : Fermer les modals

### Conseils d'Utilisation
1. **Actualisez régulièrement** pour voir les nouveaux tickets
2. **Utilisez les filtres** pour organiser votre travail
3. **Ajoutez des notes** lors des changements de statut
4. **Communiquez clairement** avec les utilisateurs
5. **Fermez les tickets** une fois résolus

## Gestion des Erreurs

### Erreurs Courantes
- **403 Forbidden** : Vérifiez que vous êtes connecté en tant qu'admin
- **404 Not Found** : Le ticket n'existe pas
- **400 Bad Request** : Données invalides dans le formulaire

### Solutions
1. **Recharger la page** (F5)
2. **Vérifier la connexion** admin
3. **Contacter le développeur** si le problème persiste

## Intégration avec le Chat

Le dashboard est entièrement intégré avec le système de chat :
- **Ouverture directe** du chat depuis le dashboard
- **Messages automatiques** lors des changements de statut
- **Synchronisation** en temps réel
- **Historique complet** des interactions

## Support Technique

En cas de problème avec le dashboard :
1. Vérifiez la console du navigateur (F12)
2. Vérifiez la connexion au backend
3. Contactez l'équipe de développement

---

**Note** : Ce dashboard est réservé aux administrateurs (ID = 1). Les utilisateurs normaux ne peuvent pas y accéder.
