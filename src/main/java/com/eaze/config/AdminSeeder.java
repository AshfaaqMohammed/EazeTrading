package com.eaze.config;

import com.eaze.domian.USER_ROLE;
import com.eaze.model.User;
import com.eaze.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.email:}")
    private String seedEmail;

    @Value("${admin.seed.password:}")
    private String seedPassword;

    @Value("${admin.seed.full-name:Administrator}")
    private String seedFullName;

    @Override
    public void run(String... args) throws Exception {
        if (seedEmail == null || seedEmail.isBlank() || seedPassword == null || seedPassword.isBlank()) {
            LOG.warn("Admin seed skipped: admin.seed.email / admin.seed.password not configured.");
            return;
        }

        if (userRepository.existsByRole(USER_ROLE.ROLE_ADMIN)) {
            LOG.info("Admin seed skipped: an admin already exists.");
            return;
        }

        if (userRepository.findByEmail(seedEmail) != null) {
            LOG.warn("Admin seed skipped: a user with email {} already exists but is not an admin.", seedEmail);
            return;
        }

        User admin = new User();
        admin.setEmail(seedEmail);
        admin.setFullName(seedFullName);
        admin.setPassword(passwordEncoder.encode(seedPassword));
        admin.setRole(USER_ROLE.ROLE_ADMIN);

        userRepository.save(admin);
        LOG.info("Admin seed: created first admin with email {}", seedEmail);
    }
}
