package com.eaze.service;

import com.eaze.config.JwtProvider;
import com.eaze.domian.USER_ROLE;
import com.eaze.exceptions.ApiResponseException;
import com.eaze.model.User;
import com.eaze.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtProvider jwtProvider;

    @InjectMocks private UserServiceImpl userService;

    private User userWithRole(Long id, USER_ROLE role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    @Test
    void updateUserRole_promotesCustomerToAdmin() throws Exception {
        User customer = userWithRole(2L, USER_ROLE.ROLE_CUSTOMER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUserRole(2L, USER_ROLE.ROLE_ADMIN);

        assertEquals(USER_ROLE.ROLE_ADMIN, result.getRole());
    }

    @Test
    void updateUserRole_blocksDemotingLastAdmin() {
        User admin = userWithRole(1L, USER_ROLE.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(USER_ROLE.ROLE_ADMIN)).thenReturn(1L); // only one admin

        ApiResponseException ex = assertThrows(ApiResponseException.class,
                () -> userService.updateUserRole(1L, USER_ROLE.ROLE_CUSTOMER));
        assertTrue(ex.getMessage().toLowerCase().contains("last"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserRole_allowsDemotingAdmin_whenOthersExist() throws Exception {
        User admin = userWithRole(1L, USER_ROLE.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(USER_ROLE.ROLE_ADMIN)).thenReturn(2L); // more than one admin
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUserRole(1L, USER_ROLE.ROLE_CUSTOMER);

        assertEquals(USER_ROLE.ROLE_CUSTOMER, result.getRole());
    }

    @Test
    void updateUserRole_throws_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(Exception.class,
                () -> userService.updateUserRole(99L, USER_ROLE.ROLE_ADMIN));
    }
}
