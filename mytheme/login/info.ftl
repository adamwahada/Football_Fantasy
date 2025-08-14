<!DOCTYPE html>
<html>
<head>
  <title>Information - ${realm.displayName!realm.name}</title>
  <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />
</head>
<body>
  <div class="container">
    <div class="login-box">
      <h2>Information</h2>
      
      <div class="email-verify-content">
        <#if message?has_content>
          <#-- Check if email is already verified or was just verified -->
          <#if message.summary?contains("already") || message.summary?contains("déjà")>
            <p class="info-message">
              ✅ Votre email est déjà vérifié !
            </p>
            <p class="instruction">
              Votre adresse email a été confirmée précédemment. Vous pouvez vous connecter à votre compte.
            </p>
          <#elseif message.summary?contains("verified") || message.summary?contains("vérifié")>
            <p class="success-message">
              ✅ Email vérifié avec succès !
            </p>
            <p class="instruction">
              Votre adresse email a été confirmée. Vous pouvez maintenant accéder à votre compte.
            </p>
          <#else>
            <p class="info-message">
              ℹ️ ${message.summary}
            </p>
          </#if>
          
          <div id="snackbar" class="snackbar show">
            <button class="snackbar-close" onclick="document.getElementById('snackbar').classList.remove('show')">×</button>
            <span class="snackbar-message">${message.summary}</span>
          </div>
        <#else>
          <p class="success-message">
            ✅ Opération terminée !
          </p>
        </#if>
        
  
      </div>
    </div>
  </div>
  
  <script>
    window.onload = function () {
      const snackbar = document.getElementById('snackbar');
      if (snackbar && snackbar.classList.contains('show')) {
        setTimeout(() => {
          snackbar.classList.remove('show');
        }, 4000);
      }
    };
  </script>
</body>
</html>