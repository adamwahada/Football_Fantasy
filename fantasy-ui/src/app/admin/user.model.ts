export interface UserEntity {
  id: number;
  keycloakId?: string;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phone?: string;
  country?: string;
  address?: string;
  postalNumber?: string;
  birthDate?: string;  // Use string because backend returns JSON date
  referralCode?: string;
  termsAccepted?: boolean;
  active: boolean;
  bannedUntil?: string; // Use string here, can parse to Date if needed
  balance: number;
}
