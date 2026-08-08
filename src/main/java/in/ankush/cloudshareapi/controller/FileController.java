package in.ankush.cloudshareapi.controller;

import in.ankush.cloudshareapi.dto.FileMetaDataDTO;
import in.ankush.cloudshareapi.service.FileMetaDataService;
import in.ankush.cloudshareapi.service.UserCreditsService;
import in.ankush.cloudshareapi.document.UserCredits;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final FileMetaDataService fileMetaDataService;
    private final UserCreditsService userCreditsService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestPart("files") MultipartFile[] files) throws IOException {
        Map<String, Object> response = new HashMap<>();
        List<FileMetaDataDTO> list = fileMetaDataService.uploadFiles(files);
        UserCredits finalCredits = userCreditsService.getUserCredits();

        response.put("files", list);
        response.put("remainingCredits", finalCredits.getCredits());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getFilesForCurrentUser() {
        List<FileMetaDataDTO> files = fileMetaDataService.getFiles();
        return ResponseEntity.ok(files);
    }

    // 🤖 AI Feature: Smart File Search (semantic search) — e.g. GET /files/search?q=invoice june
    @GetMapping("/search")
    public ResponseEntity<?> searchFiles(@RequestParam("q") String query) {
        List<FileMetaDataDTO> results = fileMetaDataService.searchFiles(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<?> getPublicFile(@PathVariable String id) {
        FileMetaDataDTO file = fileMetaDataService.getPublicFile(id);
        return ResponseEntity.ok(file);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Void> download(@PathVariable String id) throws IOException {
        FileMetaDataDTO file = fileMetaDataService.getDownloadableFile(id);

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, file.getFileLocation())
                .build();
    }

    // ✅ FIX: Cloudinary URL pe redirect karo (local disk nahi hai ab)
    @GetMapping("/download/public/{id}")
    public ResponseEntity<Void> downloadPublic(@PathVariable String id) throws IOException {
        FileMetaDataDTO file = fileMetaDataService.getPublicFile(id);

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, file.getFileLocation())
                .build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable String id) {
        fileMetaDataService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-public")
    public ResponseEntity<?> togglePublic(@PathVariable String id) {
        FileMetaDataDTO file = fileMetaDataService.togglePublic(id);
        return ResponseEntity.ok(file);
    }

    // ✅ FIX: Cloudinary URL pe redirect karo (inline view ke liye)
    @GetMapping("/view/{id}")
    public ResponseEntity<Void> viewFile(@PathVariable String id) throws IOException {
        FileMetaDataDTO file = fileMetaDataService.getPublicFile(id);

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, file.getFileLocation())
                .build();
    }
}
