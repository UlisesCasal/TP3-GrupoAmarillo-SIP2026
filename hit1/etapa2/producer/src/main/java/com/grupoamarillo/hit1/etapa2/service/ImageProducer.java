package  com.grupoamarillo.hit1.etapa2.service;

import java.awt.image.BufferedImage;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.grupoamarillo.hit1.etapa2.dto.ImageChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ImageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final Logger log = LoggerFactory.getLogger(ImageProducer.class);

  
    public void processAndSend(MultipartFile file, int numChunks, String jobId) throws IOException {
        log.info("Recive image");

        BufferedImage original = ImageIO.read(file.getInputStream());
        int chunkHeight = original.getHeight() / numChunks;
        // Si el chunkHeight es menor a 4, se genera 1 solo mensaje
        if (chunkHeight < 4){
            log.warn("Assign chunkHeight to 1, minimus kernel");
            chunkHeight = 1;
        }
        String contentType =file.getContentType(); 
        String format = contentType.substring(contentType.indexOf("/")+1);
        ObjectMapper objectMapper = new ObjectMapper();

        for (int i = 0; i < numChunks; i++) {
            int y = i * chunkHeight;
            int height = (i == numChunks - 1) ? (original.getHeight() - y) : chunkHeight;
            
           

            // --- Lógica del Halo ---
            int haloTop = (i == 0) ? 0 : 1; 
            int haloBottom = (i == numChunks - 1) ? 0 : 1;

            // Capturamos la subimagen incluyendo el halo
            BufferedImage subImage = original.getSubimage(
                0, 
                y - haloTop, 
                original.getWidth(), 
                height + haloTop + haloBottom
        );
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(subImage, format, baos);
            
            ImageChunk chunk = new ImageChunk(jobId, baos.toByteArray(), i, y, format, numChunks ,haloTop, haloBottom);
            if(chunk.getData().length == 0){
                log.error("Not data JobId {} in chunk {}, not generate message",jobId,i);
                System.err.println("Not data in chunk, not generate message");
                return;}
            String menssage = objectMapper.writeValueAsString(chunk);

            rabbitTemplate.convertAndSend("image_exchange", "to_worker", menssage);
        }
        log.info("All chunks queue");
    }
}