package com.eaze.service.domain;

import com.eaze.domian.VerificationType;
import com.eaze.model.User;

public interface UserService {
    User findUserProfileByJwt(String jwt) throws Exception;
    User findUserByEmail(String email) throws Exception;
    User findUserById(Long id) throws Exception;

    User enableTwoFactorAuthentication(VerificationType verificationType, User user);
    User updatePassword(User user, String newPassword);
}
