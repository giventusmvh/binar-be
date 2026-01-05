package com.gvn.binarbe.enums;

/**
 * Enum representing available roles in the system. Each role has specific permissions and access
 * levels.
 */
public enum RoleName {
  SUPERADMIN, // Full system access
  MARKETING, // Branch-restricted loan processing
  BRANCH_MANAGER, // Branch-restricted loan approval
  BACKOFFICE, // Final approval across all branches
  CUSTOMER // External customer access
}
