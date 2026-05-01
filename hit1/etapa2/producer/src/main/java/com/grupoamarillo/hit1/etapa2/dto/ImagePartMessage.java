package com.grupoamarillo.hit1.etapa2.dto;

import java.io.Serializable;

public class ImagePartMessage implements Serializable {
    private String jobId;
    private int sequenceNumber;
    private int totalParts;
    private byte[] imageData;
    private int width;

    // Constructor vacío para Jackson
    public ImagePartMessage() {}

    public ImagePartMessage(String jobId, int sequenceNumber, int totalParts, byte[] imageData, int width) {
        this.jobId = jobId;
        this.sequenceNumber = sequenceNumber;
        this.totalParts = totalParts;
        this.imageData = imageData;
        this.width = width;
    }

    // Getters y Setters
    public String getJobId() { return jobId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public int getTotalParts() { return totalParts; }
    public byte[] getImageData() { return imageData; }
    public int getWidth() { return width; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
}