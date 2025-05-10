package com.api;

import java.util.List;

public class ChatRequest {
    private String termo;
    private List<String> trechos;

    // Getters e Setters
    public String getTermo() {
        return termo;
    }

    public void setTermo(String termo) {
        this.termo = termo;
    }

    public List<String> getTrechos() {
        return trechos;
    }

    public void setTrechos(List<String> trechos) {
        this.trechos = trechos;
    }
}