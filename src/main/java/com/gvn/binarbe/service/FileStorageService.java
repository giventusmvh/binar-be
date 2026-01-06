package com.gvn.binarbe.service;

import org.springframework.web.multipart.MultipartFile;

/** Service interface for storing and retrieving files. */
public interface FileStorageService {

  /**
   * Store a file.
   *
   * @param file the file to store
   * @return the filename or path where the file is stored
   */
  String storeFile(MultipartFile file);
}
