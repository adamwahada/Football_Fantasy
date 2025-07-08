<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Modifier le mot de passe - ${realmName!"Keycloak"}</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />
</head>
<body>
    <div class="container">
        <h2>Modifier le mot de passe</h2>
        <#if message?has_content>
            <div class="alert alert-${message.type}">
                <span class="kc-feedback-text">${message.summary}</span>
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
            
            <div class="form-group">
                <label for="password-new">Nouveau mot de passe</label>
                <input type="password" id="password-new" name="password-new" required autocomplete="new-password" />
            </div>
            
            <div class="form-group">
                <label for="password-confirm">Confirmer le nouveau mot de passe</label>
                <input type="password" id="password-confirm" name="password-confirm" required autocomplete="new-password" />
            </div>
            
            <button type="submit" name="submit" id="kc-submit">Changer le mot de passe</button>
        </form>
        
        <div class="links">
            <a href="${url.loginUrl}">Retour à la connexion</a>
        </div>
    </div>
</body>
</html>