package com.gvn.binarbe.dto.response;

import com.gvn.binarbe.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for user details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private UserType userType;
    private Boolean isActive;
    private BranchResponse branch;
    private UserProfileResponse profile;
    private List<String> roles;
    private LocalDateTime createdAt;
}
