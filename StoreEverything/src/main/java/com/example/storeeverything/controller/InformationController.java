package com.example.storeeverything.controller;

import com.example.storeeverything.model.Category;
import com.example.storeeverything.model.Information;
import com.example.storeeverything.model.User;
import com.example.storeeverything.repository.CategoryRepository;
import com.example.storeeverything.repository.InformationRepository;
import com.example.storeeverything.repository.UserRepository;
import com.example.storeeverything.session.SessionDataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Manages all actions related to "Information" items:
 * viewing, adding, editing, deleting, and sharing.
 */
@Controller // This is a web page manager.
@RequestMapping("/information") // All addresses here start with "/information".
public class InformationController {

    // Tools to talk to the database for information, categories, and users.
    @Autowired
    private InformationRepository informationRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionDataStore sessionDataStore; // Tool for handling temporary, unsaved changes.

    /**
     * Finds out who the current user is.
     *
     * @return The logged-in user, or null if not logged in.
     */
    private User getCurrentUser() {
        // Get details of the current user who is logged in.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // If no one is logged in, or it's an anonymous user, return nothing.
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        // Find the user's full details from the database.
        String currentPrincipalName = authentication.getName();
        return userRepository.findByLogin(currentPrincipalName);
    }

    /**
     * Shows a list of the current user's information items.
     * Allows filtering by category/date, searching, and sorting.
     */
    @GetMapping("/list") // Handles requests to view the information list.
    public String listInformation(
            @RequestParam(value = "sortBy", required = false, defaultValue = "dateAdded") String sortBy, // How to sort.
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection, // Sort direction.
            @RequestParam(value = "categoryId", required = false) Long categoryId, // Filter by category.
            @RequestParam(value = "startDate", required = false) LocalDate startDate, // Filter by start date.
            @RequestParam(value = "endDate", required = false) LocalDate endDate, // Filter by end date.
            @RequestParam(value = "search", required = false) String searchKeyword, // Search text.
            Model model) { // Basket for data to show on the page.

        User currentUser = getCurrentUser(); // Get the logged-in user.
        if (currentUser == null) { // If not logged in:
            return "redirect:/login"; // Go to login page.
        }

        List<Information> informationItemsFromDb; // Start with items from database.

        // --- Apply Filters (Category and Date Range) ---
        // Checks if filters are active and fetches items from the database accordingly.
        // Also handles errors like start date being after end date, or category not found.
        if (categoryId != null && categoryId > 0) {
            Optional<Category> category = categoryRepository.findById(categoryId);
            if (category.isPresent()) {
                if (startDate != null && endDate != null) {
                    if (startDate.isAfter(endDate)) {
                        model.addAttribute("dateError", "Start date cannot be after end date.");
                        informationItemsFromDb = informationRepository.findByUserAndCategory(currentUser, category.get());
                    } else {
                        informationItemsFromDb = informationRepository.findByUserAndCategoryAndDateAddedBetween(currentUser, category.get(), startDate, endDate);
                    }
                } else {
                    informationItemsFromDb = informationRepository.findByUserAndCategory(currentUser, category.get());
                }
            } else {
                model.addAttribute("categoryError", "Selected category not found.");
                if (startDate != null && endDate != null) {
                    if (startDate.isAfter(endDate)) {
                        model.addAttribute("dateError", "Start date cannot be after end date.");
                        informationItemsFromDb = informationRepository.findByUser(currentUser);
                    } else {
                        informationItemsFromDb = informationRepository.findByUserAndDateAddedBetween(currentUser, startDate, endDate);
                    }
                } else {
                    informationItemsFromDb = informationRepository.findByUser(currentUser);
                }
            }
        } else if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                model.addAttribute("dateError", "Start date cannot be after end date.");
                informationItemsFromDb = informationRepository.findByUser(currentUser);
            } else {
                informationItemsFromDb = informationRepository.findByUserAndDateAddedBetween(currentUser, startDate, endDate);
            }
        } else {
            informationItemsFromDb = informationRepository.findByUser(currentUser); // Get all user's info by default.
        }

        // --- Mix in Unsaved Changes from the Current Session ---
        // This combines database items with any new, edited, or deleted items from the current session.
        List<Information> informationItems = new ArrayList<>();
        Set<Long> handledDbIds = new HashSet<>();

        // Add/update items that were changed or are new in the current session.
        for (Information dbInfo : informationItemsFromDb) {
            if (sessionDataStore.getModifiedInformationMap().containsKey(dbInfo.getId())) {
                informationItems.add(sessionDataStore.getModifiedInformationMap().get(dbInfo.getId()));
                handledDbIds.add(dbInfo.getId());
            } else if (!sessionDataStore.getDeletedInformationIds().contains(dbInfo.getId())) {
                informationItems.add(dbInfo);
                handledDbIds.add(dbInfo.getId());
            }
        }
        for (Information newInfo : sessionDataStore.getNewInformationMap().values()) {
            if (!sessionDataStore.getDeletedInformationIds().contains(newInfo.getId())) {
                informationItems.add(newInfo);
            }
        }

        // --- Apply Search (to the mixed list) ---
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            final String lowerCaseSearchKeyword = searchKeyword.toLowerCase();
            informationItems = informationItems.stream()
                    .filter(info -> (info.getTitle() != null && info.getTitle().toLowerCase().contains(lowerCaseSearchKeyword)) ||
                            (info.getContent() != null && info.getContent().toLowerCase().contains(lowerCaseSearchKeyword)))
                    .collect(Collectors.toList());
        }

        // --- Apply Sorting (to the final list) ---
        if ("dateAdded".equals(sortBy)) {
            if ("asc".equals(sortDirection)) { // Sort oldest first.
                informationItems.sort((i1, i2) -> {
                    if (i1.getDateAdded() == null && i2.getDateAdded() == null) return 0;
                    if (i1.getDateAdded() == null) return -1;
                    if (i2.getDateAdded() == null) return 1;
                    return i1.getDateAdded().compareTo(i2.getDateAdded());
                });
            } else { // Sort newest first.
                informationItems.sort((i1, i2) -> {
                    if (i1.getDateAdded() == null && i2.getDateAdded() == null) return 0;
                    if (i1.getDateAdded() == null) return 1;
                    if (i2.getDateAdded() == null) return -1;
                    return i2.getDateAdded().compareTo(i1.getDateAdded());
                });
            }
        } else if ("name".equals(sortBy)) { // Sort by category name.
            informationItems.sort((i1, i2) -> {
                String name1 = (i1.getCategory() != null) ? i1.getCategory().getName() : "";
                String name2 = (i2.getCategory() != null) ? i2.getCategory().getName() : "";
                return "asc".equals(sortDirection) ? name1.compareToIgnoreCase(name2) : name2.compareToIgnoreCase(name1);
            });
        }

        model.addAttribute("informationItems", informationItems); // Put final info list in basket.
        model.addAttribute("categories", categoryRepository.findAll()); // Put all categories in basket for dropdowns.

        // Pass back current filter/sort/search choices to keep them selected in the web page forms.
        model.addAttribute("selectedSortBy", sortBy);
        model.addAttribute("selectedSortDirection", sortDirection);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
        model.addAttribute("selectedSearchKeyword", searchKeyword);

        return "information/list"; // Show the list web page.
    }

    /**
     * Shows the form to add a new information item.
     */
    @GetMapping("/add") // Handles requests to open the add form.
    public String showAddInformationForm(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login"; // Go to login if not logged in.
        }
        model.addAttribute("newInformation", new Information()); // Put an empty info form in basket.
        model.addAttribute("categories", categoryRepository.findAll()); // Put all categories for dropdown.
        return "information/add"; // Show the add web page.
    }

    /**
     * Processes the form when a user adds new information.
     */
    @PostMapping("/add") // Handles form submissions (POST) for adding info.
    public String addInformation(@Valid Information newInformation, BindingResult result, Model model) {
        User currentUser = getCurrentUser();
        System.out.println("Attempting to add Information: " + (currentUser != null ? currentUser.getLogin() : "NULL"));

        if (currentUser == null) { // If user not logged in:
            System.out.println("Redirecting to login because current user is NULL.");
            return "redirect:/login"; // Go to login.
        }

        if (result.hasErrors()) { // If there are input errors:
            System.out.println("Validation errors found for Information:");
            result.getAllErrors().forEach(error -> System.out.println(" - " + error.getDefaultMessage() + " (Field: " + error.getObjectName() + "." + (error.getCodes() != null && error.getCodes().length > 0 ? error.getCodes()[0].split("\\.")[error.getCodes()[0].split("\\.").length - 1] : "N/A") + ")"));

            model.addAttribute("newInformation", newInformation); // Put back entered info.
            model.addAttribute("categories", categoryRepository.findAll()); // Put categories for dropdown.
            return "information/add"; // Show add page again with errors.
        }

        newInformation.setDateAdded(LocalDate.now()); // Set today's date.
        newInformation.setUser(currentUser); // Link info to the current user.

        sessionDataStore.addNewInformation(newInformation); // Add to temporary storage (session).
        System.out.println("Information added to session: " + newInformation.getTitle());
        return "redirect:/information/list?success=added_to_session"; // Go to list page with success message.
    }

    /**
     * Shows the form to edit an existing information item.
     */
    @GetMapping("/edit/{id}") // Handles requests to open the edit form.
    public String showEditInformationForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login"; // Go to login if not logged in.
        }

        // Try to get the item from temporary storage, then from database.
        Information informationToEdit = sessionDataStore.getModifiedInformationMap().get(id);
        if (informationToEdit == null) {
            Optional<Information> informationOptional = informationRepository.findById(id);
            if (informationOptional.isEmpty()) {
                // Check if it's a new item still in session
                informationToEdit = sessionDataStore.getNewInformationMap().values().stream()
                        .filter(info -> info.getId() != null && info.getId().equals(id))
                        .findFirst().orElse(null);
                if(informationToEdit == null){
                    return "redirect:/information/list?error=notfound"; // Go to list if not found.
                }
            } else {
                informationToEdit = informationOptional.get();
            }
        }

        // Check if the current user owns this item (security).
        if (!informationToEdit.getUser().equals(currentUser)) {
            System.out.println("Unauthorized attempt to edit information ID: " + id + " by user: " + currentUser.getLogin());
            return "redirect:/information/list?error=not_authorized"; // Go to list with error.
        }

        model.addAttribute("information", informationToEdit); // Put info item in basket.
        model.addAttribute("categories", categoryRepository.findAll()); // Put all categories for dropdown.
        return "information/edit"; // Show the edit web page.
    }

    /**
     * Processes the form when a user updates an information item.
     */
    @PostMapping("/edit/{id}") // Handles form submissions (POST) for editing info.
    public String updateInformation(@PathVariable Long id, @Valid Information updatedInformation, BindingResult result, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login"; // Go to login if not logged in.
        }

        // Try to get the existing item from temporary storage, then from database.
        Information existingInformation = sessionDataStore.getModifiedInformationMap().get(id);
        if (existingInformation == null) {
            Optional<Information> existingInformationOptional = informationRepository.findById(id);
            if (existingInformationOptional.isEmpty()) {
                // Check if it's a new item still in session
                existingInformation = sessionDataStore.getNewInformationMap().values().stream()
                        .filter(info -> info.getId() != null && info.getId().equals(id))
                        .findFirst().orElse(null);
                if(existingInformation == null){
                    System.out.println("Information with ID " + id + " not found for update.");
                    return "redirect:/information/list?error=notfound"; // Go to list with error.
                }
            } else {
                existingInformation = existingInformationOptional.get();
            }
        }

        // Check if the current user owns this item (security).
        if (!existingInformation.getUser().equals(currentUser)) {
            System.out.println("Unauthorized attempt to update information ID: " + id + " by user: " + currentUser.getLogin());
            return "redirect:/information/list?error=not_authorized"; // Go to list with error.
        }

        if (result.hasErrors()) { // If there are input errors:
            System.out.println("Validation errors found for Information update:");
            result.getAllErrors().forEach(error -> System.out.println(" - " + error.getDefaultMessage() + " (Field: " + error.getObjectName() + "." + (error.getCodes() != null && error.getCodes().length > 0 ? error.getCodes()[0].split("\\.")[error.getCodes()[0].split("\\.").length - 1] : "N/A") + ")"));
            model.addAttribute("information", updatedInformation); // Put back entered info.
            model.addAttribute("categories", categoryRepository.findAll()); // Put categories for dropdown.
            return "information/edit"; // Show edit page again with errors.
        }

        // Update the item's details with new info from the form.
        existingInformation.setTitle(updatedInformation.getTitle());
        existingInformation.setContent(updatedInformation.getContent());
        existingInformation.setLink(updatedInformation.getLink());
        existingInformation.setCategory(updatedInformation.getCategory());

        sessionDataStore.updateExistingInformation(existingInformation); // Update in temporary storage.
        System.out.println("Information updated in session: " + existingInformation.getTitle());
        return "redirect:/information/list?success=updated_in_session"; // Go to list page with success message.
    }

    /**
     * Processes the request to delete an information item.
     */
    @PostMapping("/delete/{id}") // Handles form submissions (POST) for deleting info.
    public String deleteInformation(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login"; // Go to login if not logged in.
        }

        // Try to find the item in the database first.
        Optional<Information> informationOptional = informationRepository.findById(id);
        Information informationToDelete = informationOptional.orElse(null);

        // If not found in DB, check if it's a new item still in temporary storage.
        if(informationToDelete == null){
            informationToDelete = sessionDataStore.getNewInformationMap().values().stream()
                    .filter(info -> info.getId() != null && info.getId().equals(id))
                    .findFirst().orElse(null);
        }

        if (informationToDelete == null) { // If item not found at all:
            System.out.println("Information with ID " + id + " not found for deletion.");
            return "redirect:/information/list?error=notfound"; // Go to list with error.
        }

        // Check if the current user owns this item (security).
        if (!informationToDelete.getUser().equals(currentUser)) {
            System.out.println("Unauthorized attempt to delete information ID: " + id + " by user: " + currentUser.getLogin());
            return "redirect:/information/list?error=not_authorized"; // Go to list with error.
        }

        sessionDataStore.markForDeletion(id); // Mark for deletion in temporary storage.
        System.out.println("Information marked for deletion in session: " + informationToDelete.getTitle());
        return "redirect:/information/list?success=marked_for_deletion"; // Go to list page with success message.
    }


    /**
     * Shows the form to share an information item with other users.
     */
    @GetMapping("/share/{id}") // Handles requests to open the share form.
    public String showShareInformationForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login"; // Go to login if not logged in.
        }

        Information informationToShare = findInformationToShare(id); // Find the item to share.
        if (informationToShare == null) { // If item not found:
            return "redirect:/information/list?error=notfound"; // Go to list with error.
        }

        // Security check: Only the item's owner can share it.
        if (!informationToShare.getUser().equals(currentUser)) {
            System.out.println("Unauthorized attempt to share information ID: " + id + " by user: " + currentUser.getLogin());
            return "redirect:/information/list?error=not_authorized"; // Go to list with error.
        }

        // Get all other users for selecting who to share with.
        List<User> allUsers = userRepository.findAll();
        // Filter out current user and already shared users.
        List<User> availableUsers = allUsers.stream()
                .filter(user -> !user.equals(currentUser) && !informationToShare.getSharedWithUsers().contains(user))
                .collect(Collectors.toList());

        model.addAttribute("information", informationToShare); // Put info item in basket.
        model.addAttribute("availableUsers", availableUsers); // Put users available for sharing.
        model.addAttribute("currentSharedUsers", informationToShare.getSharedWithUsers()); // Put currently shared users.

        // Generate a special shareable link if one doesn't exist yet for this item.
        if (informationToShare.getShareableToken() == null || informationToShare.getShareableToken().isEmpty()) {
            informationToShare.generateShareableToken();
            sessionDataStore.updateExistingInformation(informationToShare); // Save token to temporary storage.
        }

        // Create the full shareable link to display on the page.
        String shareLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/information/sharedlink/")
                .path(informationToShare.getShareableToken())
                .toUriString();
        model.addAttribute("shareLink", shareLink); // Put the link in the basket.

        return "information/share"; // Show the share web page.
    }

    /**
     * Helper to find an information item from temporary storage or database.
     * @param id The ID of the information item.
     * @return The found information item, or null.
     */
    private Information findInformationToShare(Long id) {
        Information info = sessionDataStore.getModifiedInformationMap().get(id); // Check modified first.
        if (info != null) return info; // If found, return it.

        Optional<Information> optional = informationRepository.findById(id); // Check database.
        if (optional.isPresent()) return optional.get(); // If found, return it.

        // Finally, check newly added items in temporary storage.
        return sessionDataStore.getNewInformationMap().values().stream()
                .filter(i -> i.getId() != null && i.getId().equals(id))
                .findFirst().orElse(null);
    }

    /**
     * Processes the form when a user updates who an information item is shared with.
     */
    @PostMapping("/share/{id}") // Handles form submissions (POST) for sharing.
    public String shareInformation(@PathVariable Long id,
                                   @RequestParam(value = "userIdsToAdd", required = false) List<Long> userIdsToAdd, // Users to add for sharing.
                                   @RequestParam(value = "userIdsToRemove", required = false) List<Long> userIdsToRemove) { // Users to remove from sharing.
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login"; // Go to login if not logged in.
        }

        // Try to get the item from temporary storage, then from database.
        Information informationToUpdate = sessionDataStore.getModifiedInformationMap().get(id);
        if (informationToUpdate == null) {
            Optional<Information> informationOptional = informationRepository.findById(id);
            if (informationOptional.isEmpty()) {
                informationToUpdate = sessionDataStore.getNewInformationMap().values().stream()
                        .filter(info -> info.getId() != null && info.getId().equals(id))
                        .findFirst().orElse(null);
                if(informationToUpdate == null){
                    return "redirect:/information/list?error=notfound"; // Go to list with error.
                }
            } else {
                informationToUpdate = informationOptional.get();
            }
        }

        // Security check: Only the item's owner can change sharing settings.
        if (!informationToUpdate.getUser().equals(currentUser)) {
            System.out.println("Unauthorized attempt to modify sharing for information ID: " + id + " by user: " + currentUser.getLogin());
            return "redirect:/information/list?error=not_authorized"; // Go to list with error.
        }

        // Add users chosen for sharing.
        if (userIdsToAdd != null && !userIdsToAdd.isEmpty()) {
            List<User> usersToAdd = userRepository.findAllById(userIdsToAdd);
            for (User user : usersToAdd) {
                if (!user.equals(currentUser)) { // Don't share with self.
                    informationToUpdate.addSharedUser(user);
                }
            }
        }

        // Remove users chosen from sharing.
        if (userIdsToRemove != null && !userIdsToRemove.isEmpty()) {
            List<User> usersToRemove = userRepository.findAllById(userIdsToRemove);
            for (User user : usersToRemove) {
                informationToUpdate.removeSharedUser(user);
            }
        }

        // Generate a shareable token if one doesn't exist (in case it's newly shared).
        if (informationToUpdate.getShareableToken() == null || informationToUpdate.getShareableToken().isEmpty()) {
            informationToUpdate.generateShareableToken();
        }

        sessionDataStore.updateExistingInformation(informationToUpdate); // Update in temporary storage.
        System.out.println("Information ID: " + id + " sharing updated in session by user: " + currentUser.getLogin());
        return "redirect:/information/list?success=sharing_updated_in_session"; // Go to list with success message.
    }

    /**
     * Shows a list of information items that have been shared WITH the current user.
     * Also allows filtering, searching, and sorting.
     */
    @GetMapping("/shared") // Handles requests to view shared info list.
    public String listSharedInformation(
            @RequestParam(value = "sortBy", required = false, defaultValue = "dateAdded") String sortBy,
            @RequestParam(value = "sortDirection", required = false, defaultValue = "desc") String sortDirection,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "search", required = false) String searchKeyword,
            Model model) {

        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login"; // Go to login if not logged in.
        }

        // Get all information items shared with the current user from the database.
        List<Information> sharedInformationItems = informationRepository.findBySharedWithUsersContains(currentUser);

        // --- Apply Filters (Category and Date Range) ---
        // Filters items from the shared list.
        if (categoryId != null && categoryId > 0) {
            Optional<Category> category = categoryRepository.findById(categoryId);
            if (category.isPresent()) {
                final Category filterCategory = category.get();
                sharedInformationItems = sharedInformationItems.stream()
                        .filter(info -> info.getCategory() != null && info.getCategory().equals(filterCategory))
                        .collect(Collectors.toList());
            } else {
                model.addAttribute("categoryError", "Selected category not found.");
            }
        }

        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                model.addAttribute("dateError", "Start date cannot be after end date.");
            } else {
                final LocalDate finalStartDate = startDate;
                final LocalDate finalEndDate = endDate;
                sharedInformationItems = sharedInformationItems.stream()
                        .filter(info -> info.getDateAdded() != null && !info.getDateAdded().isBefore(finalStartDate) && !info.getDateAdded().isAfter(finalEndDate))
                        .collect(Collectors.toList());
            }
        }

        // --- Apply Search (to the filtered list) ---
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            final String lowerCaseSearchKeyword = searchKeyword.toLowerCase();
            sharedInformationItems = sharedInformationItems.stream()
                    .filter(info -> (info.getTitle() != null && info.getTitle().toLowerCase().contains(lowerCaseSearchKeyword)) ||
                            (info.getContent() != null && info.getContent().toLowerCase().contains(lowerCaseSearchKeyword)))
                    .collect(Collectors.toList());
        }

        // --- Apply Sorting (to the final list) ---
        if ("dateAdded".equals(sortBy)) {
            if ("asc".equals(sortDirection)) { // Sort oldest first.
                sharedInformationItems.sort((i1, i2) -> {
                    if (i1.getDateAdded() == null && i2.getDateAdded() == null) return 0;
                    if (i1.getDateAdded() == null) return -1;
                    if (i2.getDateAdded() == null) return 1;
                    return i1.getDateAdded().compareTo(i2.getDateAdded());
                });
            } else { // Sort newest first.
                sharedInformationItems.sort((i1, i2) -> {
                    if (i1.getDateAdded() == null && i2.getDateAdded() == null) return 0;
                    if (i1.getDateAdded() == null) return 1;
                    if (i2.getDateAdded() == null) return -1;
                    return i2.getDateAdded().compareTo(i1.getDateAdded());
                });
            }
        } else if ("name".equals(sortBy)) { // Sort by category name.
            sharedInformationItems.sort((i1, i2) -> {
                String name1 = (i1.getCategory() != null) ? i1.getCategory().getName() : "";
                String name2 = (i2.getCategory() != null) ? i2.getCategory().getName() : "";
                return "asc".equals(sortDirection) ? name1.compareToIgnoreCase(name2) : name2.compareToIgnoreCase(name1);
            });
        }

        model.addAttribute("sharedInformationItems", sharedInformationItems); // Put shared info list in basket.
        model.addAttribute("categories", categoryRepository.findAll()); // Put all categories for dropdowns.

        // Pass back current filter/sort/search choices to keep them selected in the web page forms.
        model.addAttribute("selectedSortBy", sortBy);
        model.addAttribute("selectedSortDirection", sortDirection);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
        model.addAttribute("selectedSearchKeyword", searchKeyword);

        return "information/shared_list"; // Show the shared list web page.
    }

    /**
     * Handles when a user clicks a shared link. Adds the shared item to their list.
     *
     * @return Redirects to the shared list page.
     */
    @GetMapping("/sharedlink/{token}") // Handles requests from shared links.
    public String accessSharedInformation(@PathVariable String token, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpSession session) {
        User currentUser = getCurrentUser();
        if (currentUser == null) { // If not logged in:
            redirectAttributes.addFlashAttribute("message", "Please log in to view this shared information.");
            return "redirect:/login"; // Go to login page.
        }

        // Find the information item using the unique shareable token.
        Optional<Information> informationOptional = informationRepository.findByShareableToken(token);
        if (informationOptional.isEmpty()) { // If item not found for token:
            redirectAttributes.addFlashAttribute("error", "Shared information not found or link is invalid.");
            return "redirect:/information/shared"; // Go to shared list with error.
        }

        Information informationToShare = informationOptional.get();

        // If the user doesn't own it and it's not already shared with them, add them to shared list.
        if (!informationToShare.getSharedWithUsers().contains(currentUser) && !informationToShare.getUser().equals(currentUser)) {
            informationToShare.addSharedUser(currentUser);
            informationRepository.save(informationToShare); // Save changes to database.
            System.out.println("Information '" + informationToShare.getTitle() + "' successfully shared via link with user: " + currentUser.getLogin());
        } else {
            System.out.println("Information '" + informationToShare.getTitle() + "' already shared with user: " + currentUser.getLogin() + " or user is the owner.");
        }

        redirectAttributes.addFlashAttribute("success", "Shared information added to your list."); // Send success message.
        return "redirect:/information/shared"; // Go to the shared list page.
    }
}
