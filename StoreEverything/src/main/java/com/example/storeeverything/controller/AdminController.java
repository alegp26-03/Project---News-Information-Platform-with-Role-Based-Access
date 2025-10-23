package com.example.storeeverything.controller;

import com.example.storeeverything.model.User;
import com.example.storeeverything.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;

/**
 * Manages admin tasks: viewing users and changing their roles.
 * Only 'ADMIN' users can access this section.
 */
@Controller // This is a web page manager.
@RequestMapping("/admin") // All addresses here start with "/admin".
@PreAuthorize("hasRole('ADMIN')") // Only 'ADMIN' users allowed here.
public class AdminController {

    // Our tool to get/save user data from the database.
    private final UserRepository userRepository;

    // A list of all allowed user roles (e.g., LIMITED, FULL, ADMIN).
    private static final List<String> ALL_ROLES = Arrays.asList("LIMITED_USER", "FULL_USER", "ADMIN");

    /**
     * Sets up this admin manager, giving it the user database tool.
     *
     * @param userRepository The database tool for users.
     */
    @Autowired // Spring automatically provides the tool.
    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Shows a web page listing all users.
     * (Accessed by visiting "/admin/users"
     */
    @GetMapping("/users") // Handles requests to view all users.
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll(); // Get all users.
        model.addAttribute("users", users); // Put users in the basket.
        return "admin/users"; // Show the list page.
    }

    /**
     * Shows a form to change one user's role.
     * (Accessed by clicking "Edit Role" for a specific user: "/admin/users/edit-role/ID")
     */
    @GetMapping("/users/edit-role/{id}") // Handles requests to open the role edit form.
    public String showEditUserRoleForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userRepository.findById(id); // Try to find the user.
        if (userOptional.isEmpty()) { // If user not found:
            redirectAttributes.addFlashAttribute("error", "User not found."); // Send error message.
            return "redirect:/admin/users"; // Go back to user list.
        }
        User user = userOptional.get(); // Get the user data.
        model.addAttribute("user", user); // Put user data in basket.
        model.addAttribute("availableRoles", ALL_ROLES); // Put allowed roles in basket.
        return "admin/edit-user-role"; // Show the edit form.
    }

    /**
     * Saves the new role chosen for a user.
     * (Happens when the role edit form is submitted)
     */
    @PostMapping("/users/edit-role/{id}") // Handles form submissions to update roles.
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam("newRole") String newRole,
                                 RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userRepository.findById(id); // Find the user.
        if (userOptional.isEmpty()) { // If user not found:
            redirectAttributes.addFlashAttribute("error", "User not found."); // Send error.
            return "redirect:/admin/users"; // Go back.
        }

        User user = userOptional.get(); // Get user data.

        // Check if the chosen role is valid.
        if (!ALL_ROLES.contains(newRole)) {
            redirectAttributes.addFlashAttribute("error", "Invalid role selected."); // Send error.
            return "redirect:/admin/users/edit-role/" + id; // Go back to the form.
        }

        user.setRole(newRole); // Set the new role.
        userRepository.save(user); // Save changes to the database.

        redirectAttributes.addFlashAttribute("success", "User " + user.getLogin() + "'s role updated to " + newRole + " successfully!"); // Send success message.
        return "redirect:/admin/users"; // Go to user list page.
    }
}
