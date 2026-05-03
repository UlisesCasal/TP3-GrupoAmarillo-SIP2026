package com.grupoamarillo.hit1.etapa3.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.rabbitmq.client.Channel;

import org.springframework.amqp.core.Message;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import java.io.IOException;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupoamarillo.hit1.etapa3.dto.ImageChunk;
import com.grupoamarillo.hit1.etapa3.dto.ResultChunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class SobelWorker {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Value("${consumer.delay:2000}")
    private long delayMs;

    private static final Logger log = LoggerFactory.getLogger(SobelWorker.class);


    @RabbitListener(queues = "sobel.work_queue", containerFactory = "manualAckFactory")
    public void processChunk(Message msg, Channel channel) throws IOException {
        long tag = msg.getMessageProperties().getDeliveryTag();
        ObjectMapper mapper = new ObjectMapper();
        ImageChunk chunk = mapper.readValue(msg.getBody(),ImageChunk.class);
        log.info("Apply Sobel to jobID {} chunk {}/{}",chunk.getJobId(), chunk.getChunkId(),chunk.getTotalChunks());
        
        try{
            // 1. Decodificar la imagen recibida
            ByteArrayInputStream bais = new ByteArrayInputStream(chunk.getData());
            BufferedImage image = ImageIO.read(bais);
            
            // 2. Aplicar el filtro Sobel
            BufferedImage resultImage = applySobel(image);

            // Eliminar Halo
            int width = resultImage.getWidth();
            int realHeight = resultImage.getHeight() - chunk.getHaloTop() - chunk.getHaloBottom();
           
            BufferedImage finalChunk = resultImage.getSubimage(0, chunk.getHaloTop(), width, realHeight);
            // 3. Convertir resultado a bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(finalChunk, chunk.getFormat(), baos);
            
            // 4. Crear el DTO de salida preservando los metadatos de rastreo
            ResultChunk result = new ResultChunk();
            result.setJobId(chunk.getJobId()); // Crucial para el Joiner
            result.setData(baos.toByteArray());
            result.setChunkId(chunk.getChunkId());
            result.setTotalChunks(chunk.getTotalChunks());
            result.setFormat(chunk.getFormat());

            log.debug("PROCESANDO");
            Thread.sleep(delayMs); 
            
            // 5. Enviar a la cola de resultados
            String message = mapper.writeValueAsString(result);
            rabbitTemplate.convertAndSend("image_exchange", "to_aggregator", message);
            log.info("Correct apply Sobel to jobID {} chunk {}/{}",chunk.getJobId(), chunk.getChunkId(),chunk.getTotalChunks());
            channel.basicAck(tag, false);
         } catch (Exception e){
            System.err.println("Error en proceso de imagen Sobel");
            log.warn("Error to apply Sobel to jobID {} chunk {}/{} \n Error {}",chunk.getJobId(), chunk.getChunkId(),chunk.getTotalChunks(),e);
            Integer deliveryCount = (Integer) msg.getMessageProperties().getHeaders().getOrDefault("x-delivery-count", 0);
            
            if (deliveryCount <= 4){
                log.info("Retry {}", deliveryCount);
                channel.basicNack(tag, false,true);
            }
            log.error("Not posibble apply Sobel");
            channel.basicNack(tag, true, false);
        }
    }

    private BufferedImage applySobel(BufferedImage src) {
        int width = src.getWidth();
        int height = src.getHeight();
        BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                int p00 = getGray(src.getRGB(x - 1, y - 1));
                int p01 = getGray(src.getRGB(x, y - 1));
                int p02 = getGray(src.getRGB(x + 1, y - 1));
                int p10 = getGray(src.getRGB(x - 1, y));
                int p12 = getGray(src.getRGB(x + 1, y));
                int p20 = getGray(src.getRGB(x - 1, y + 1));
                int p21 = getGray(src.getRGB(x, y + 1));
                int p22 = getGray(src.getRGB(x + 1, y + 1));

                int gx = (p02 + 2 * p12 + p22) - (p00 + 2 * p10 + p20);
                int gy = (p00 + 2 * p01 + p02) - (p20 + 2 * p21 + p22);
                
                int magnitude = (int) Math.sqrt(gx * gx + gy * gy);
                magnitude = Math.min(255, magnitude);
                
                int rgb = (magnitude << 16) | (magnitude << 8) | magnitude;
                dest.setRGB(x, y, rgb);
            }
        }
        return dest;
    }

    private int getGray(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }
}