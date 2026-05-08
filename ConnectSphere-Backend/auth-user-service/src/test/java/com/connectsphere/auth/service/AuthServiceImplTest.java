package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.*;
import com.connectsphere.auth.email.EmailService;
import com.connectsphere.auth.entity.User;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.exception.ResourceNotFoundException;
import com.connectsphere.auth.repository.UserRepository;
import com.connectsphere.auth.security.CustomUserDetailsService;
import com.connectsphere.auth.security.JwtUtil;
import com.connectsphere.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private EmailService emailService;

    @InjectMocks private AuthServiceImpl authService;

    private User mockUser() {
        return User.builder()
                .userId("u1")
                .username("test")
                .email("test@example.com")
                .passwordHash("hashed")
                .fullName("Test User")
                .role(User.Role.USER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("register: success")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("test");
        req.setEmail("test@example.com");
        req.setPassword("pass");
        req.setFullName("Test User");
        
        Mockito.when(userRepository.existsByEmail(anyString())).thenReturn(false);
        Mockito.when(userRepository.existsByUsername(anyString())).thenReturn(false);
        Mockito.when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        
        User savedUser = mockUser();
        Mockito.when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        Mockito.when(jwtUtil.generateToken(any(), anyString(), anyString(), anyString())).thenReturn("token");
        Mockito.when(jwtUtil.getExpirationMs()).thenReturn(3600000L);

        AuthResponse resp = authService.register(req);

        assertThat(resp.getAccessToken()).isEqualTo("token");
        Mockito.verify(emailService).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("register: email taken throws exception")
    void register_emailTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("test");
        req.setEmail("test@example.com");
        req.setPassword("pass");
        req.setFullName("Test User");
        
        Mockito.when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("register: username taken throws exception")
    void register_usernameTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("test");
        req.setEmail("test@example.com");
        req.setPassword("pass");
        req.setFullName("Test User");
        
        Mockito.when(userRepository.existsByEmail(anyString())).thenReturn(false);
        Mockito.when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    @DisplayName("login: success")
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("pass");
        
        User user = mockUser();
        Mockito.when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        
        UserDetails userDetails = Mockito.mock(UserDetails.class);
        Mockito.when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        Mockito.when(jwtUtil.generateToken(any(), anyString(), anyString(), anyString())).thenReturn("token");

        AuthResponse resp = authService.login(req);

        assertThat(resp.getAccessToken()).isEqualTo("token");
        Mockito.verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login: bad credentials throws exception")
    void login_badCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("wrong");
        
        Mockito.when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login: user not active throws exception")
    void login_notActive() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com");
        req.setPassword("pass");
        
        User inactive = mockUser();
        inactive.setActive(false);
        Mockito.when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Account is deactivated");
    }

    @Test
    @DisplayName("getUserById: success")
    void getUserById_success() {
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(mockUser()));
        UserProfileDto dto = authService.getUserById("u1");
        assertThat(dto.getUserId()).isEqualTo("u1");
    }

    @Test
    @DisplayName("getUserByEmail: success")
    void getUserByEmail_success() {
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser()));
        UserProfileDto dto = authService.getUserByEmail("test@example.com");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("updateProfile: success")
    void updateProfile_success() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name");
        req.setBio("New Bio");
        
        User user = mockUser();
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        Mockito.when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileDto dto = authService.updateProfile("u1", req);

        assertThat(dto.getFullName()).isEqualTo("New Name");
        assertThat(user.getBio()).isEqualTo("New Bio");
    }

    @Test
    @DisplayName("updateProfile: email already taken throws exception")
    void updateProfile_emailTaken() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("taken@example.com");
        
        User user = mockUser();
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        Mockito.when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.updateProfile("u1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already taken");
    }

    @Test
    @DisplayName("updateProfile: username already taken throws exception")
    void updateProfile_usernameTaken() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("taken_user");
        
        User user = mockUser();
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        Mockito.when(userRepository.existsByUsername("taken_user")).thenReturn(true);

        assertThatThrownBy(() -> authService.updateProfile("u1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    @DisplayName("updateProfile: changes email and username successfully")
    void updateProfile_changeEmailAndUsername() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setEmail("new@example.com");
        req.setUsername("new_user");
        req.setProfilePicUrl("http://new-pic");
        
        User user = mockUser();
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        Mockito.when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        Mockito.when(userRepository.existsByUsername("new_user")).thenReturn(false);
        Mockito.when(userRepository.save(any(User.class))).thenReturn(user);

        authService.updateProfile("u1", req);

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getUsername()).isEqualTo("new_user");
        assertThat(user.getProfilePicUrl()).isEqualTo("http://new-pic");
    }

    @Test
    @DisplayName("changePassword: success")
    void changePassword_success() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old");
        req.setNewPassword("new");
        
        User user = mockUser();
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("old", "hashed")).thenReturn(true);
        Mockito.when(passwordEncoder.encode("new")).thenReturn("newHashed");

        authService.changePassword("u1", req);

        assertThat(user.getPasswordHash()).isEqualTo("newHashed");
        Mockito.verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword: wrong old password")
    void changePassword_wrongPassword() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("new");
        
        User user = mockUser();
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword("u1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("forgotPassword: sends OTP email")
    void forgotPassword_success() {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("test@example.com");
        User user = mockUser();
        
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(req);

        Mockito.verify(emailService).sendOtpEmail(eq("test@example.com"), eq("Test User"), anyString());
        Mockito.verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPassword: success")
    void resetPassword_success() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("test@example.com");
        req.setOtp("482910");
        req.setNewPassword("newPass");
        
        User user = mockUser();
        user.setResetToken("482910");
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
        
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.encode("newPass")).thenReturn("newHashed");

        authService.resetPassword(req);

        assertThat(user.getPasswordHash()).isEqualTo("newHashed");
        assertThat(user.getResetToken()).isNull();
        Mockito.verify(userRepository).save(user);
    }

    @Test
    @DisplayName("resetPassword: OTP expired throws exception")
    void resetPassword_expired() {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("test@example.com");
        req.setOtp("482910");
        req.setNewPassword("newPass");
        
        User user = mockUser();
        user.setResetToken("482910");
        user.setResetTokenExpiry(LocalDateTime.now().minusHours(1));
        
        Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetPassword(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OTP has expired");
    }

    @Test
    @DisplayName("searchUsers: success")
    void searchUsers() {
        Mockito.when(userRepository.searchByUsernameOrFullName("query")).thenReturn(List.of(mockUser()));
        List<UserProfileDto> res = authService.searchUsers("query");
        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("deactivateAccount: success")
    void deactivateAccount() {
        User user = mockUser();
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        authService.deactivateAccount("u1");
        assertThat(user.isActive()).isFalse();
    }

    @Test
    @DisplayName("reactivateAccount: success")
    void reactivateAccount() {
        User user = mockUser();
        user.setActive(false);
        Mockito.when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        authService.reactivateAccount("u1");
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("deleteAccount: success")
    void deleteAccount() {
        authService.deleteAccount("u1");
        Mockito.verify(userRepository).deleteByUserId("u1");
    }

    @Test
    @DisplayName("getAllUsers and getUsersByRole: success")
    void getLists() {
        Mockito.when(userRepository.findAll()).thenReturn(List.of(mockUser()));
        Mockito.when(userRepository.findAllByRole(User.Role.ADMIN)).thenReturn(List.of(mockUser()));
        
        assertThat(authService.getAllUsers()).hasSize(1);
        assertThat(authService.getUsersByRole(User.Role.ADMIN)).hasSize(1);
    }
}
