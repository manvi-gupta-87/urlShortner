package com.urlshortener.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// jwt service is used to generate and validate jwt tokens
// jwt tokens are used to authenticate and authorize users
// jwt tokens are used to store user information in a secure way

// When a user logs in, the server generates a jwt token and sends it to the client
// The client stores the token in local storage or a cookie
// The client sends the token in the Authorization header of every request to the server
// The server validates the token and extracts the user information from the token
// The server uses the user information to authenticate and authorize the user
// The server can also use the user information to store in the database
// The server can also use the user information to generate a new token for the user


@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    //claim typically contains the username, expiration date, and other metadata

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // jwt token has three parts: header, payload, signature
    // header: contains the algorithm used to sign the token
    // payload: contains the claims
    // signature: is the hash of the header and payload
    // jwt token is a string of the form: header.payload.signature
    // jwt token is used to authenticate the user
    // jwt token is used to authorize the user
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // getSigningKey is used to get the key used to sign the jwt token
    // this is the password that only the server knows. It is used to sign the jwt token
    // the key is used to verify the signature of the jwt token
   
    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 