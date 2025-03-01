package com.example.myapplication;

import java.util.List;

public class SourcesResponse {
    private String status;
    private List<Source> sources;

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }
}


