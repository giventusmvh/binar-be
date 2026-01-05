package com.gvn.binarbe.security;

import com.gvn.binarbe.entity.User;
import com.gvn.binarbe.repository.UserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for loading user-specific data. Builds authorities from
 * user roles and permissions.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmailWithRoles(email)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with email: " + email));

    if (!user.getIsActive()) {
      throw new UsernameNotFoundException("User account is disabled");
    }

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPassword(),
        user.getIsActive(),
        true,
        true,
        true,
        getAuthorities(user));
  }

  /**
   * Build granted authorities from user roles and permissions. Role names are prefixed with "ROLE_"
   * for Spring Security. Permissions are added directly as authorities.
   */
  private Collection<? extends GrantedAuthority> getAuthorities(User user) {
    Set<GrantedAuthority> authorities = new HashSet<>();

    user.getRoles()
        .forEach(
            role -> {
              // Add role authority
              authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));

              // Add permission authorities from each role
              role.getPermissions()
                  .forEach(
                      permission ->
                          authorities.add(new SimpleGrantedAuthority(permission.getCode())));
            });

    return authorities;
  }
}
