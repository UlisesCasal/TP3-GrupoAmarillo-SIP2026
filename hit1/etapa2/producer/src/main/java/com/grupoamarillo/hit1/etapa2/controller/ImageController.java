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

@RestController
@RequestMapping("/image")
public class ImageController {

    private final ImageProducer imgProd;
    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    public ImageController(ImageProducer imgProd) {
        this.imgProd = imgProd;
    }

    @PostMapping("/sobel")
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file, @RequestParam(name="parts", defaultValue="4") int parts) throws Exception {
        BufferedImage original = ImageIO.read(file.getInputStream());
        String jobId = UUID.randomUUID().toString();

        // parts no puede ser negativo no mayor al tamaño de la imagen
        if (parts <= 0 | original.getHeight() < parts) { 
            log.error("Parts value incorrect. Parts value: {} - Image height: {}", parts, original.getHeight());
            System.err.println("Parts value incorrect.");
            return ResponseEntity.badRequest().body("Parts value incorrect");}
        
        // Solo se admiten imagenes jpg o png
        String origFileName = file.getOriginalFilename();
        String formatImage = origFileName.substring(origFileName.length() -3);
        if (!"jpg".equals(formatImage) && !"png".equals(formatImage) ){
          log.error("Image format not allow. Image format: {}",formatImage);
          System.err.println("Image format not allow");
          return ResponseEntity.badRequest().body("Image format not allow");
        } 
        
        
        imgProd.processAndSend(file, parts, jobId);
        return ResponseEntity.ok("Procesando Job: " + jobId + " Mira la imagen final en: http://localhost:8088/image/sobel/result_"+ jobId +"."+ formatImage +" ");
    }
}