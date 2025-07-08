<#-- error.ftl minimal pour Keycloak -->

<h1>Erreur</h1>
<p>Une erreur est survenue lors du traitement de la requête.</p>

<#if errorMessage??>
  <p><b>Détail:</b> ${kcSanitize(errorMessage)}</p>
</#if>

<p><a href="${url.loginUrl}">Retour à la page de connexion</a></p>
