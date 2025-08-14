<!DOCTYPE html>
<html>
<head>
  <title>Vérification Email - ${realm.displayName!realm.name}</title>
  <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />
</head>
<body>
  <div class="container">
    <div class="login-box">
      <h2>Vérification de votre email</h2>
      
      <#if message?has_content>
        <div id="snackbar" class="snackbar show">
          <button class="snackbar-close" onclick="document.getElementById('snackbar').classList.remove('show')">×</button>
          <span class="snackbar-message">${message.summary}</span>
        </div>
      </#if>

      <div class="email-verify-content">
        <p class="instruction">
          Un email de vérification a été envoyé à votre adresse email. 
          Veuillez vérifier votre boîte de réception et cliquer sur le lien dans l'email pour activer votre compte.
        </p>
        
        <p class="instruction">
          Vous n'avez pas reçu l'email ? 
          <a href="${url.loginAction}" style="color: #007bff; text-decoration: none;">Cliquez ici</a> 
          pour renvoyer l'email de vérification.
        </p>

        <div class="links">
          <a href="${url.loginUrl}">← Retour à la connexion</a>
        </div>
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