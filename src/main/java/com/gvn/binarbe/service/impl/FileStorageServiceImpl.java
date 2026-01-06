package com.gvn.binarbe.service.impl;

import com.gvn.binarbe.exception.BusinessException;
import com.gvn.binarbe.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

  private final Path fileStorageLocation;

  public FileStorageServiceImpl(@Value("${app.file-storage.upload-dir}") String uploadDir) {
    this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

    try {
      Files.createDirectories(this.fileStorageLocation);
    } catch (Exception ex) {
      throw new BusinessException(
          "Could not create the directory where the uploaded files will be stored.", ex);
    }
  }

  @Override
  public String storeFile(MultipartFile file) {
    // Normalize file name
    String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

    try {
      // Check if the file's name contains invalid characters
      if (originalFileName.contains("..")) {
        throw new BusinessException(
            "Sorry! Filename contains invalid path sequence " + originalFileName);
      }

      // Generate a unique file name to avoid collisions
      String fileExtension = "";
      int dotIndex = originalFileName.lastIndexOf('.');
      if (dotIndex >= 0) {
        fileExtension = originalFileName.substring(dotIndex);
      }

      String fileName = UUID.randomUUID().toString() + fileExtension;

      // Copy file to the target location (Replacing existing file with the same name)
      Path targetLocation = this.fileStorageLocation.resolve(fileName);
      Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

      return fileName;
    } catch (IOException ex) {
      throw new BusinessException(
          "Could not store file " + originalFileName + ". Please try again!", ex);
    }
  }
}
