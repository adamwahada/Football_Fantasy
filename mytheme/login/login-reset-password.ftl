<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Mot de passe oublié - ${realm.displayName!realm.name}</title>
    <link rel="stylesheet" href="${url.resourcesPath}/css/styles.css" />
</head>
    <style>
        /* Modern button styles */
        .btn {
            display: inline-block;
            padding: 12px 24px;
            font-size: 16px;
            font-weight: 500;
            text-align: center;
            text-decoration: none;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s ease;
            margin: 8px 4px;
            min-width: 140px;
        }
        
        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
        }
        
        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
        }
        
        .btn-secondary {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
            color: white;
            box-shadow: 0 4px 15px rgba(245, 87, 108, 0.4);
        }
        
        .btn-secondary:hover:not(:disabled) {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(245, 87, 108, 0.6);
        }
        
        .btn-secondary:disabled {
            background: #cccccc;
            cursor: not-allowed;
            transform: none;
            box-shadow: none;
        }
        
        .btn:active {
            transform: translateY(0);
        }
        
        /* Submit button styling */
        #kc-submit {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
            padding: 12px 24px;
            font-size: 16px;
            font-weight: 500;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s ease;
            width: 100%;
        }
        
        #kc-submit:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
        }
        
        #kc-submit:active {
            transform: translateY(0);
        }
        
        /* Countdown text styling */
        .countdown-text {
            font-size: 14px;
            color: #666;
            margin-top: 8px;
        }
    </style>
<body>
    <div class="container">
        <h2>Mot de passe oublié</h2>
        
        <!-- Success message container (hidden by default) -->
        <div id="success-container" class="success-container" style="display: none;">
            <h3>Email envoyé!</h3>
            <p>Un email de réinitialisation de mot de passe a été envoyé à <strong id="email-address"></strong>.</p>
            <p>Veuillez vérifier votre boîte de réception et cliquer sur le lien pour continuer.</p>
            <p class="note">Le lien expire dans 5 minutes.</p>
            <div class="success-actions">
                <a href="${url.loginUrl}" class="btn btn-primary">Retour à la connexion</a>
            </div>
        </div>
        
        <!-- Form container -->
        <div id="form-container">
            <!-- Error message -->
            <#if message?has_content && message.type == 'error'>
                <div id="snackbar" class="snackbar show error">
                    <button class="snackbar-close" onclick="document.getElementById('snackbar').classList.remove('show')">×</button>
                    <span class="snackbar-message">
                        <#if message.summary?contains("User not found") || message.summary?contains("utilisateur introuvable")>
                            Aucun compte n'est associé à cette adresse email.
                        <#else>
                            ${kcSanitize(message.summary)?no_esc}
                        </#if>
                    </span>
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
    </div>
    
    <script>
        function showSuccess(email) {
            document.getElementById('form-container').style.display = 'none';
            document.getElementById('success-container').style.display = 'block';
            document.getElementById('email-address').textContent = email;
        }
        
        function showForm() {
            document.getElementById('form-container').style.display = 'block';
            document.getElementById('success-container').style.display = 'none';
        }
        
        window.onload = function () {
            const snackbar = document.getElementById('snackbar');
            if (snackbar && snackbar.classList.contains('show')) {
                setTimeout(() => {
                    snackbar.classList.remove('show');
                }, 6000);
            }
            
            // Check if we should show success message
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('success') === 'true') {
                const email = urlParams.get('email') || 'votre adresse email';
                showSuccess(email);
            }
            
            // Handle form submission
document.getElementById('kc-reset-password-form').addEventListener('submit', function(e) {
    e.preventDefault(); // stop normal form submission

    const email = document.getElementById('username').value;
    const form = e.target;
    const formData = new FormData(form);

    fetch(form.action, {
        method: 'POST',
        body: formData,
        credentials: 'include'
    })
    .then(response => {
        if (response.redirected || response.ok) {
            showSuccess(email);
        } else {
            // Optional: show error
            alert("Une erreur est survenue.");
        }
    })
    .catch(() => {
        alert("Erreur lors de la soumission du formulaire.");
    });
});
        };
    </script>
</body>
</html> 