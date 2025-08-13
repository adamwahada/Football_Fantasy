package FootballFantasy.fantasy.Services.Cloudinary;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class CloudinaryFileService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Upload une image avec des transformations automatiques
     * @param file L'image à uploader
     * @param folder Le dossier de destination
     * @return L'URL de l'image uploadée
     * @throws IOException si l'upload échoue
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (!isImageFile(file)) {
            throw new IllegalArgumentException("Le fichier doit être une image");
        }

        try {
            // Extraire l'extension du fichier original
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String publicId = folder + "/img_" + UUID.randomUUID().toString() + extension;

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "quality", "auto:good",
                    "fetch_format", "auto",
                    "use_filename", false,
                    "unique_filename", false,  // On gère nous-même l'unicité
                    "overwrite", false
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return (String) uploadResult.get("secure_url");

        } catch (IOException e) {
            log.error("Erreur lors de l'upload de l'image: {}", e.getMessage());
            throw new IOException("Échec de l'upload de l'image: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifie si le fichier est une image
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Obtient les informations d'un fichier sur Cloudinary
     * @param publicId L'ID public du fichier
     * @return Les informations du fichier
     */
    public Map getFileInfo(String publicId) {
        try {
            return cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des infos du fichier: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Upload un fichier vers Cloudinary
     * @param file Le fichier à uploader
     * @param folder Le dossier de destination (ex: "chat", "profiles")
     * @return L'URL sécurisée du fichier uploadé
     * @throws IOException si l'upload échoue
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ne peut pas être vide");
        }

        // Validation de la taille (10MB max par exemple)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Le fichier est trop volumineux (max 10MB)");
        }

        try {
            // Extraire l'extension du fichier original
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Générer un nom unique pour le fichier AVEC extension
            String publicId = folder + "/" + UUID.randomUUID().toString() + extension;

            // Configuration de l'upload pour fichiers non-image (PDF, doc, etc.)
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "raw",  // Correct pour PDF et autres fichiers
                    "use_filename", false,
                    "unique_filename", false,  // On gère nous-même l'unicité
                    "overwrite", false
            );

            // Upload vers Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

            // Retourner l'URL sécurisée
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Fichier uploadé avec succès: {}", secureUrl);

            return secureUrl;

        } catch (IOException e) {
            log.error("Erreur lors de l'upload du fichier: {}", e.getMessage());
            throw new IOException("Échec de l'upload du fichier: " + e.getMessage(), e);
        }
    }

    /**
     * Extrait l'ID public depuis une URL Cloudinary - VERSION CORRIGÉE
     * @param cloudinaryUrl L'URL complète du fichier
     * @return L'ID public
     */
    /**
     * Extrait l'ID public depuis une URL Cloudinary - VERSION CORRIGÉE
     * @param cloudinaryUrl L'URL complète du fichier
     * @return L'ID public avec extension
     */
    public String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }

        try {
            String[] parts = cloudinaryUrl.split("/");

            // Trouver l'index de "upload"
            int uploadIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("upload".equals(parts[i])) {
                    uploadIndex = i;
                    break;
                }
            }

            if (uploadIndex == -1 || uploadIndex + 2 >= parts.length) {
                log.error("Format d'URL Cloudinary invalide: {}", cloudinaryUrl);
                return null;
            }

            // Construire le public_id à partir de l'index upload + 2 (skip version)
            StringBuilder publicId = new StringBuilder();
            for (int i = uploadIndex + 2; i < parts.length; i++) {
                if (i > uploadIndex + 2) {
                    publicId.append("/");
                }
                publicId.append(parts[i]);
            }

            // GARDER l'extension pour les fichiers raw
            return publicId.toString();

        } catch (Exception e) {
            log.error("Erreur lors de l'extraction du public_id: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Méthode pour supprimer un fichier avec le bon resource_type
     */
    public boolean deleteFile(String publicId, String resourceType) {
        try {
            Map<String, Object> options = ObjectUtils.asMap("resource_type", resourceType);
            Map result = cloudinary.uploader().destroy(publicId, options);
            String resultStatus = (String) result.get("result");
            return "ok".equals(resultStatus);
        } catch (IOException e) {
            log.error("Erreur lors de la suppression du fichier: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Méthode overloadée pour rétrocompatibilité
     */
    public boolean deleteFile(String publicId) {
        // Essayer d'abord avec raw, puis image si ça échoue
        if (deleteFile(publicId, "raw")) {
            return true;
        }
        return deleteFile(publicId, "image");
    }
}