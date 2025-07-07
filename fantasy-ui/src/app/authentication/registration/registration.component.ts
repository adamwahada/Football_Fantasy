import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SnackbarErrorComponent } from '../snackbar-error/snackbar-error.component';

declare global {
  interface Window {
    grecaptcha: {
      render: (element: string | HTMLElement, options: any) => number;
      getResponse: (widgetId: number) => string;
      reset: (widgetId: number) => void;
      ready: (callback: () => void) => void;
      execute: (widgetId: number, options?: any) => Promise<string>;
    };
    onRecaptchaLoad: () => void;
  }
}

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [
    ReactiveFormsModule, 
    CommonModule, 
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    // SnackbarErrorComponent
  ],
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.scss']
})
export class RegistrationComponent implements OnInit, AfterViewInit, OnDestroy {
  currentStep = 1;
  basicForm: FormGroup;
  customForm: FormGroup;
  errorMessage: string = '';
  fieldErrors: { [key: string]: string } = {};
  isSubmitting = false;
  
  recaptchaInitialized = false;
  private recaptchaWidgetId: number | null = null;
  recaptchaToken: string | null = null;
  private readonly RECAPTCHA_SITE_KEY = '6LfgHGUrAAAAAIYJZpivfvWwdel4PdGulFnPSXSF';
  private readonly RECAPTCHA_CONTAINER_ID = 'recaptcha-container';
  showModal = true;

  constructor(private fb: FormBuilder, private authService: AuthService, private router: Router, private snackBar: MatSnackBar) {
    console.log('🔍 RegistrationComponent initialized');
    
    this.basicForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), this.usernameValidator]],
      firstName: ['', [Validators.required, Validators.minLength(2), this.nameValidator]],
      lastName: ['', [Validators.required, Validators.minLength(2), this.nameValidator]],
      email: ['', [Validators.required, Validators.email, this.emailValidator]],
      password: ['', [Validators.required, Validators.minLength(8), this.passwordValidator]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordsMatchValidator });

    this.customForm = this.fb.group({
      phone: ['', [Validators.required, this.phoneValidator]],
      country: ['', [Validators.required, Validators.minLength(2)]],
      address: ['', [Validators.required]],
      postalNumber: ['', [this.postalCodeValidator]],
      birthDate: ['', [Validators.required, this.birthDateValidator]],
      referralCode: ['', [Validators.pattern(/^[A-Za-z0-9]{6,12}$/)]],
      termsAccepted: [false, Validators.requiredTrue]
    });
  }

  // ✅ Validateurs personnalisés avec messages spécifiques
  usernameValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const username = control.value.trim();
    if (username.length < 3) {
      return { username: { message: 'Le nom d\'utilisateur doit contenir au moins 3 caractères' } };
    }
    if (!/^[a-zA-Z0-9_-]+$/.test(username)) {
      return { username: { message: 'Le nom d\'utilisateur ne peut contenir que des lettres, chiffres, tirets et underscores' } };
    }
    return null;
  }

  nameValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const name = control.value.trim();
    if (name.length < 2) {
      return { name: { message: 'Le nom doit contenir au moins 2 caractères' } };
    }
    if (!/^[a-zA-ZÀ-ÿ\s-']+$/.test(name)) {
      return { name: { message: 'Le nom ne peut contenir que des lettres, espaces, tirets et apostrophes' } };
    }
    return null;
  }

  emailValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const email = control.value.trim().toLowerCase();
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    
    if (!emailRegex.test(email)) {
      return { email: { message: 'Veuillez saisir une adresse email valide (ex: nom@domaine.com)' } };
    }
    return null;
  }

  passwordValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const password = control.value;
    const errors: string[] = [];
    
    if (password.length < 8) {
      errors.push('au moins 8 caractères');
    }
    if (!/[A-Z]/.test(password)) {
      errors.push('une lettre majuscule');
    }
    if (!/[a-z]/.test(password)) {
      errors.push('une lettre minuscule');
    }
    if (!/[0-9]/.test(password)) {
      errors.push('un chiffre');
    }
    
    if (errors.length > 0) {
      return { 
        password: { 
          message: `Le mot de passe doit contenir ${errors.join(', ')}` 
        } 
      };
    }
    return null;
  }

  phoneValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value || control.value.trim() === '') return null;
    
    const phone = control.value.replace(/\s/g, '');
    if (!/^[+]?[0-9]{8,15}$/.test(phone)) {
      return { 
        phone: { 
          message: 'Le numéro de téléphone doit contenir entre 8 et 15 chiffres (+ optionnel)' 
        } 
      };
    }
    return null;
  }

  postalCodeValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value || control.value.trim() === '') return null;
    
    const postalCode = control.value.trim();
    if (!/^[0-9]{4,10}$/.test(postalCode)) {
      return { 
        postalCode: { 
          message: 'Le code postal doit contenir uniquement des chiffres (4 à 10 caractères)' 
        } 
      };
    }
    return null;
  }

  birthDateValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) return null;
    
    const birthDate = new Date(control.value);
    const today = new Date();
    const minAge = 13;
    const maxAge = 120;
    
    if (isNaN(birthDate.getTime())) {
      return { birthDate: { message: 'Veuillez sélectionner une date de naissance valide' } };
    }
    
    const age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    const dayDiff = today.getDate() - birthDate.getDate();
    
    const actualAge = age - (monthDiff < 0 || (monthDiff === 0 && dayDiff < 0) ? 1 : 0);
    
    if (actualAge < minAge) {
      return { birthDate: { message: `Vous devez avoir au moins ${minAge} ans pour vous inscrire` } };
    }
    
    if (actualAge > maxAge) {
      return { birthDate: { message: 'Veuillez vérifier votre date de naissance' } };
    }
    
    return null;
  }

  passwordsMatchValidator(group: FormGroup): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    if (password !== confirmPassword) {
      return { passwordsMismatch: { message: 'Les mots de passe ne correspondent pas' } };
    }
    return null;
  }

  ngOnInit() {
    console.log('🔍 ngOnInit called');
    // Charger le script reCAPTCHA
    if (!document.getElementById('recaptcha-script')) {
      const script = document.createElement('script');
      script.id = 'recaptcha-script';
      script.src = 'https://www.google.com/recaptcha/api.js?render=explicit&hl=fr';
      script.async = true;
      script.defer = true;
      
      script.onload = () => {
        console.log('✅ reCAPTCHA script loaded');
        if (this.currentStep === 2) {
          this.initializeRecaptcha();
        }
      };
      
      document.head.appendChild(script);
    } else if (window.grecaptcha && this.currentStep === 2) {
      this.initializeRecaptcha();
    }
  }

  ngAfterViewInit() {
    console.log('🔍 ngAfterViewInit called');
    
    setTimeout(() => {
      if (this.currentStep === 2 && !this.recaptchaInitialized) {
        this.initializeRecaptcha();
      }
    }, 1000);
  }

  ngOnDestroy() {
    if (this.recaptchaWidgetId !== null) {
      try {
        window.grecaptcha.reset(this.recaptchaWidgetId);
      } catch (error) {
        console.error('Error cleaning up reCAPTCHA:', error);
      }
    }
  }

  private initializeRecaptcha() {
    console.log('🔍 Initializing reCAPTCHA...');
    
    if (this.recaptchaInitialized) {
      console.log('⚠️ reCAPTCHA already initialized');
      return;
    }

    if (!window.grecaptcha) {
      console.log('❌ grecaptcha not available');
      return;
    }

    const container = document.getElementById(this.RECAPTCHA_CONTAINER_ID);
    if (!container) {
      console.log('❌ reCAPTCHA container not found');
      return;
    }

    try {
      console.log('🚀 Rendering reCAPTCHA...');
      container.innerHTML = '';
      
      this.recaptchaWidgetId = window.grecaptcha.render(container, {
        sitekey: this.RECAPTCHA_SITE_KEY,
        theme: 'light',
        size: 'normal',
        callback: (token: string) => {
          console.log('✅ reCAPTCHA verified');
          this.recaptchaToken = token;
          this.clearFieldError('recaptcha');
        },
        'expired-callback': () => {
          console.log('⚠️ reCAPTCHA expired');
          this.recaptchaToken = null;
          this.setFieldError('recaptcha', 'La vérification reCAPTCHA a expiré. Veuillez recommencer.');
        },
        'error-callback': () => {
          console.log('❌ reCAPTCHA error');
          this.recaptchaToken = null;
          this.setFieldError('recaptcha', 'Erreur de vérification reCAPTCHA. Veuillez réessayer.');
        }
      });

      this.recaptchaInitialized = true;
      console.log('✅ reCAPTCHA initialized successfully');
    } catch (error) {
      console.error('❌ Failed to initialize reCAPTCHA:', error);
      this.setFieldError('recaptcha', 'Impossible de charger reCAPTCHA. Veuillez actualiser la page.');
    }
  }

  // ✅ Gestion des erreurs par champ
  setFieldError(fieldName: string, message: string) {
    this.fieldErrors[fieldName] = message;
  }

  clearFieldError(fieldName: string) {
    delete this.fieldErrors[fieldName];
  }

  getFieldError(fieldName: string): string {
    const form = this.currentStep === 1 ? this.basicForm : this.customForm;
    const control = form.get(fieldName);
    
    // Erreur serveur spécifique
    if (this.fieldErrors[fieldName]) {
      return this.fieldErrors[fieldName];
    }
    
    // Erreurs de validation
    if (control?.errors && (control.touched || control.dirty)) {
      const errors = control.errors;
      
      if (errors['required']) {
        return this.getRequiredMessage(fieldName);
      } else if (errors['username']) {
        return errors['username'].message;
      } else if (errors['name']) {
        return errors['name'].message;
      } else if (errors['email']) {
        return errors['email'].message;
      } else if (errors['password']) {
        return errors['password'].message;
      } else if (errors['phone']) {
        return errors['phone'].message;
      } else if (errors['postalCode']) {
        return errors['postalCode'].message;
      } else if (errors['birthDate']) {
        return errors['birthDate'].message;
      }
    }
    // Erreur de correspondance des mots de passe
    if (fieldName === 'confirmPassword' && form.errors && form.errors['passwordsMismatch'] && (form.get('confirmPassword')?.touched || form.get('confirmPassword')?.dirty)) {
      return form.errors['passwordsMismatch'].message;
    }
    return '';
  }

  private getRequiredMessage(fieldName: string): string {
    const messages: { [key: string]: string } = {
      username: 'Le nom d\'utilisateur est requis',
      firstName: 'Le prénom est requis',
      lastName: 'Le nom est requis',
      email: 'L\'adresse email est requise',
      password: 'Le mot de passe est requis',
      confirmPassword: 'La confirmation du mot de passe est requise',
      phone: 'Le numéro de téléphone est requis',
      country: 'Le pays est requis',
      address: 'L\'adresse est requise',
      postalNumber: 'Le code postal est requis',
      birthDate: 'La date de naissance est requise',
      termsAccepted: 'Vous devez accepter les conditions générales'
    };
    return messages[fieldName] || `Ce champ est requis`;
  }

  nextStep() {
    if (this.basicForm.valid) {
      console.log('🔍 Moving to step 2');
      this.currentStep = 2;
      this.errorMessage = '';
      this.fieldErrors = {};
      
      setTimeout(() => {
        if (window.grecaptcha) {
          this.initializeRecaptcha();
        } else {
          console.log('⏳ Waiting for reCAPTCHA to load...');
          const checkInterval = setInterval(() => {
            if (window.grecaptcha) {
              clearInterval(checkInterval);
              this.initializeRecaptcha();
            }
          }, 100);
        }
      }, 100);
    } else {
      this.markFormGroupTouched(this.basicForm);
      this.errorMessage = 'Veuillez corriger les erreurs ci-dessus avant de continuer';
    }
  }

  previousStep() {
    this.currentStep = 1;
    this.errorMessage = '';
    this.fieldErrors = {};
    
    if (this.recaptchaWidgetId !== null) {
      try {
        window.grecaptcha.reset(this.recaptchaWidgetId);
      } catch (error) {
        console.error('Error resetting reCAPTCHA:', error);
      }
    }
    this.recaptchaInitialized = false;
    this.recaptchaWidgetId = null;
    this.recaptchaToken = null;
  }

  private markFormGroupTouched(formGroup: FormGroup) {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
      control?.markAsDirty();
    });
  }

  async onSubmit() {
    console.log('🔍 Form submission started');
    if (this.currentStep === 1) {
      this.nextStep();
      return;
    }

    this.markFormGroupTouched(this.customForm);

    if (this.customForm.invalid) {
      console.log('❌ Form is invalid');
      this.errorMessage = 'Veuillez corriger les erreurs ci-dessus';
      return;
    }

    if (!this.recaptchaToken) {
      console.error('❌ No reCAPTCHA token available');
      this.setFieldError('recaptcha', 'Veuillez compléter la vérification reCAPTCHA');
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.fieldErrors = {};

    try {
      const formData = {
        ...this.customForm.value,
        ...this.basicForm.value,
        recaptchaToken: this.recaptchaToken
      };

      if (formData.birthDate instanceof Date) {
        formData.birthDate = this.formatDateForInput(formData.birthDate);
      }

      console.log('🚀 Submitting registration with data:', {
        ...formData,
        password: '***',
        recaptchaToken: 'TOKEN_PROVIDED'
      });

      const response = await this.authService.registerUser(formData).toPromise();
      console.log('✅ Registration successful:', response);
      this.snackBar.open('Inscription réussie ! Vous pouvez maintenant vous connecter.', 'Fermer', { duration: 5000, panelClass: 'snackbar-success' });
      this.router.navigate(['/login']);
    } catch (error: any) {
      console.error('❌ Registration failed:', error);
      const msg = (error.message || '').toLowerCase();
    
      // 🔎 Détails dynamiques
      if (msg.includes('user already exists')) {
        const friendlyMsg = '❌ Ce nom d\'utilisateur ou cette adresse email est déjà associé à un compte. Veuillez en choisir un autre ou vous connecter à votre compte.';
        this.snackBar.openFromComponent(SnackbarErrorComponent, {
          data: friendlyMsg,
          duration: 7000,
          horizontalPosition: 'center',
          verticalPosition: 'top',
          panelClass: 'snackbar-error'
        });
        if (msg.includes('email')) {
          this.setFieldError('email', '');
        } else if (msg.includes('username')) {
          this.setFieldError('username', '');
        } else {
          this.errorMessage = '';
        }
      } else if (msg.includes('postal') || msg.includes('code postal')) {
        this.setFieldError('postalNumber', error.message);
        this.snackBar.openFromComponent(SnackbarErrorComponent, {
          data: error.message,
          duration: 7000,
          horizontalPosition: 'center',
          verticalPosition: 'top',
          panelClass: 'snackbar-error'
        });
      } else if (msg.includes('phone') || msg.includes('téléphone')) {
        this.setFieldError('phone', error.message);
        this.snackBar.openFromComponent(SnackbarErrorComponent, {
          data: error.message,
          duration: 7000,
          horizontalPosition: 'center',
          verticalPosition: 'top',
          panelClass: 'snackbar-error'
        });
      } else if (msg.includes('recaptcha')) {
        this.setFieldError('recaptcha', error.message);
        this.snackBar.openFromComponent(SnackbarErrorComponent, {
          data: error.message,
          duration: 7000,
          horizontalPosition: 'center',
          verticalPosition: 'top',
          panelClass: 'snackbar-error'
        });
      } else {
        this.snackBar.openFromComponent(SnackbarErrorComponent, {
          data: error.message || 'Une erreur s\'est produite lors de l\'inscription. Veuillez réessayer.',
          duration: 7000,
          horizontalPosition: 'center',
          verticalPosition: 'top',
          panelClass: 'snackbar-error'
        });
      }
    
      // ♻️ Recharger le captcha
      if (this.recaptchaWidgetId !== null) {
        try {
          window.grecaptcha.reset(this.recaptchaWidgetId);
          this.recaptchaToken = null;
        } catch (recaptchaError) {
          console.error('Error resetting reCAPTCHA:', recaptchaError);
        }
      }
      this.isSubmitting = false;
    }
    
  }

  // ✅ Méthode pour formater la date pour l'input date
  formatDateForInput(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  // ✅ Calculer l'âge minimum et maximum pour le sélecteur de date
  getMinDate(): string {
    const today = new Date();
    const minDate = new Date(today.getFullYear() - 120, today.getMonth(), today.getDate());
    return this.formatDateForInput(minDate);
  }

  getMaxDate(): string {
    const today = new Date();
    const maxDate = new Date(today.getFullYear() - 13, today.getMonth(), today.getDate());
    return this.formatDateForInput(maxDate);
  }

  onStep2Input() {
    if (!this.recaptchaInitialized && this.currentStep === 2) {
      console.log('🔍 Initializing reCAPTCHA on first input');
      setTimeout(() => this.initializeRecaptcha(), 200);
    }
  }

  closeModal() {
    this.showModal = false;
  }
}
