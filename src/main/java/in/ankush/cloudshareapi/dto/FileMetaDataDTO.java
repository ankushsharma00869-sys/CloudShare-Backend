package in.ankush.cloudshareapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileMetaDataDTO {
    private String id;
    private  String name;
    private  String type;
    private Long size;
    private String userId;
    private  Boolean isPublic;
    private  String fileLocation;
    private LocalDateTime uploadedAt;
    private String summary;
    private String aiStatus;
}
