package com.example.storeeverything.controller;

import com.example.storeeverything.model.Category;
import com.example.storeeverything.model.User;
import com.example.storeeverything.repository.CategoryRepository;
import com.example.storeeverything.repository.UserRepository;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid; // For input checks

/**
 * Manages public pages (home, login, sign-up) and the dashboard for logged-in users.
 * Also sets up initial data when the app starts.
 */
@Controller // This is a web page manager.
public class HomeController {

    // Tools to talk to the database for users and categories, and to scramble passwords.
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Sets up this controller, giving it necessary database and password tools.
     */
    public HomeController(UserRepository userRepository, CategoryRepository categoryRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Puts basic info (like a default admin user and categories) into the database
     * when the application first starts up.
     */
    @EventListener // Runs automatically when the app is ready.
    public void seedInitialData(ContextRefreshedEvent event) {
        // Create an "admin" user if one doesn't exist.
        if (userRepository.findByLogin("admin") == null) {
            User admin = new User();
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setLogin("admin");
            admin.setPassword(passwordEncoder.encode("adminpass")); // Scramble password.
            admin.setAge(30);
            admin.setRole("ADMIN"); // Give admin role.
            userRepository.save(admin); // Save to database.
            System.out.println("Admin user 'admin' created.");
        }

        // Create default categories (general, work, personal) if they don't exist.
        if (categoryRepository.count() == 0) {
            categoryRepository.save(new Category(null, "general"));
            categoryRepository.save(new Category(null, "work"));
            categoryRepository.save(new Category(null, "personal"));
            System.out.println("Default categories created.");
        }
    }

    /**
     * Shows the main homepage.
     * (Accessed by visiting "/" or "/home")
     *
     * @return The "home" web page.
     */
    @GetMapping({"/", "/home"}) // Handles requests for the homepage.
    public String home() {
        return "home"; // Shows home.html.
    }

    /**
     * Shows the user login page.
     * (Accessed by visiting "/login")
     *
     * @return The "login" web page.
     */
    @GetMapping("/login") // Handles requests for the login page.
    public String login() {
        return "login"; // Shows login.html.
    }

    /**
     * Shows the user sign-up (registration) form.
     * (Accessed by visiting "/register")
     *
     * @return The "register" web page.
     */
    @GetMapping("/register") // Handles requests to open the sign-up form.
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User()); // Put an empty user form in the basket.
        return "register"; // Shows register.html.
    }

    /**
     * Processes the sign-up form when a new user tries to register.
     *
     * @return Redirects to login on success, or back to sign-up on errors.
     */
    @PostMapping("/register") // Handles submissions (POST) of the sign-up form.
    public String registerUser(@Valid User newUser, BindingResult result, Model model) {
        if (result.hasErrors()) { // If there are errors in the form input:
            return "register"; // Show the sign-up page again with errors.
        }
        newUser.setPassword(passwordEncoder.encode(newUser.getPassword())); // Scramble user's password.
        newUser.setRole("LIMITED_USER"); // Give new users a default role.
        userRepository.save(newUser); // Save the new user to the database.
        return "redirect:/login?registered"; // Go to login page, showing "registered" message.
    }

    /**
     * Shows the dashboard page for users who are logged in.
     * (Accessed by visiting "/dashboard" after logging in)
     *
     * @return The "dashboard" web page.
     */
    @GetMapping("/dashboard") // Handles requests for the dashboard.
    public String dashboard() {
        return "dashboard"; // Shows dashboard.html.
    }
}
