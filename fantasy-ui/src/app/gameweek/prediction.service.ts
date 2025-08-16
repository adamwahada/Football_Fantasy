// prediction.service.ts - Enhanced for better error handling with new backend

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../core/services/auth.service';

// Types
export interface PredictionDTO {
  matchId: number;
  predictedResult: 'HOME_WIN' | 'DRAW' | 'AWAY_WIN';
  predictedHomeScore?: number | null;
  predictedAwayScore?: number | null;
}

export interface GameweekPredictionSubmissionDTO {
  userId: number;
  gameweekId: number;
  competition: string;
  predictions: PredictionDTO[];
  sessionType: string;
  buyInAmount: number;
  sessionId?: number | null;
  isPrivate: boolean;
  sessionDescription?: string;
  complete: boolean;
}

export interface SubmitPredictionResponse {
  success: boolean;
  message: string;
  data: {
    predictions: any[];
    sessionParticipation: any;
  };
  timestamp: string;
}

export interface BackendErrorResponse {
  success: false;
  error: string;
  message: string;
  details?: {
    required?: string;
    current?: string;
    userId?: string;
    shortage?: string;
  };
  suggestions?: {
    action?: string;
    minimumRequired?: string;
  };
  timestamp: string;
  path: string;
}

export class PredictionError extends Error {
  constructor(
    message: string,
    public errorCode: string,
    public details?: any,
    public statusCode?: number
  ) {
    super(message);
    this.name = 'PredictionError';
  }
}

@Injectable({
  providedIn: 'root'
})
export class PredictionService {
  private apiUrl = 'http://localhost:9090/fantasy/api/predictions';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  // -----------------------
  // Public Method
  // -----------------------
  submitPredictionsAndJoinSession(
    submissionDTO: GameweekPredictionSubmissionDTO,
    sessionType: string,
    buyInAmount: number,
    isPrivate: boolean,
    accessKey?: string
  ): Observable<SubmitPredictionResponse> {

    console.log('[PREDICTION SERVICE] 🚀 Starting submission process...');

    // Validation
    const validationError = this.validateSubmission(sessionType, buyInAmount);
    if (validationError) {
      console.error('[PREDICTION SERVICE] ❌ Validation failed:', validationError);
      return throwError(() => validationError);
    }

    // Authentication check
    if (!this.authService.isLoggedIn()) {
      console.error('[PREDICTION SERVICE] ❌ User not logged in');
      return this.error('Please log in to participate.', 'USER_NOT_LOGGED_IN');
    }

    const currentUserId = this.authService.getCurrentUserId();
    if (!currentUserId) {
      console.error('[PREDICTION SERVICE] ❌ User ID not found');
      return this.error('Could not determine user ID.', 'USER_ID_NOT_FOUND');
    }

    // Build query params
    let params = new HttpParams()
      .set('sessionType', sessionType.trim())
      .set('buyInAmount', buyInAmount.toString())
      .set('isPrivate', isPrivate.toString());

    if (accessKey?.trim()) {
      params = params.set('accessKey', accessKey.trim());
    }

    // Build request body
    const requestBody: GameweekPredictionSubmissionDTO = {
      ...submissionDTO,
      userId: currentUserId,
      sessionType,
      buyInAmount,
      isPrivate,
      complete: true
    };

    console.log('[PREDICTION SERVICE] 📤 Sending request:', {
      url: `${this.apiUrl}/submit-predictions`,
      params: params.toString(),
      body: requestBody
    });

    return this.http.post<SubmitPredictionResponse>(
      `${this.apiUrl}/submit-predictions`,
      requestBody,
      { params }
    ).pipe(
      catchError((error) => this.handleHttpError(error))
    );
  }

  // -----------------------
  // Private Helpers
  // -----------------------
  private validateSubmission(sessionType: string, buyInAmount: number): PredictionError | null {
    if (isNaN(Number(buyInAmount)) || buyInAmount < 0) {
      return new PredictionError(`Invalid buyInAmount: ${buyInAmount}`, 'INVALID_BUY_IN_AMOUNT');
    }
    if (!sessionType?.trim()) {
      return new PredictionError('SessionType is required', 'SESSION_TYPE_REQUIRED');
    }
    return null;
  }

  private handleHttpError(error: HttpErrorResponse): Observable<never> {
    console.error('[PREDICTION SERVICE] 🔥 HTTP Error Details:', {
      status: error.status,
      statusText: error.statusText,
      error: error.error,
      url: error.url,
      message: error.message
    });

    // Network error
    if (error.status === 0) {
      return this.error(
        'Unable to connect to server. Please check your internet connection.',
        'NETWORK_ERROR',
        null,
        0
      );
    }

    // Parse backend error response
    const backendError = error.error as BackendErrorResponse;
    
    // Check if we have a proper backend error response
    if (this.isBackendError(backendError)) {
      console.log('[PREDICTION SERVICE] 📋 Backend error response:', backendError);

      // Handle specific business logic errors (422 status)
      if (error.status === 422) {
        console.log('[PREDICTION SERVICE] 📋 422 Business logic error:', backendError);
        
        // ✅ CRITICAL FIX: For 422 errors, pass the backend error details directly
        // so the modal can access them properly
        return this.error(
          backendError.message,
          backendError.error,
          {
            ...backendError,
            // Ensure the details are accessible at the top level for the modal
            required: backendError.details?.required,
            current: backendError.details?.current,
            shortage: backendError.details?.shortage,
            userId: backendError.details?.userId
          },
          422
        );
      }

      // Handle other backend errors
      return this.error(
        backendError.message,
        backendError.error,
        backendError,
        error.status
      );
    }

    // Handle non-JSON error responses
    if (error.status >= 400 && error.status < 500) {
      return this.error(
        'Request failed. Please check your input and try again.',
        'CLIENT_ERROR',
        { originalError: error },
        error.status
      );
    }

    if (error.status >= 500) {
      return this.error(
        'Server error. Please try again later.',
        'SERVER_ERROR',
        { originalError: error },
        error.status
      );
    }

    // Fallback for unexpected errors
    return this.error(
      `Unexpected error (${error.status}): ${error.message || 'Unknown error'}`,
      'HTTP_ERROR',
      { originalError: error },
      error.status
    );
  }

  private isBackendError(obj: any): obj is BackendErrorResponse {
    return obj && 
           typeof obj === 'object' && 
           'success' in obj && 
           obj.success === false &&
           'error' in obj && 
           'message' in obj &&
           typeof obj.error === 'string' &&
           typeof obj.message === 'string';
  }

  private error(message: string, code: string, details?: any, statusCode?: number): Observable<never> {
    const error = new PredictionError(message, code, details, statusCode);
    console.error('[PREDICTION SERVICE] ⚠️ Creating error:', error);
    return throwError(() => error);
  }

  private logDebug(message: string, data?: any) {
    console.log(`[PREDICTION SERVICE] ${message}`, data || '');
  }

  private logError(message: string, error: any) {
    console.error(`[PREDICTION SERVICE] ${message}`, error);
  }

  // -----------------------
  // Public Utility Methods
  // -----------------------
  
  /**
   * Get user-friendly error message based on error type
   */
  getErrorMessage(error: any): string {
    console.log('[PREDICTION SERVICE] 📝 Processing error for display:', error);

    if (error instanceof PredictionError) {
      switch (error.errorCode) {
        case 'INSUFFICIENT_BALANCE':
          if (error.details?.details) {
            const details = error.details.details;
            return `Solde insuffisant. Vous avez besoin de ${details.required}€ mais n'avez que ${details.current}€.`;
          }
          return error.message;

        case 'INVALID_BUY_IN_AMOUNT':
          return 'Veuillez entrer un montant de mise valide.';

        case 'SESSION_TYPE_REQUIRED':
          return 'Veuillez sélectionner un type de session.';

        case 'USER_NOT_LOGGED_IN':
          return 'Veuillez vous connecter pour participer.';

        case 'NETWORK_ERROR':
          return 'Erreur de connexion. Vérifiez votre internet et réessayez.';

        case 'USER_NOT_FOUND':
          return 'Utilisateur non trouvé. Veuillez vous reconnecter.';

        case 'GAMEWEEK_NOT_FOUND':
          return 'Gameweek non trouvée. Veuillez actualiser la page.';

        case 'ALREADY_JOINED':
          return 'Vous avez déjà rejoint cette session.';

        case 'SESSION_FULL':
          return 'Cette session est complète.';

        case 'TERMS_NOT_ACCEPTED':
          return 'Vous devez accepter les conditions d\'utilisation.';

        case 'VALIDATION_ERROR':
          return 'Données invalides. Veuillez vérifier vos informations.';

        case 'INTERNAL_SERVER_ERROR':
        case 'SERVER_ERROR':
          return 'Erreur serveur. Veuillez réessayer plus tard.';

        default:
          return error.message || 'Une erreur inattendue est survenue.';
      }
    }

    // Handle direct backend error objects
    if (error?.error === 'INSUFFICIENT_BALANCE') {
      const details = error.details;
      if (details?.required && details?.current) {
        return `Solde insuffisant. Vous avez besoin de ${details.required}€ mais n'avez que ${details.current}€.`;
      }
      return error.message || 'Solde insuffisant pour rejoindre cette session.';
    }

    // Fallback
    return error?.message || 'Une erreur inattendue est survenue. Veuillez réessayer.';
  }

  /**
   * Check if error should keep modal open (business logic errors)
   */
  shouldKeepModalOpen(error: any): boolean {
    if (error instanceof PredictionError) {
      // Business logic errors that user can potentially fix
      const retryableErrors = [
        'INSUFFICIENT_BALANCE',
        'INVALID_BUY_IN_AMOUNT',
        'SESSION_TYPE_REQUIRED',
        'VALIDATION_ERROR',
        'ALREADY_JOINED'
      ];
      return retryableErrors.includes(error.errorCode) || error.statusCode === 422;
    }

    // Check backend error response
    const backendError = error?.error || error;
    if (backendError?.error === 'INSUFFICIENT_BALANCE') {
      return true;
    }

    return false;
  }

  /**
   * Get error details for display
   */
  getErrorDetails(error: any): any {
    if (error instanceof PredictionError) {
      return error.details;
    }
    return error?.details || null;
  }
}