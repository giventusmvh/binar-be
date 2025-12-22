package com.gvn.binarbe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for branch details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {

    private Long id;
    private String code;
    private String location;
}
