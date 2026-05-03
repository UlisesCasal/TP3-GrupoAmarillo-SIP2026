package  com.grupoamarillo.hit1.etapa2.controller;


import java.awt.image.BufferedImage;

import java.util.UUID;

import javax.imageio.ImageIO;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.grupoamarillo.hit1.etapa2.service.ImageProducer;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

@RestController
@RequestMapping("/image")
public class ImageController {

    private final ImageProducer imgProd;
    private static final List<String> ALLOWED_MIMES = List.of("image/jpeg", "image/png");

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    public ImageController(ImageProducer imgProd) {
        this.imgProd = imgProd;
    }

    @PostMapping("/sobel")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file, @RequestParam(name="parts", defaultValue="4") int parts) throws Exception {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
         }
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_MIMES.contains(contentType)) {
            log.error("Content type not allowed: {}", contentType);
            System.err.println("Image format not allow");
            return ResponseEntity.badRequest().body("Unsupported Media Type. Only JPG and PNG are allowed.");
        }

        BufferedImage original = ImageIO.read(file.getInputStream());
        if (original == null) {
            log.error("Could not decode image. The file might be corrupted or in an unsupported format.");
            return ResponseEntity.badRequest().body("Invalid image file");
        }

        if (parts <= 0 || original.getHeight() < parts) { 
            log.error("Parts value incorrect. Parts: {} - Height: {}", parts, original.getHeight());
            return ResponseEntity.badRequest().body("Parts value incorrect");
        }

        String jobId = UUID.randomUUID().toString();
        // parts no puede ser negativo no mayor al tamaño de la imagen
        if (parts <= 0 || original.getHeight() < parts) { 
            log.error("Parts value incorrect. Parts value: {} - Image height: {}", parts, original.getHeight());
            System.err.println("Parts value incorrect.");
            return ResponseEntity.badRequest().body("Parts value incorrect");}
        String formatImage = contentType.substring(contentType.length() -3);
        
        
        imgProd.processAndSend(file, parts, jobId);
        return ResponseEntity.ok("Procesando Job: " + jobId + " Mira la imagen final en: http://localhost:8088/image/sobel/result_"+ jobId +"."+ formatImage);
    }
}