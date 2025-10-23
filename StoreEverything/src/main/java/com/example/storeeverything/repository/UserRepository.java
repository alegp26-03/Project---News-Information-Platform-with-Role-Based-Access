package com.example.storeeverything.repository;

import com.example.storeeverything.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for User entities.
 * Extends JpaRepository to provide CRUD operations.
 */
@Repository // Marks this interface as a Spring Data repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Custom method to find a User by their login (username).
     * Spring Data JPA automatically generates the implementation based on the method name.
     * @param login The username to search for.
     * @return The User object if found, null otherwise.
     */
    User findByLogin(String login);
}