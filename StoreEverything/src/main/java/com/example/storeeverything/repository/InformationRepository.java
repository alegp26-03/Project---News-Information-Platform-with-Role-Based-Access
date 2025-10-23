package com.example.storeeverything.repository;

import com.example.storeeverything.model.Information;
import com.example.storeeverything.model.User;
import com.example.storeeverything.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Information entity.
 * Provides standard CRUD operations and custom query methods for sorting and filtering.
 */
@Repository
public interface InformationRepository extends JpaRepository<Information, Long> {

    // --- Methods for "Displaying 'their information'" (2p.) ---
    // Find all information entries belonging to a specific user
    List<Information> findByUser(User user);

    // --- Methods for Sorting (2p.) ---
    // Sort by dateAdded in ascending order for a specific user
    List<Information> findByUserOrderByDateAddedAsc(User user);

    // Sort by dateAdded in descending order for a specific user
    List<Information> findByUserOrderByDateAddedDesc(User user);

    // Sort by category name in ascending order for a specific user
    // Note: This requires accessing the name property of the nested Category object.
    // Spring Data JPA can often handle this with property expressions (e.g., category.name)
    List<Information> findByUserOrderByCategoryNameAsc(User user);

    // Sort by category name in descending order for a specific user
    List<Information> findByUserOrderByCategoryNameDesc(User user);

    // --- Methods for Filtering (2p.) ---
    // Filter by date range for a specific user
    List<Information> findByUserAndDateAddedBetween(User user, LocalDate startDate, LocalDate endDate);

    // Filter by category for a specific user
    List<Information> findByUserAndCategory(User user, Category category);

    // Filter by title or content (search functionality)
    // Note: This query method name is correctly split to apply `User` filter for both title and content.
    List<Information> findByUserAndTitleContainingIgnoreCaseOrUserAndContentContainingIgnoreCase(User user1, String titleKeyword, User user2, String contentKeyword);

    // You can also combine filtering and sorting, e.g.:
    List<Information> findByUserAndCategoryOrderByDateAddedDesc(User user, Category category);

    // Method to find information by a shared link (if we implement a unique link field)
    Information findByLink(String link);

    // Existing NEW METHOD: Filter by user, category, and date range
    List<Information> findByUserAndCategoryAndDateAddedBetween(User user, Category category, LocalDate startDate, LocalDate endDate);

    // NEW METHOD FOR SHARING: Find information items that are shared with a specific user
    List<Information> findBySharedWithUsersContains(User user);

    // NEW method to find by shareable token
    Optional<Information> findByShareableToken(String shareableToken);

}