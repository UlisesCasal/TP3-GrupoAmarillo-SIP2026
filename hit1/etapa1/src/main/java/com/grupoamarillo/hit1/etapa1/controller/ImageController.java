package com.grupoamarillo.hit1.etapa1.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.grupoamarillo.hit1.etapa1.services.SobelFilter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/image")
public class ImageController {

    @PostMapping(value = "/sobel", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] sobel(@RequestParam("file") MultipartFile file) throws Exception {

        BufferedImage input = ImageIO.read(file.getInputStream());

        BufferedImage output = SobelFilter.apply(input);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(output, "png", baos);

        return baos.toByteArray();
    }
}