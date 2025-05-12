package com.api;

public class SearchResult {
    private final String title;
    private final String url;
    private final String citation;

    public SearchResult(String title, String url, String citation) {
        this.title = title;
        this.url = url;
        this.citation = citation;
    }

    // Getters
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getCitation() { return citation; }
}