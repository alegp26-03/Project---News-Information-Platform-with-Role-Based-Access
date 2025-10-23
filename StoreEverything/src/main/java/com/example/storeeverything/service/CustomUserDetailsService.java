package com.example.storeeverything.service;

import com.example.storeeverything.model.User;
import com.example.storeeverything.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections; // For creating a single-element set

/**
 * Tells Spring Security how to find user info (like username and role) from our database
 * when someone tries to log in.
 */
@Service // Marks this as a service component.
public class CustomUserDetailsService implements UserDetailsService {

    // Our tool to get user data from the database.
    private final UserRepository userRepository;

    /**
     * Sets up this service, giving it the user database tool.
     *
     * @param userRepository The database tool for users.
     */
    // Spring automatically provides the tool.
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds a user by their login name for Spring Security.
     *
     * @param login The username (login) to find.
     * @return User details for Spring Security.
     * @throws UsernameNotFoundException If user isn't found.
     */
    @Override // This method is required by Spring Security.
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Find the user in our database.
        User user = userRepository.findByLogin(login);

        // If user not found, throw an error.
        if (user == null) {
            throw new UsernameNotFoundException("User not found with login: " + login);
        }

        // Convert our user's role (e.g., "ADMIN") into a format Spring Security understands (e.g., "ROLE_ADMIN").
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole());

        // Return Spring Security's UserDetails object with login, scrambled password, and role.
        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),         // User's login name.
                user.getPassword(),      // User's scrambled password.
                Collections.singleton(authority) // User's role.
        );
    }
}
