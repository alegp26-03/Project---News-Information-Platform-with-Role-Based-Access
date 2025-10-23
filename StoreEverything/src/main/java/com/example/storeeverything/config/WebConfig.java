package com.example.storeeverything.config;

import com.example.storeeverything.listener.SessionSavingListener;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This file helps configure special web settings for our application.
 * Specifically, it sets up something to listen for when user sessions start and end.
 */
@Configuration // Tells Spring: "This file is for setting up various parts of the web application."
public class WebConfig {

    /**
     * This method registers a "listener" that watches for user sessions.
     * It makes sure our `SessionSavingListener` gets activated when a user starts or leaves the app.
     *
     * @return A registration object for our session listener.
     */
    @Bean // Spring creates and manages this listener setup.
    public ServletListenerRegistrationBean<SessionSavingListener> sessionListener() {
        // This is a special Spring tool to register a web listener.
        ServletListenerRegistrationBean<SessionSavingListener> listenerRegBean = new ServletListenerRegistrationBean<>();
        // We tell it which listener to use: our SessionSavingListener.
        listenerRegBean.setListener(new SessionSavingListener());
        // Return the setup for the listener.
        return listenerRegBean;
    }
}
