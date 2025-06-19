package FootballFantasy.fantasy.Dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    // 🔽 Custom Attributes
    private String phone;
    private String country;
    private String address;
    private String postalNumber;
    private String birthDate;
    private String referralCode;
    private boolean termsAccepted;

//    @NotBlank(message = "reCAPTCHA token is required")
    private String recaptchaToken;

}