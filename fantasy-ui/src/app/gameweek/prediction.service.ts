// prediction.service.ts - FIXED INTERFACES
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../core/services/auth.service';

export interface PredictionDTO {
  matchId: number;
  predictedResult: 'HOME_WIN' | 'DRAW' | 'AWAY_WIN';
  predictedHomeScore?: number | null;
  predictedAwayScore?: number | null;
}

// ✅ Match backend DTO structure
export interface GameweekPredictionSubmissionDTO {
  userId: number;          // Required by backend
  gameweekId: number;      // Required
  competition: string;     // LeagueTheme enum
  predictions: PredictionDTO[];  // Required
  sessionType: string;     // Required - SessionType enum
  buyInAmount: number;     // Required - matches BigDecimal
  sessionId?: number | null;
  isPrivate: boolean;      // Defaults to false
  sessionDescription?: string;  // Optional
  complete: boolean;       // For validation
}

export interface SubmitPredictionResponse {
  predictions: any[];
  sessionParticipation: any;
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

  submitPredictionsAndJoinSession(
    submissionDTO: GameweekPredictionSubmissionDTO,
    sessionType: string,
    buyInAmount: number,
    isPrivate: boolean,
    accessKey?: string
  ): Observable<SubmitPredictionResponse> {
    
    // Validate buyInAmount
    const cleanBuyInAmount = Number(buyInAmount);
    if (isNaN(cleanBuyInAmount) || cleanBuyInAmount < 0) {
      throw new Error(`Invalid buyInAmount: ${buyInAmount}`);
    }

    // Validate sessionType
    if (!sessionType || sessionType.trim() === '') {
      throw new Error('SessionType is required');
    }

    // ✅ SEND REQUIRED FIELDS AS QUERY PARAMETERS (as backend expects)
    let params = new HttpParams()
      .set('sessionType', sessionType.trim())
      .set('buyInAmount', cleanBuyInAmount.toString())
      .set('isPrivate', isPrivate.toString());
    
    // Add accessKey only if provided and not empty
    if (accessKey && accessKey.trim().length > 0) {
      params = params.set('accessKey', accessKey.trim());
    }

    // Check if user is logged in and get their ID
    if (!this.authService.isLoggedIn()) {
      throw new Error('User must be logged in to submit predictions');
    }
    
    const currentUserId = this.authService.getCurrentUserId();
    if (!currentUserId) {
      throw new Error('Could not determine user ID');
    }

    // ✅ BUILD REQUEST BODY with all required fields
    const requestBody: GameweekPredictionSubmissionDTO = {
      userId: currentUserId,
      gameweekId: submissionDTO.gameweekId,
      competition: submissionDTO.competition,
      predictions: submissionDTO.predictions,
      sessionType: sessionType,
      buyInAmount: cleanBuyInAmount,
      isPrivate: isPrivate,
      complete: true
    };

    // Debug logging
    console.log('[PREDICTION SERVICE] Request details:');
    console.log('- User ID:', currentUserId);
    console.log('- sessionType param:', sessionType, typeof sessionType);
    console.log('- buyInAmount param:', cleanBuyInAmount, typeof cleanBuyInAmount);
    console.log('- isPrivate param:', isPrivate, typeof isPrivate);
    console.log('- accessKey param:', accessKey);
    console.log('- Final query params:', params.toString());
    console.log('- Request body:', JSON.stringify(requestBody, null, 2));

    const finalUrl = `${this.apiUrl}/submit-predictions`;
    console.log('[PREDICTION SERVICE] Final URL with params:', `${finalUrl}?${params.toString()}`);

    return this.http.post<SubmitPredictionResponse>(
      finalUrl,
      requestBody,
      { params }
    );
  }
}