import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private messageSubject = new BehaviorSubject<string>('');
  private typeSubject = new BehaviorSubject<'success' | 'error'>('success');
  
  message$ = this.messageSubject.asObservable();
  type$ = this.typeSubject.asObservable();

  get message(): string {
    return this.messageSubject.value;
  }

  get type(): 'success' | 'error' {
    return this.typeSubject.value;
  }

  show(msg: string, type: 'success' | 'error' = 'success') {
    console.log('🔔 NotificationService: Showing message:', msg);
    this.messageSubject.next(msg);
    this.typeSubject.next(type);
    
    // Clear after 5 seconds
    if (type === 'success') {
      setTimeout(() => this.clear(), 5000);
    }
  }

  clear() {
    this.messageSubject.next('');
    this.typeSubject.next('success');
  }
}