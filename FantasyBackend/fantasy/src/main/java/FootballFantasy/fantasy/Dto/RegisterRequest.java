package FootballFantasy.fantasy.Dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Le nom d'utilisateur est requis")
    @Size(min = 3, max = 30, message = "Le nom d'utilisateur doit contenir entre 3 et 30 caractères")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores")
    private String username;

    @Email(message = "Format d'email invalide")
    @NotBlank(message = "L'email est requis")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @NotBlank(message = "Le prénom est requis")
    @Size(max = 50, message = "Le prénom ne peut pas dépasser 50 caractères")
    private String firstName;

    @NotBlank(message = "Le nom de famille est requis")
    @Size(max = 50, message = "Le nom de famille ne peut pas dépasser 50 caractères")
    private String lastName;

    // Optional fields
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]*$", message = "Format de téléphone invalide")
    private String phone;

    @Size(max = 50, message = "Le pays ne peut pas dépasser 50 caractères")
    private String country;

    @Size(max = 100, message = "L'adresse ne peut pas dépasser 100 caractères")
    private String address;

    @Size(max = 10, message = "Le code postal ne peut pas dépasser 10 caractères")
    private String postalNumber;

    // Date should be in YYYY-MM-DD format
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "La date de naissance doit être au format YYYY-MM-DD")
    private String birthDate;

    @Size(max = 20, message = "Le code de parrainage ne peut pas dépasser 20 caractères")
    private String referralCode;

    @NotNull(message = "L'acceptation des conditions est requise")
    private Boolean termsAccepted;

    private String recaptchaToken;

    public boolean isTermsAccepted() {
        return termsAccepted;
    }

    // Additional validation method for birthDate
    public boolean isValidAge() {
        if (birthDate == null || birthDate.trim().isEmpty()) {
            return true; // Optional field
        }

        try {
            java.time.LocalDate birth = java.time.LocalDate.parse(birthDate);
            java.time.LocalDate now = java.time.LocalDate.now();
            int age = java.time.Period.between(birth, now).getYears();
            return age >= 13 && age <= 120; // Reasonable age limits
        } catch (Exception e) {
            return false;
        }
    }
}