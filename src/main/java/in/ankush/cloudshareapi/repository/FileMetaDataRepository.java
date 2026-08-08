package in.ankush.cloudshareapi.repository;

import in.ankush.cloudshareapi.document.FileMetaDataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FileMetaDataRepository extends MongoRepository<FileMetaDataDocument, String>{

    List<FileMetaDataDocument> findByUserId(String userId);

    Long countByUserId(String userId);
}
