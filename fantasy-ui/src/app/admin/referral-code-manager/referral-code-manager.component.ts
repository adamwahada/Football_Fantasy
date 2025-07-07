import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ReferralCodeService } from './referral-code.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-referral-code-manager',
  templateUrl: './referral-code-manager.component.html',
  styleUrls: ['./referral-code-manager.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule]
})
export class ReferralCodeManagerComponent implements OnInit {
  referralForm: FormGroup;
  referralCodes: any[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private fb: FormBuilder,
    private referralService: ReferralCodeService
  ) {
    this.referralForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern('^[A-Za-z0-9]{6,12}$')]],
      expirationDate: [''] // optionnel
    });
  }

  ngOnInit(): void {
    this.loadReferralCodes();
  }

  loadReferralCodes() {
    this.loading = true;
    this.referralService.getAllReferralCodes().subscribe({
      next: (codes) => {
        this.referralCodes = codes;
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors du chargement des codes';
        console.error(err);
        this.loading = false;
      }
    });
  }

  onSubmit() {
    this.errorMessage = '';
    this.successMessage = '';
    if (this.referralForm.invalid) {
      this.errorMessage = 'Veuillez corriger les erreurs dans le formulaire.';
      return;
    }

    const { code, expirationDate } = this.referralForm.value;
    this.referralService.createReferralCode(code, expirationDate || undefined).subscribe({
      next: (res) => {
        this.successMessage = `Code ${code} créé avec succès !`;
        this.referralForm.reset();
        this.loadReferralCodes();
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors de la création du code';
        console.error(err);
      }
    });
  }

  deleteCode(code: string) {
    if (!confirm(`Voulez-vous vraiment supprimer le code ${code} ?`)) return;

    this.referralService.deleteReferralCode(code).subscribe({
      next: () => {
        this.successMessage = `Code ${code} supprimé.`;
        this.loadReferralCodes();
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors de la suppression du code';
        console.error(err);
      }
    });
  }
}
