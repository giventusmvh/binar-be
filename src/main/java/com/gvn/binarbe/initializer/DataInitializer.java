package com.gvn.binarbe.initializer;

import com.gvn.binarbe.entity.*;
import com.gvn.binarbe.enums.RoleName;
import com.gvn.binarbe.enums.UserType;
import com.gvn.binarbe.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data initializer that seeds the database with initial data on startup.
 * Creates branches, roles, permissions, default users, and products.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

        private final BranchRepository branchRepository;
        private final RoleRepository roleRepository;
        private final PermissionRepository permissionRepository;
        private final UserRepository userRepository;
        private final UserProfileRepository userProfileRepository;
        private final ProductRepository productRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        @Transactional
        public void run(String... args) {
                log.info("Starting data initialization...");

                initializeBranches();
                initializePermissions();
                initializeRoles();
                initializeUsers();
                initializeProducts();

                log.info("Data initialization completed!");
        }

        private void initializeBranches() {
                if (branchRepository.count() > 0) {
                        log.info("Branches already exist, skipping...");
                        return;
                }

                log.info("Creating branches...");

                Branch jakarta = Branch.builder()
                                .code("JKT")
                                .location("Jakarta")
                                .build();

                Branch surabaya = Branch.builder()
                                .code("SBY")
                                .location("Surabaya")
                                .build();

                Branch bandung = Branch.builder()
                                .code("BDG")
                                .location("Bandung")
                                .build();

                branchRepository.saveAll(Arrays.asList(jakarta, surabaya, bandung));
                log.info("Created 3 branches: JKT, SBY, BDG");
        }

        private void initializePermissions() {
                if (permissionRepository.count() > 0) {
                        log.info("Permissions already exist, skipping...");
                        return;
                }

                log.info("Creating permissions...");

                List<Permission> permissions = Arrays.asList(
                                // User permissions
                                Permission.builder().code("USER_READ").description("Read user data").build(),
                                Permission.builder().code("USER_CREATE").description("Create users").build(),
                                Permission.builder().code("USER_UPDATE").description("Update users").build(),
                                Permission.builder().code("USER_DELETE").description("Delete users").build(),

                                // Role permissions
                                Permission.builder().code("ROLE_READ").description("Read roles").build(),
                                Permission.builder().code("ROLE_ASSIGN").description("Assign roles to users").build(),
                                Permission.builder().code("ROLE_MANAGE").description("Manage role permissions").build(),

                                // Loan permissions
                                Permission.builder().code("LOAN_CREATE").description("Create loan applications")
                                                .build(),
                                Permission.builder().code("LOAN_READ").description("Read loan applications").build(),
                                Permission.builder().code("LOAN_READ_ALL").description("Read all loan applications")
                                                .build(),
                                Permission.builder().code("LOAN_READ_BRANCH")
                                                .description("Read branch loan applications").build(),

                                // Approval permissions
                                Permission.builder().code("LOAN_APPROVE_MARKETING").description("Approve as Marketing")
                                                .build(),
                                Permission.builder().code("LOAN_APPROVE_BRANCH_MANAGER")
                                                .description("Approve as Branch Manager")
                                                .build(),
                                Permission.builder().code("LOAN_APPROVE_BACKOFFICE")
                                                .description("Final approval as Backoffice")
                                                .build(),
                                Permission.builder().code("LOAN_REJECT").description("Reject loan applications")
                                                .build(),
                                Permission.builder().code("LOAN_RETURN").description("Return loan for revision")
                                                .build(),

                                // Product permissions
                                Permission.builder().code("PRODUCT_READ").description("Read products").build(),
                                Permission.builder().code("PRODUCT_MANAGE").description("Manage products").build(),

                                // Branch permissions
                                Permission.builder().code("BRANCH_READ").description("Read branches").build(),
                                Permission.builder().code("BRANCH_MANAGE").description("Manage branches").build());

                permissionRepository.saveAll(permissions);
                log.info("Created {} permissions", permissions.size());
        }

        private void initializeRoles() {
                if (roleRepository.count() > 0) {
                        log.info("Roles already exist, skipping...");
                        return;
                }

                log.info("Creating roles...");

                // Get all permissions
                Permission userRead = permissionRepository.findByCode("USER_READ").orElseThrow();
                Permission userCreate = permissionRepository.findByCode("USER_CREATE").orElseThrow();
                Permission userUpdate = permissionRepository.findByCode("USER_UPDATE").orElseThrow();
                Permission userDelete = permissionRepository.findByCode("USER_DELETE").orElseThrow();
                Permission roleRead = permissionRepository.findByCode("ROLE_READ").orElseThrow();
                Permission roleAssign = permissionRepository.findByCode("ROLE_ASSIGN").orElseThrow();
                Permission roleManage = permissionRepository.findByCode("ROLE_MANAGE").orElseThrow();
                Permission loanCreate = permissionRepository.findByCode("LOAN_CREATE").orElseThrow();
                Permission loanRead = permissionRepository.findByCode("LOAN_READ").orElseThrow();
                Permission loanReadAll = permissionRepository.findByCode("LOAN_READ_ALL").orElseThrow();
                Permission loanReadBranch = permissionRepository.findByCode("LOAN_READ_BRANCH").orElseThrow();
                Permission loanApproveMarketing = permissionRepository.findByCode("LOAN_APPROVE_MARKETING")
                                .orElseThrow();
                Permission loanApproveBranchManager = permissionRepository.findByCode("LOAN_APPROVE_BRANCH_MANAGER")
                                .orElseThrow();
                Permission loanApproveBackoffice = permissionRepository.findByCode("LOAN_APPROVE_BACKOFFICE")
                                .orElseThrow();
                Permission loanReject = permissionRepository.findByCode("LOAN_REJECT").orElseThrow();
                Permission loanReturn = permissionRepository.findByCode("LOAN_RETURN").orElseThrow();
                Permission productRead = permissionRepository.findByCode("PRODUCT_READ").orElseThrow();
                Permission productManage = permissionRepository.findByCode("PRODUCT_MANAGE").orElseThrow();
                Permission branchRead = permissionRepository.findByCode("BRANCH_READ").orElseThrow();
                Permission branchManage = permissionRepository.findByCode("BRANCH_MANAGE").orElseThrow();

                // SUPERADMIN - all permissions
                Set<Permission> superadminPerms = new HashSet<>(permissionRepository.findAll());
                Role superadmin = Role.builder()
                                .name(RoleName.SUPERADMIN)
                                .permissions(superadminPerms)
                                .build();

                // MARKETING - branch-restricted loan processing
                Set<Permission> marketingPerms = new HashSet<>(Arrays.asList(
                                loanReadBranch, loanApproveMarketing, loanReject, productRead, branchRead));
                Role marketing = Role.builder()
                                .name(RoleName.MARKETING)
                                .permissions(marketingPerms)
                                .build();

                // BRANCH_MANAGER - branch-restricted loan approval
                Set<Permission> branchManagerPerms = new HashSet<>(Arrays.asList(
                                loanReadBranch, loanApproveBranchManager, loanReject, productRead, branchRead,
                                userRead));
                Role branchManager = Role.builder()
                                .name(RoleName.BRANCH_MANAGER)
                                .permissions(branchManagerPerms)
                                .build();

                // BACKOFFICE - final approval across all branches
                Set<Permission> backofficePerms = new HashSet<>(Arrays.asList(
                                loanReadAll, loanApproveBackoffice, loanReject, loanReturn, productRead, branchRead));
                Role backoffice = Role.builder()
                                .name(RoleName.BACKOFFICE)
                                .permissions(backofficePerms)
                                .build();

                // CUSTOMER - loan creation and tracking
                Set<Permission> customerPerms = new HashSet<>(Arrays.asList(
                                loanCreate, loanRead, productRead, branchRead));
                Role customer = Role.builder()
                                .name(RoleName.CUSTOMER)
                                .permissions(customerPerms)
                                .build();

                roleRepository.saveAll(Arrays.asList(superadmin, marketing, branchManager, backoffice, customer));
                log.info("Created 5 roles with permissions");
        }

        private void initializeUsers() {
                if (userRepository.count() > 0) {
                        log.info("Users already exist, skipping...");
                        return;
                }

                log.info("Creating users...");

                Role superadminRole = roleRepository.findByName(RoleName.SUPERADMIN).orElseThrow();
                Role marketingRole = roleRepository.findByName(RoleName.MARKETING).orElseThrow();
                Role branchManagerRole = roleRepository.findByName(RoleName.BRANCH_MANAGER).orElseThrow();
                Role backofficeRole = roleRepository.findByName(RoleName.BACKOFFICE).orElseThrow();

                Branch jakarta = branchRepository.findByCode("JKT").orElseThrow();
                Branch surabaya = branchRepository.findByCode("SBY").orElseThrow();

                // Create Superadmin (no branch)
                User admin = User.builder()
                                .name("Super Admin")
                                .email("admin@loan.com")
                                .password(passwordEncoder.encode("admin123"))
                                .userType(UserType.INTERNAL)
                                .isActive(true)
                                .roles(new HashSet<>(Arrays.asList(superadminRole)))
                                .build();
                userRepository.save(admin);

                // Create Backoffice user (no branch - can see all)
                User backofficeUser = User.builder()
                                .name("Backoffice User")
                                .email("backoffice@loan.com")
                                .password(passwordEncoder.encode("backoffice123"))
                                .userType(UserType.INTERNAL)
                                .isActive(true)
                                .roles(new HashSet<>(Arrays.asList(backofficeRole)))
                                .build();
                userRepository.save(backofficeUser);

                // Create Jakarta branch users
                User marketingJkt = User.builder()
                                .name("Marketing Jakarta")
                                .email("marketing.jkt@loan.com")
                                .password(passwordEncoder.encode("marketing123"))
                                .userType(UserType.INTERNAL)
                                .branch(jakarta)
                                .isActive(true)
                                .roles(new HashSet<>(Arrays.asList(marketingRole)))
                                .build();
                userRepository.save(marketingJkt);

                User bmJkt = User.builder()
                                .name("Branch Manager Jakarta")
                                .email("bm.jkt@loan.com")
                                .password(passwordEncoder.encode("bm123"))
                                .userType(UserType.INTERNAL)
                                .branch(jakarta)
                                .isActive(true)
                                .roles(new HashSet<>(Arrays.asList(branchManagerRole)))
                                .build();
                userRepository.save(bmJkt);

                // Create Surabaya branch users
                User marketingSby = User.builder()
                                .name("Marketing Surabaya")
                                .email("marketing.sby@loan.com")
                                .password(passwordEncoder.encode("marketing123"))
                                .userType(UserType.INTERNAL)
                                .branch(surabaya)
                                .isActive(true)
                                .roles(new HashSet<>(Arrays.asList(marketingRole)))
                                .build();
                userRepository.save(marketingSby);

                User bmSby = User.builder()
                                .name("Branch Manager Surabaya")
                                .email("bm.sby@loan.com")
                                .password(passwordEncoder.encode("bm123"))
                                .userType(UserType.INTERNAL)
                                .branch(surabaya)
                                .isActive(true)
                                .roles(new HashSet<>(Arrays.asList(branchManagerRole)))
                                .build();
                userRepository.save(bmSby);

                // Create internal users without roles (to be assigned by superadmin)
                User internalNoRole = User.builder()
                                .name("Internal User No Role")
                                .email("internal@loan.com")
                                .password(passwordEncoder.encode("internal123"))
                                .userType(UserType.INTERNAL)
                                .branch(jakarta)
                                .isActive(true)
                                .roles(new HashSet<>())
                                .build();
                userRepository.save(internalNoRole);

                log.info("Created 7 internal users");

                // Create customer users
                Role customerRole = roleRepository.findByName(RoleName.CUSTOMER).orElseThrow();

                // Customer with COMPLETE profile (can submit loans)
                User customerComplete = User.builder()
                                .name("John Doe")
                                .email("john.doe@email.com")
                                .password(passwordEncoder.encode("customer123"))
                                .userType(UserType.CUSTOMER)
                                .isActive(true)
                                .roles(new HashSet<>(Arrays.asList(customerRole)))
                                .build();
                customerComplete = userRepository.save(customerComplete);

                // Create complete profile for John Doe
                UserProfile completeProfile = UserProfile.builder()
                                .user(customerComplete)
                                .birthdate(LocalDate.of(1990, 5, 15))
                                .phone("081234567890")
                                .address("Jl. Sudirman No. 123, Jakarta Pusat")
                                .nik("3174051505900001")
                                .build();
                userProfileRepository.save(completeProfile);

                // Customer with EMPTY profile (cannot submit loans until profile completed)
                User customerEmpty = User.builder()
                                .name("Jane Smith")
                                .email("jane.smith@email.com")
                                .password(passwordEncoder.encode("customer123"))
                                .userType(UserType.CUSTOMER)
                                .isActive(true)
                                .roles(new HashSet<>(Arrays.asList(customerRole)))
                                .build();
                customerEmpty = userRepository.save(customerEmpty);

                // Create empty profile for Jane Smith
                UserProfile emptyProfile = UserProfile.builder()
                                .user(customerEmpty)
                                .build();
                userProfileRepository.save(emptyProfile);

                log.info("Created 2 customer users (1 with complete profile, 1 with empty profile)");
        }

        private void initializeProducts() {
                if (productRepository.count() > 0) {
                        log.info("Products already exist, skipping...");
                        return;
                }

                log.info("Creating products...");

                List<Product> products = Arrays.asList(
                                Product.builder()
                                                .name("BRONZE")
                                                .amount(new BigDecimal("5000000"))
                                                .tenor(12)
                                                .interestRate(new BigDecimal("12.00"))
                                                .build(),
                                Product.builder()
                                                .name("SILVER")
                                                .amount(new BigDecimal("10000000"))
                                                .tenor(24)
                                                .interestRate(new BigDecimal("10.00"))
                                                .build(),
                                Product.builder()
                                                .name("GOLD")
                                                .amount(new BigDecimal("25000000"))
                                                .tenor(36)
                                                .interestRate(new BigDecimal("8.50"))
                                                .build(),
                                Product.builder()
                                                .name("PLATINUM")
                                                .amount(new BigDecimal("50000000"))
                                                .tenor(48)
                                                .interestRate(new BigDecimal("7.00"))
                                                .build());

                productRepository.saveAll(products);
                log.info("Created 4 loan products: BRONZE, SILVER, GOLD, PLATINUM");
        }
}
