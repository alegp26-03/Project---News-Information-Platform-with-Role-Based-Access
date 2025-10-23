package com.example.storeeverything.listener;

import com.example.storeeverything.model.Information;
import com.example.storeeverything.repository.InformationRepository;
import com.example.storeeverything.session.SessionDataStore;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.transaction.annotation.Transactional; // For database transactions
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Saves unsaved user data to the database when a web session ends.
 */
@WebListener // Marks this as a web listener.
public class SessionSavingListener implements HttpSessionListener {

    // Note: Can't use @Autowired directly here; tools are fetched specially.

    /**
     * Runs when a user's web session closes. Saves unsaved data.
     *
     * @param se Info about the session that ended.
     */
    @Override // Custom handling for session end.
    @Transactional // All database actions happen as one step.
    public void sessionDestroyed(HttpSessionEvent se) {
        System.out.println("Session destroyed: " + se.getSession().getId() + ". Attempting to save unsaved data.");

        // Get Spring's main setup to access tools.
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(se.getSession().getServletContext());

        if (ctx == null) { // If setup not found:
            System.err.println("Spring ApplicationContext not found in SessionSavingListener. Cannot save data.");
            return; // Stop.
        }

        // Get database and temporary storage tools.
        InformationRepository informationRepository = ctx.getBean(InformationRepository.class);
        SessionDataStore sessionDataStore = null;

        try {
            // Get temporary storage for this session.
            sessionDataStore = (SessionDataStore) se.getSession().getAttribute("scopedTarget.sessionDataStore");
        } catch (Exception e) {
            System.err.println("Error retrieving SessionDataStore from session. Data might be lost: " + e.getMessage());
            e.printStackTrace(); // Show error.
            return; // Stop.
        }


        if (sessionDataStore != null) { // If temporary storage found:
            try {
                // 1. Delete marked items.
                if (!sessionDataStore.getDeletedInformationIds().isEmpty()) {
                    informationRepository.deleteAllById(sessionDataStore.getDeletedInformationIds());
                    System.out.println("Successfully deleted " + sessionDataStore.getDeletedInformationIds().size() + " information items.");
                }

                // 2. Save new items.
                if (!sessionDataStore.getNewInformationMap().isEmpty()) {
                    List<Information> newItems = new ArrayList<>(sessionDataStore.getNewInformationMap().values());
                    // Important: Clear ID for new items for database auto-generation.
                    newItems.forEach(info -> info.setId(null));
                    informationRepository.saveAll(newItems); // Save new.
                    System.out.println("Successfully saved " + newItems.size() + " new information items.");
                }

                // 3. Update modified items.
                if (!sessionDataStore.getModifiedInformationMap().isEmpty()) {
                    informationRepository.saveAll(sessionDataStore.getModifiedInformationMap().values()); // Save modified.
                    System.out.println("Successfully updated " + sessionDataStore.getModifiedInformationMap().size() + " information items.");
                }

            } catch (Exception e) { // If saving fails:
                System.err.println("CRITICAL ERROR: Failed to save data on session destruction. Data loss likely!");
                e.printStackTrace(); // Print error.
            } finally {
                // Always clear temporary data after attempting save.
                sessionDataStore.clearChanges();
            }
        } else {
            System.out.println("No SessionDataStore found for this session. No data to save.");
        }
    }
}
