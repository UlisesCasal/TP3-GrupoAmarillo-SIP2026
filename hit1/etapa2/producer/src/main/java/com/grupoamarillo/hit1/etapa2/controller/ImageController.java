package  com.grupoamarillo.hit1.etapa2.controller;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupoamarillo.hit1.etapa2.dto.ImagePartMessage;

@RestController
@RequestMapping("/image")
public class ImageController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/sobel")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file, @RequestParam(name="parts", defaultValue="10") int parts) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        BufferedImage original = ImageIO.read(file.getInputStream());
        String jobId = UUID.randomUUID().toString();
        int chunkHeight = original.getHeight() / parts;
        
        for (int i = 0; i < parts; i++) {
            int y = i * chunkHeight;
            int h = (i == parts - 1) ? (original.getHeight() - y) : chunkHeight;
            
            BufferedImage sub = original.getSubimage(0, y, original.getWidth(), h);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(sub, "jpg", baos);
            
            ImagePartMessage msg = new ImagePartMessage(jobId, i, parts, baos.toByteArray(), original.getWidth());
            if (msg.getImageData().length == 0){
                System.err.println("No se pudo generar la división de la imagen");
                return ResponseEntity.badRequest().body("No se pudo procesar la imagen");
            }
            String menssage = objectMapper.writeValueAsString(msg);
            rabbitTemplate.convertAndSend("image_exchange", "to_worker", menssage);
        }
        return ResponseEntity.ok("Procesando Job: " + jobId + "Aceede a la imagen final en: http://localhost:8080/image/sobel/result_"+ jobId +".jpg ");
    }
}