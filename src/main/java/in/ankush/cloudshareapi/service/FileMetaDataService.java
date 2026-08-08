package in.ankush.cloudshareapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import in.ankush.cloudshareapi.document.FileMetaDataDocument;
import in.ankush.cloudshareapi.document.User;
import in.ankush.cloudshareapi.dto.FileMetaDataDTO;
import in.ankush.cloudshareapi.repository.FileMetaDataRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileMetaDataService {

    private final UserService userService;
    private final UserCreditsService userCreditsService;
    private final FileMetaDataRepository fileMetaDataRepository;
    private final TextExtractorService textExtractorService; // 🤖 AI: extract text
    private final GeminiService geminiService;                // 🤖 AI: embeddings + summary
    private final MongoTemplate mongoTemplate;                 // 🤖 AI: $vectorSearch aggregation

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary getCloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public List<FileMetaDataDTO> uploadFiles(MultipartFile[] files) throws IOException {
        User currentUser = userService.getCurrentUser();

        if (!userCreditsService.hashEnoughCredits(files.length)) {
            throw new RuntimeException("Not enough credits to upload files. Please purchase more credits");
        }

        List<FileMetaDataDocument> savedFiles = new ArrayList<>();
        Cloudinary cloudinary = getCloudinary();

        for (MultipartFile file : files) {
            // ✅ Cloudinary pe upload karo
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "auto",
                            "public_id", UUID.randomUUID().toString()
                    )
            );

            String fileUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String resourceType = (String) uploadResult.get("resource_type"); // Cloudinary resolves "auto" to image/video/raw here

            // 🤖 AI Feature: try to pull text out of the file (PDF/DOCX/TXT only)
            String extractedText = textExtractorService.extractText(file);

            FileMetaDataDocument fileMetaData = FileMetaDataDocument.builder()
                    .fileLocation(fileUrl)
                    .cloudinaryPublicId(publicId)
                    .cloudinaryResourceType(resourceType)
                    .name(file.getOriginalFilename())
                    .size(file.getSize())
                    .type(file.getContentType())
                    .userId(currentUser.getId())
                    .isPublic(false)
                    .uploadedAt(LocalDateTime.now())
                    .extractedText(extractedText)
                    .aiStatus(extractedText == null ? "UNSUPPORTED" : "PENDING")
                    .build();

            userCreditsService.consumeCredit();
            FileMetaDataDocument saved = fileMetaDataRepository.save(fileMetaData);
            savedFiles.add(saved);

            // 🤖 AI Feature: generate embedding + summary in the background so
            // upload response doesn't wait on AI calls (better UX, no timeout risk).
            if (extractedText != null) {
                processAiPipelineAsync(saved.getId(), extractedText);
            }
        }

        return savedFiles.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * 🤖 AI Feature: runs embedding + summarization off the request thread,
     * then writes the results back onto the same document once ready.
     */
    @Async
    public void processAiPipelineAsync(String fileId, String extractedText) {
        try {
            List<Double> embedding = geminiService.embed(extractedText, "RETRIEVAL_DOCUMENT");
            String summary = geminiService.summarize(extractedText);

            FileMetaDataDocument doc = fileMetaDataRepository.findById(fileId).orElse(null);
            if (doc == null) return;

            doc.setEmbedding(embedding);
            doc.setSummary(summary);
            doc.setAiStatus((embedding != null || summary != null) ? "DONE" : "FAILED");
            fileMetaDataRepository.save(doc);
        } catch (Exception e) {
            System.err.println("AI pipeline failed for file " + fileId + ": " + e.getMessage());
            fileMetaDataRepository.findById(fileId).ifPresent(doc -> {
                doc.setAiStatus("FAILED");
                fileMetaDataRepository.save(doc);
            });
        }
    }

    private FileMetaDataDTO mapToDTO(FileMetaDataDocument doc) {
        return FileMetaDataDTO.builder()
                .id(doc.getId())
                .fileLocation(doc.getFileLocation())
                .name(doc.getName())
                .size(doc.getSize())
                .type(doc.getType())
                .userId(doc.getUserId())
                .isPublic(doc.getIsPublic())
                .uploadedAt(doc.getUploadedAt())
                .summary(doc.getSummary())
                .aiStatus(doc.getAiStatus())
                .build();
    }

    public List<FileMetaDataDTO> getFiles() {
        User currentUser = userService.getCurrentUser();
        List<FileMetaDataDocument> files =
                fileMetaDataRepository.findByUserId(currentUser.getId());
        return files.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public FileMetaDataDTO getPublicFile(String id) {
        Optional<FileMetaDataDocument> fileOptional = fileMetaDataRepository.findById(id);
        if (fileOptional.isEmpty() || !fileOptional.get().getIsPublic()) {
            throw new RuntimeException("Unable to get the file");
        }
        return mapToDTO(fileOptional.get());
    }

    public FileMetaDataDTO getDownloadableFile(String id) {
        FileMetaDataDocument file = fileMetaDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
        return mapToDTO(file);
    }

    public void deleteFile(String id) {
        User currentUser = userService.getCurrentUser();
        FileMetaDataDocument file = fileMetaDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!file.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: file does not belong to current user");
        }

        if (file.getCloudinaryPublicId() != null) {
            // Cloudinary's destroy() rejects resource_type "auto" (only upload() accepts it) -
            // that mismatch was throwing on every delete. Use the type captured at upload time,
            // falling back to "image" for files uploaded before that field existed.
            String resourceType = file.getCloudinaryResourceType() != null
                    ? file.getCloudinaryResourceType()
                    : "image";
            try {
                getCloudinary().uploader().destroy(
                        file.getCloudinaryPublicId(),
                        ObjectUtils.asMap("resource_type", resourceType)
                );
            } catch (Exception cloudinaryError) {
                // Don't let a Cloudinary-side failure (e.g. already deleted there, transient network
                // issue) block removing the file record - log and continue so the user isn't stuck
                // with an undeletable file in their list.
                System.err.println("Cloudinary destroy failed for " + file.getCloudinaryPublicId() + ": " + cloudinaryError.getMessage());
            }
        }

        fileMetaDataRepository.deleteById(id);
    }

    public FileMetaDataDTO togglePublic(String id) {
        FileMetaDataDocument file = fileMetaDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));
        file.setIsPublic(!file.getIsPublic());
        fileMetaDataRepository.save(file);
        return mapToDTO(file);
    }

    /**
     * 🤖 AI Feature: Smart File Search (semantic search).
     * Embeds the user's query with Gemini, then runs MongoDB Atlas $vectorSearch
     * to find the closest matching files by meaning (not just keyword match).
     * Falls back to a simple name-contains search if no vector index/results exist yet.
     */
    public List<FileMetaDataDTO> searchFiles(String query) {
        User currentUser = userService.getCurrentUser();
        List<Double> queryVector = geminiService.embed(query, "RETRIEVAL_QUERY");

        if (queryVector == null) {
            // Gemini unavailable or query empty — fall back to plain name search
            return fallbackNameSearch(currentUser.getId(), query);
        }

        List<Document> pipeline = List.of(
                new Document("$vectorSearch", new Document()
                        .append("index", "file_vector_index")
                        .append("path", "embedding")
                        .append("queryVector", queryVector)
                        .append("numCandidates", 100)
                        .append("limit", 20)
                        .append("filter", new Document("userId", currentUser.getId()))
                ),
                new Document("$addFields", new Document("score", new Document("$meta", "vectorSearchScore")))
        );

        List<FileMetaDataDocument> matches = new ArrayList<>();
        try {
            mongoTemplate.getCollection("files")
                    .aggregate(pipeline, Document.class)
                    .forEach(doc -> matches.add(documentToFileMetaData(doc)));
        } catch (Exception e) {
            // Vector index missing/not ready yet — fall back gracefully
            System.err.println("$vectorSearch failed (index missing?): " + e.getMessage());
            return fallbackNameSearch(currentUser.getId(), query);
        }

        if (matches.isEmpty()) {
            return fallbackNameSearch(currentUser.getId(), query);
        }

        return matches.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private FileMetaDataDocument documentToFileMetaData(Document doc) {
        return mongoTemplate.getConverter().read(FileMetaDataDocument.class, doc);
    }

    private List<FileMetaDataDTO> fallbackNameSearch(String userId, String query) {
        String lowerQuery = query == null ? "" : query.toLowerCase();
        return fileMetaDataRepository.findByUserId(userId).stream()
                .filter(f -> f.getName() != null && f.getName().toLowerCase().contains(lowerQuery))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}
