<!DOCTYPE html>
<html>
<head>
  <title>Login - ${realm.displayName!realm.name}</title>
  <#-- Add your custom styles -->
  <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />

</head>
<body>
  <div class="container">
    <div class="login-box">
      <h2>Bienvenue sur ${realm.displayName!realm.name}</h2>
      
      <#-- Display messages (errors, info, etc.) -->
      <#if message?has_content>
        <div class="alert alert-${message.type}">
          ${kcSanitize(message.summary)?no_esc}
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
        
        <div class="form-group">
          <label for="password">Mot de passe</label>
          <input id="password" name="password" type="password" 
                 autocomplete="current-password" />
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
</body>
</html>