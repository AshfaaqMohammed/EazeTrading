package com.eaze.service;

import com.eaze.config.JwtTokenValidator;
import com.eaze.model.User;
import com.eaze.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
//need to implement this, so that spring security will consider this as default user creds, know more on google.
public class CustomUserDetailsService implements UserDetailsService {

    private final Logger LOG = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email);
        if (user == null){
            LOG.error("user not found");
            throw  new UsernameNotFoundException(email);
        }

        List<GrantedAuthority> authorityList = new ArrayList<>();

        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(),authorityList);


    }
}
