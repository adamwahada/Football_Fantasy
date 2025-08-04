package FootballFantasy.fantasy.Services.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    @Value("${app.file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:10485760}") // 10MB
    private long maxFileSize;

    public String uploadFile(MultipartFile file, String chatRoomId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir, "chat", chatRoomId);
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        // Copy file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path
        return "/uploads/chat/" + chatRoomId + "/" + filename;
    }

    public void deleteFile(String filePath) {
        try {
            Path path = Paths.get(uploadDir).resolve(filePath.substring(1)); // Remove leading /
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Error deleting file: {}", filePath, e);
        }
    }

    public boolean isValidFileType(String mimeType) {
        return mimeType != null && (
                mimeType.startsWith("image/") ||
                        mimeType.startsWith("video/") ||
                        mimeType.startsWith("audio/") ||
                        mimeType.equals("application/pdf") ||
                        mimeType.startsWith("text/") ||
                        mimeType.equals("application/msword") ||
                        mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }
}