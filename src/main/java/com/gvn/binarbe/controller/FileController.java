package com.gvn.binarbe.controller;

import com.gvn.binarbe.entity.UserProfile;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.repository.UserProfileRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Slf4j
public class FileController {

  private final UserProfileRepository userProfileRepository;

  @GetMapping("/{filename:.+}")
  public ResponseEntity<Resource> getFile(
      @PathVariable String filename, Authentication authentication) {

    log.info("Requesting file: {}", filename);
    log.info("User: {}", authentication.getName());
    log.info("Authorities: {}", authentication.getAuthorities());

    // 1. Check if user is staff
    boolean isStaff =
        authentication.getAuthorities().stream()
            .anyMatch(
                a ->
                    a.getAuthority().equals("ROLE_" + RoleName.MARKETING.name())
                        || a.getAuthority().equals("ROLE_" + RoleName.BRANCH_MANAGER.name())
                        || a.getAuthority().equals("ROLE_" + RoleName.BACKOFFICE.name())
                        || a.getAuthority().equals("ROLE_SUPERADMIN"));

    // 2. If not staff, check ownership via UserProfile
    boolean isOwner = false;
    Optional<UserProfile> profile =
        userProfileRepository.findByKtpPathOrKkPathOrNpwpPath(filename, filename, filename);

    if (profile.isPresent()) {
      if (profile.get().getUser().getEmail().equals(authentication.getName())) {
        isOwner = true;
      }
    }

    log.info(
        "File access check - User: {}, Filename: {}, IsStaff: {}, IsOwner: {}",
        authentication.getName(),
        filename,
        isStaff,
        isOwner);

    if (isStaff || isOwner) {
      log.info("Access granted.");
      return serveFile(filename);
    } else {
      log.warn("Access denied for user {} to file {}", authentication.getName(), filename);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // Old UserDocument ownership check removed as per instruction

  }

  private ResponseEntity<Resource> serveFile(String filename) {
    try {
      Path path = Paths.get("uploads").resolve(filename);
      Resource resource = new FileSystemResource(path);

      if (resource.exists() && resource.isReadable()) {
        String contentType = Files.probeContentType(path);
        if (contentType == null) {
          contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }
}
