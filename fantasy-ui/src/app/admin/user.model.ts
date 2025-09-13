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
  birthDate?: string;  
  referralCode?: string;
  termsAccepted?: boolean;
  active: boolean;
  bannedUntil?: string; 
  balance: number;
}
