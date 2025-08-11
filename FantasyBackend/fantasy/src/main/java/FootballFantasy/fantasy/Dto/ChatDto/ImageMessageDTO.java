package FootballFantasy.fantasy.Dto.ChatDto;

import org.springframework.web.multipart.MultipartFile;

public class ImageMessageDTO {
    private String roomId;
    private MultipartFile image;
    private String content; // Texte optionnel
    private Long replyToId; // Réponse à un message

    // Validation intégrée
    public void validate() {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("L'image est obligatoire");
        }
        if (!image.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Seules les images sont acceptées");
        }
    }
}
