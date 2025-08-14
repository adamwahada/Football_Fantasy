<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Confirmez votre email - Football Fantasy</title>
</head>
<body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
    <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center;">
        <h1>⚽ Football Fantasy</h1>
        <p>Bienvenue ${user.firstName!""}!</p>
    </div>
    
    <div style="padding: 30px; background: #f9f9f9;">
        <h2>🎉 Bienvenue dans Football Fantasy !</h2>
        
        <p>Votre compte a été créé avec succès ! Pour finaliser votre inscription, confirmez votre adresse email :</p>
        
        <div style="text-align: center; margin: 30px 0;">
            <a href="${link}" style="background: #28a745; color: white; padding: 15px 30px; text-decoration: none; border-radius: 25px; font-weight: bold;">
                ✅ Confirmer mon email
            </a>
        </div>
        
        <p style="color: #666; font-size: 14px;">Ce lien expire dans ${linkExpirationFormatter(linkExpiration)}.</p>
    </div>
</body>
</html>