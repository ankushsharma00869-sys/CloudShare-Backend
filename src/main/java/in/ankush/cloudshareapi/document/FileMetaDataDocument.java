package in.ankush.cloudshareapi.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "files")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FileMetaDataDocument {

    @Id
    private String id;
    private  String name;
    private  String type;
    private Long size;
    private String userId;
    private  Boolean isPublic;
    private  String fileLocation;
    private LocalDateTime uploadedAt;
    private String cloudinaryPublicId;
    private String cloudinaryResourceType; // "image" | "video" | "raw" - Cloudinary's destroy() needs the exact type, unlike upload() which accepts "auto"

    // AI Features: Smart Search + Auto Summarization
    private String extractedText;
    private List<Double> embedding;
    private String summary;
    private String aiStatus;
}
