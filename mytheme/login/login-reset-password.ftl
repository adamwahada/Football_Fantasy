<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Mot de passe oublié - ${realmName!"Keycloak"}</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />

</head>
<body>
    <div class="container">
        <h2>Mot de passe oublié</h2>
        
        <#if message?has_content>
            <div class="alert alert-${message.type}">
                <span class="kc-feedback-text">${message.summary}</span>
            </div>
        </#if>
        
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
            <div class="form-group">
                <label for="username">
                    <#if !realm.loginWithEmailAllowed>Nom d'utilisateur
                    <#elseif !realm.registrationEmailAsUsername>Nom d'utilisateur ou email
                    <#else>Email</#if>
                </label>
                <input type="text" id="username" name="username" value="${(auth.attemptedUsername!'')}" autofocus required />
            </div>
            
            <button type="submit" name="submit" id="kc-submit">Envoyer le lien de réinitialisation</button>
        </form>
        
        <div class="links">
            <a href="${url.loginUrl}">Retour à la connexion</a>
        </div>
    </div>
</body>
</html>
