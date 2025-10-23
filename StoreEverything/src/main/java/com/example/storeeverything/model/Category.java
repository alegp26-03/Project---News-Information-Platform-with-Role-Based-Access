package com.example.storeeverything.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern; // Import this

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Category name is required")
    @Size(min = 3, max = 20, message = "Category name must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-z]{3,20}$", message = "Category name must contain only lowercase letters and be between 3 and 20 characters")
    private String name;

    public Category(String name) {
        this.name = name;
    }
}