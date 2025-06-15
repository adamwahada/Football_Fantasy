package FootballFantasy.fantasy.Service.ServiceUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    public boolean verifyToken(String token) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("secret", secretKey);
        params.add("response", token);

        ResponseEntity<RecaptchaResponse> response = restTemplate.postForEntity(VERIFY_URL, params, RecaptchaResponse.class);

        return response.getBody() != null && response.getBody().isSuccess();
    }

    public static class RecaptchaResponse {
        private boolean success;
        private List<String> errorCodes;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public List<String> getErrorCodes() { return errorCodes; }
        public void setErrorCodes(List<String> errorCodes) { this.errorCodes = errorCodes; }
    }
}

