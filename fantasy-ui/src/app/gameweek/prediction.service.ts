// prediction.service.ts - FIXED INTERFACES
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PredictionDTO {
  matchId: number;
  predictedResult: 'HOME_WIN' | 'DRAW' | 'AWAY_WIN';
  predictedHomeScore?: number | null;
  predictedAwayScore?: number | null;
}

// ✅ FIXED: Make session fields optional since they go as query params
export interface GameweekPredictionSubmissionDTO {
  userId: number;
  gameweekId: number;
  competition: string;
  predictions: PredictionDTO[];
  // ✅ Optional fields (sent as query parameters)
  sessionType?: string;
  buyInAmount?: number;
  sessionId?: number | null;
  isPrivate?: boolean;
  sessionDescription?: string;
  complete?: boolean;
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

  constructor(private http: HttpClient) {}

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

    // ✅ CLEAN REQUEST BODY (remove session fields to avoid confusion)
    const requestBody: GameweekPredictionSubmissionDTO = {
      userId: submissionDTO.userId,
      gameweekId: submissionDTO.gameweekId,
      competition: submissionDTO.competition,
      predictions: submissionDTO.predictions,
      sessionType: sessionType,         // <-- ADD THIS
      buyInAmount: cleanBuyInAmount,    // <-- ADD THIS
      complete: true
    };

    // Debug logging
    console.log('[PREDICTION SERVICE] Request details:');
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