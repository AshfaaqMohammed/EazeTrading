package com.eaze.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class JwtProvider {

    private static SecretKey KEY = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

    public static String generateToken(Authentication auth) {

        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        String roles = populateAuthorities(authorities);

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime()+8_64_00_000))
                .claim("email", auth.getName())
                .claim("authorities", roles)
                .signWith(KEY)
                .compact();

    }

    public static String getEmailFromToken(String token) {
        token = token.substring(7);
        Claims claims = Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return String.valueOf(claims.get("email"));

    }

    private static String populateAuthorities(Collection<? extends GrantedAuthority> authorities){
        Set<String> auth = new HashSet<>();
        for (GrantedAuthority ga : authorities) {
            auth.add(ga.getAuthority());
        }
        return String.join(",",auth);
    }

}
