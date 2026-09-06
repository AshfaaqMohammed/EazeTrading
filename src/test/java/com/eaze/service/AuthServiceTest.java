package com.eaze.service;

import com.eaze.config.JwtProvider;
import com.eaze.model.TwoFactorAuth;
import com.eaze.model.TwoFactorOTP;
import com.eaze.model.User;
import com.eaze.repository.UserRepository;
import com.eaze.request.UserLoginRequest;
import com.eaze.response.AuthResponse;
import com.eaze.service.domain.TwoFactorOTPService;
import com.eaze.service.domain.WalletService;
import com.eaze.service.domain.WatchListService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomUserDetailsService customUserDetailsService;
    @Mock private TwoFactorOTPService twoFactorOTPService;
    @Mock private EmailService emailService;
    @Mock private WatchListService watchListService;
    @Mock private WalletService walletService;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks
    private AuthService authService;

    private UserDetails springUser(String email, String password) {
        return new org.springframework.security.core.userdetails.User(
                email, password, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"));
    }

    // ---------- register ----------

    @Test
    void register_hashesPassword_notPlaintext() throws Exception {
        User input = new User();
        input.setEmail("new@eaze.com");
        input.setFullName("New User");
        input.setPassword("plaintext");

        when(userRepository.findByEmail("new@eaze.com")).thenReturn(null);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customUserDetailsService.loadUserByUsername("new@eaze.com"))
                .thenReturn(springUser("new@eaze.com", "$2a$hashed"));
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        AuthResponse res = authService.register(input);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals("$2a$hashed", saved.getValue().getPassword(), "password must be BCrypt-hashed, not plaintext");
        assertNotEquals("plaintext", saved.getValue().getPassword());

        // wallet + watchlist provisioned at registration
        verify(watchListService).createWatchList(any(User.class));
        verify(walletService).getUserWallet(any(User.class));
        assertTrue(res.isStatus());
        assertEquals("jwt-token", res.getJwt());
    }

    @Test
    void register_rejects_duplicateEmail() {
        User input = new User();
        input.setEmail("exists@eaze.com");
        when(userRepository.findByEmail("exists@eaze.com")).thenReturn(new User());

        assertThrows(Exception.class, () -> authService.register(input));
        verify(userRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    void login_success_whenPasswordMatches_no2FA() throws Exception {
        User authUser = new User();
        authUser.setId(1L);
        authUser.setEmail("user@eaze.com");
        authUser.setTwoFactorAuth(new TwoFactorAuth()); // disabled by default

        when(userRepository.findByEmail("user@eaze.com")).thenReturn(authUser);
        when(customUserDetailsService.loadUserByUsername("user@eaze.com"))
                .thenReturn(springUser("user@eaze.com", "$2a$hashed"));
        when(passwordEncoder.matches("rawpass", "$2a$hashed")).thenReturn(true);
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");

        UserLoginRequest req = new UserLoginRequest();
        req.setEmail("user@eaze.com");
        req.setPassword("rawpass");

        AuthResponse res = authService.login(req);

        assertTrue(res.isStatus());
        assertEquals("jwt-token", res.getJwt());
        assertFalse(res.isTwoFactorAuthEnabled());
    }

    @Test
    void login_throwsBadCredentials_whenPasswordWrong() {
        when(customUserDetailsService.loadUserByUsername("user@eaze.com"))
                .thenReturn(springUser("user@eaze.com", "$2a$hashed"));
        when(passwordEncoder.matches("wrong", "$2a$hashed")).thenReturn(false);
        // authUser lookup happens before authenticate; return a user so NPE doesn't mask the test
        when(userRepository.findByEmail("user@eaze.com")).thenReturn(new User());

        UserLoginRequest req = new UserLoginRequest();
        req.setEmail("user@eaze.com");
        req.setPassword("wrong");

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
        verify(jwtProvider, never()).generateToken(any());
    }

    @Test
    void login_twoFactorEnabled_returnsSessionAndSendsOtp_noJwtInBody() throws Exception {
        TwoFactorAuth twoFa = new TwoFactorAuth();
        twoFa.setEnabled(true);

        User authUser = new User();
        authUser.setId(7L);
        authUser.setEmail("2fa@eaze.com");
        authUser.setTwoFactorAuth(twoFa);

        when(userRepository.findByEmail("2fa@eaze.com")).thenReturn(authUser);
        when(customUserDetailsService.loadUserByUsername("2fa@eaze.com"))
                .thenReturn(springUser("2fa@eaze.com", "$2a$hashed"));
        when(passwordEncoder.matches("rawpass", "$2a$hashed")).thenReturn(true);
        when(jwtProvider.generateToken(any())).thenReturn("jwt-token");
        when(twoFactorOTPService.findByUser(7L)).thenReturn(null);
        TwoFactorOTP otp = new TwoFactorOTP();
        otp.setId("session-123");
        when(twoFactorOTPService.createTwoFactorOtp(eq(authUser), anyString(), anyString())).thenReturn(otp);

        UserLoginRequest req = new UserLoginRequest();
        req.setEmail("2fa@eaze.com");
        req.setPassword("rawpass");

        AuthResponse res = authService.login(req);

        assertTrue(res.isTwoFactorAuthEnabled());
        assertEquals("session-123", res.getSession());
        assertNull(res.getJwt(), "2FA response must not expose the JWT directly");
        verify(emailService).sendVerificationOtpEmail(eq("2fa@eaze.com"), anyString());
    }
}
