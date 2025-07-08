<!DOCTYPE html>
<html>
<head>
  <title>Login - ${realm.displayName!realm.name}</title>
  <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />
</head>
<body>
  <div class="container">
    <div class="login-box">
      <h2>Bienvenue sur ${realm.displayName!realm.name}</h2>

      <!-- ✅ Snackbar shown only on error -->
      <#if message?has_content && message.type == 'error'>
        <div id="snackbar" class="snackbar show">
          <button class="snackbar-close" onclick="document.getElementById('snackbar').classList.remove('show')">×</button>
          <span class="snackbar-message">Votre nom d'utilisateur ou mot de passe est incorrect.</span>
        </div>
      </#if>

      <form id="kc-form-login" action="${url.loginAction}" method="post">
        <div class="form-group">
          <label for="username">
            <#if !realm.loginWithEmailAllowed>
              Nom d'utilisateur
            <#elseif !realm.registrationEmailAsUsername>
              Email ou nom d'utilisateur
            <#else>
              Email
            </#if>
          </label>
          <input id="username" name="username" type="text" 
                 value="${(login.username!'')}" 
                 autofocus 
                 autocomplete="username" />
        </div>

        <!-- ✅ Password input with toggle -->
        <div class="form-group password-group">
          <label for="password">Mot de passe</label>
          <div class="password-wrapper">
            <input id="password" name="password" type="password" autocomplete="current-password" />
            <button type="button" class="toggle-password" onclick="togglePasswordVisibility()">👁️</button>
          </div>
        </div>

        <#if realm.rememberMe && !usernameEditDisabled??>
          <div class="checkbox">
            <input type="checkbox" id="rememberMe" name="rememberMe" 
                   <#if login.rememberMe??>checked</#if> />
            <label for="rememberMe">Se souvenir de moi</label>
          </div>
        </#if>

        <button type="submit">Se connecter</button>
      </form>

      <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
        <div class="links">
          <a href="http://localhost:4200/register" target="_blank">Créer un compte</a>
        </div>
      </#if>

      <#if realm.resetPasswordAllowed>
        <div class="links">
          <a href="${url.loginResetCredentialsUrl}">Mot de passe oublié?</a>
        </div>
      </#if>
    </div>
  </div>

  <script>
    function togglePasswordVisibility() {
      const passwordInput = document.getElementById('password');
      const toggleBtn = document.querySelector('.toggle-password');

      if (passwordInput.type === 'password') {
        passwordInput.type = 'text';
        toggleBtn.textContent = '🙈';
      } else {
        passwordInput.type = 'password';
        toggleBtn.textContent = '👁️';
      }
    }

    window.onload = function () {
      const snackbar = document.getElementById('snackbar');
      if (snackbar && snackbar.textContent.trim().length > 0) {
        snackbar.classList.add('show');
        setTimeout(() => {
          snackbar.classList.remove('show');
        }, 4000);
      }
    };
  </script>
</body>
</html>
