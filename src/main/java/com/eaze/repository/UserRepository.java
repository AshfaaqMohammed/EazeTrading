package com.eaze.repository;

import com.eaze.domian.USER_ROLE;
import com.eaze.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByRole(USER_ROLE role);
    long countByRole(USER_ROLE role);
}
