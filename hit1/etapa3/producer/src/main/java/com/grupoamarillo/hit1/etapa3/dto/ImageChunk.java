package com.grupoamarillo.hit1.etapa3.dto;

import java.io.Serializable;

public class ImageChunk implements Serializable {
    private String jobId;
    private byte[] data; //imgen
    private int chunkId; // seq
    private int offset; // y
    private int totalChunks;
    private String format; // "jpg" o "png"
    private int haloTop;
    private int haloBottom;

    public ImageChunk() {}

    public ImageChunk(String jobId, byte[] data, int chunkId, int offset, String format, int totalChunks, int haloTop, int haloBottom) {
        this.jobId = jobId;
        this.data = data;
        this.chunkId = chunkId;
        this.offset = offset;
        this.format = format;
        this.totalChunks = totalChunks;
        this.haloBottom = haloBottom;
        this.haloTop = haloTop;

    }

    // Getters y Setters
    public String getJobId() { return jobId; }
    public int getChunkId() { return chunkId; }
    public int getTotalChunks() { return totalChunks; }
    public byte[] getData() { return data; }
    public int getOffset() { return offset; }
    public String getFormat(){return format; }
    public int getHaloTop() {return haloTop;}
    public int getHaloBottom() {return haloBottom;}
    public void setData(byte[] data) { this.data = data; }
    public void setHaloTop(int haloTop) { this.haloTop= haloTop;}
    public void setHaloBottom(int haloBottom) {this.haloBottom = haloBottom;}
}