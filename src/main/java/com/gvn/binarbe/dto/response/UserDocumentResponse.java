package com.gvn.binarbe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocumentResponse {
  private Long id;
  private String fileName;
  private String fileType;
  private String url; // Could be a download URL in the future
}
