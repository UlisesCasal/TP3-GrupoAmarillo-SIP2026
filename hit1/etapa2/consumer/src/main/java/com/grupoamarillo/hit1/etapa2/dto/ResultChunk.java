package com.grupoamarillo.hit1.etapa2.dto;

import java.io.Serializable;

public class ResultChunk implements Serializable {
    private String jobId;
    private byte[] data; //imgen
    private int chunkId; // seq
    private int offset; // y
    private int totalChunks;
    private String format; // "jpg" o "png"

    public ResultChunk() {}

    public ResultChunk(String jobId, byte[] data, int chunkId, int offset, String format, int totalChunks) {
        this.jobId = jobId;
        this.data = data;
        this.chunkId = chunkId;
        this.offset = offset;
        this.format = format;
        this.totalChunks = totalChunks;

    }

    // Getters y Setters
    public String getJobId() { return jobId; }
    public int getChunkId() { return chunkId; }
    public int getTotalChunks() { return totalChunks; }
    public byte[] getData() { return data; }
    public int getOffset() { return offset; }
    public String getFormat(){return format; }

    public void setData(byte[] data) { this.data = data; }
    public String setJobId(String jobId) { return this.jobId= jobId; } 
    public int setChunkId(int chunkId) { return this.chunkId= chunkId; }
    public int setTotalChunks(int totalChunks) { return this.totalChunks= totalChunks; }
    public int setOffset(int offset) { return this.offset=offset; }
    public String setFormat(String format){return this.format=format; }


}