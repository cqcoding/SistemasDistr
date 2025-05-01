package com.example.ProjetoSD;

public class Project {
    private String name;
    private String description;
    private String manager;
    private String status;

    public Project() {
    }

    public Project(String name, String description, String manager, String status) {
        this.name = name;
        this.description = description;
        this.manager = manager;
        this.status = status;
    }

    // Getters e Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
} 