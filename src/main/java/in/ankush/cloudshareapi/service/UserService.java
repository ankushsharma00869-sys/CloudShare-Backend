package in.ankush.cloudshareapi.service;

import in.ankush.cloudshareapi.document.User;
import in.ankush.cloudshareapi.dto.UpdateProfileRequest;
import in.ankush.cloudshareapi.dto.UserProfileDTO;
import in.ankush.cloudshareapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    /**
     * Returns the currently authenticated user's full document.
     * Every other service (FileMetaDataService, UserCreditsService, PaymentService...)
     * calls this exactly the way it used to call ProfileService.getCurrentProfile().
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("User not authenticated");
        }
        // The JWT filter sets the principal's name to the Mongo user id (see UserPrincipal).
        String userId = authentication.getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserProfileDTO getCurrentUserProfile() {
        return toDto(getCurrentUser());
    }

    /** Updates first name / last name / email for the logged-in user. Any field left null is left unchanged. */
    public UserProfileDTO updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (StringUtils.hasText(request.getEmail())) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new DuplicateKeyException("An account with this email already exists");
            }
            user.setEmail(newEmail);
        }

        return toDto(userRepository.save(user));
    }

    /** Uploads a new profile photo to Cloudinary and saves its URL on the user. */
    public UserProfileDTO updatePhoto(MultipartFile photo) throws IOException {
        if (photo == null || photo.isEmpty()) {
            throw new IllegalArgumentException("No photo file provided");
        }
        User user = getCurrentUser();
        String photoUrl = cloudinaryService.uploadImage(photo);
        user.setPhotoUrl(photoUrl);
        return toDto(userRepository.save(user));
    }

    public UserProfileDTO toDto(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .photoUrl(user.getPhotoUrl())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

