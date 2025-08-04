package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Services.ChatService.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "Upload file for chat")
    @PostMapping("/upload/{roomId}")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable String roomId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            if (!fileUploadService.isValidFileType(file.getContentType())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid file type"));
            }

            String fileUrl = fileUploadService.uploadFile(file, roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("fileUrl", fileUrl);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("mimeType", file.getContentType());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to upload file"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}