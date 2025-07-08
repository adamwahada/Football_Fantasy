<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <title>Modifier le mot de passe - ${realm.displayName!realm.name}</title>
  <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />
</head>
<body>
  <div class="container">
    <h2>Modifier votre mot de passe</h2>

    <!-- 🔐 Error Handling (SAFE version of errors.join) -->
    <#if errors?? && errors?has_content>
      <#-- On force errors en liste si ce n'est pas déjà une séquence -->
      <#assign errorsList = errors?is_sequence?then(errors, [errors])>
      <div id="snackbar-error" class="snackbar show error">
        <button class="snackbar-close" onclick="document.getElementById('snackbar-error').classList.remove('show')">×</button>
        <span class="snackbar-message">
          ${errorsList?join(", ")}
        </span>
      </div>
    </#if>

    <!-- Snackbar for message object -->
    <#if message?has_content && (message.type == 'success' || message.type == 'error')>
      <div id="snackbar-message" class="snackbar show ${message.type}">
        <button class="snackbar-close" onclick="document.getElementById('snackbar-message').classList.remove('show')">×</button>
        <span class="snackbar-message">
          <#if message.type == 'success'>
            Mot de passe mis à jour avec succès.
          <#elseif message.type == 'error'>
            ${kcSanitize(message.summary)?no_esc}
          </#if>
        </span>
      </div>
    </#if>
    
    <form id="kc-update-password-form" action="${url.loginAction}" method="post">
      <input type="hidden" name="execution" value="${execution}" />
      <#if tabId??>
        <input type="hidden" name="tab_id" value="${tabId}" />
      </#if>
      <#if client??>
        <input type="hidden" name="client_id" value="${client.clientId}" />
      </#if>
      
      <!-- New password -->
      <div class="form-group password-group">
        <label for="password-new">Nouveau mot de passe</label>
        <div class="password-wrapper">
          <input type="password" id="password-new" name="password-new" required autocomplete="new-password" />
          <button type="button" class="toggle-password" onclick="togglePasswordVisibility('password-new', this)">👁️</button>
        </div>
        <div class="password-requirements">
          <small>Le mot de passe doit contenir :</small>
          <ul class="requirements-list">
            <li id="req-length">Au moins 8 caractères</li>
            <li id="req-uppercase">Une lettre majuscule</li>
            <li id="req-lowercase">Une lettre minuscule</li>
            <li id="req-number">Un chiffre</li>
          </ul>
        </div>
      </div>
      
      <!-- Confirm password -->
      <div class="form-group password-group">
        <label for="password-confirm">Confirmer le mot de passe</label>
        <div class="password-wrapper">
          <input type="password" id="password-confirm" name="password-confirm" required autocomplete="new-password" />
          <button type="button" class="toggle-password" onclick="togglePasswordVisibility('password-confirm', this)">👁️</button>
        </div>
        <div id="password-match-indicator" class="password-match-indicator"></div>
      </div>
      
      <button type="submit" name="submit" id="kc-submit">Changer le mot de passe</button>
    </form>
    
    <div class="links">
      <a href="${url.loginUrl}">Retour à la connexion</a>
    </div>
  </div>
  
  <script>
    function togglePasswordVisibility(id, button) {
      const input = document.getElementById(id);
      if (input.type === "password") {
        input.type = "text";
        button.textContent = "🙈";
      } else {
        input.type = "password";
        button.textContent = "👁️";
      }
    }

    function updatePasswordRequirements(password) {
      const lengthReq = document.getElementById('req-length');
      const uppercaseReq = document.getElementById('req-uppercase');
      const lowercaseReq = document.getElementById('req-lowercase');
      const numberReq = document.getElementById('req-number');

      lengthReq.className = password.length >= 8 ? 'valid' : 'invalid';
      uppercaseReq.className = /[A-Z]/.test(password) ? 'valid' : 'invalid';
      lowercaseReq.className = /[a-z]/.test(password) ? 'valid' : 'invalid';
      numberReq.className = /[0-9]/.test(password) ? 'valid' : 'invalid';
    }

    function checkPasswordMatch() {
      const password = document.getElementById('password-new').value;
      const confirmPassword = document.getElementById('password-confirm').value;
      const indicator = document.getElementById('password-match-indicator');

      if (confirmPassword.length > 0) {
        if (password === confirmPassword) {
          indicator.textContent = '✓ Les mots de passe correspondent';
          indicator.className = 'password-match-indicator match';
        } else {
          indicator.textContent = '✗ Les mots de passe ne correspondent pas';
          indicator.className = 'password-match-indicator no-match';
        }
      } else {
        indicator.textContent = '';
        indicator.className = 'password-match-indicator';
      }
    }

    window.onload = function () {
      // Auto hide error snackbar if visible
      const snackbarError = document.getElementById('snackbar-error');
      if (snackbarError && snackbarError.classList.contains('show')) {
        setTimeout(() => {
          snackbarError.classList.remove('show');
        }, 4000);
      }
      // Auto hide message snackbar if visible
      const snackbarMessage = document.getElementById('snackbar-message');
      if (snackbarMessage && snackbarMessage.classList.contains('show')) {
        setTimeout(() => {
          snackbarMessage.classList.remove('show');
        }, 4000);
      }

      const passwordInput = document.getElementById('password-new');
      const confirmPasswordInput = document.getElementById('password-confirm');

      passwordInput.addEventListener('input', function () {
        updatePasswordRequirements(this.value);
        checkPasswordMatch();
      });

      confirmPasswordInput.addEventListener('input', function () {
        checkPasswordMatch();
      });
    };
  </script>
</body>
</html>
