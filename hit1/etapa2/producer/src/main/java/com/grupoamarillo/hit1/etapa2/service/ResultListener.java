package  com.grupoamarillo.hit1.etapa2.service;

import java.awt.Graphics2D;

import org.springframework.stereotype.Service;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import java.util.List;
import java.util.Map;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.grupoamarillo.hit1.etapa2.dto.ImagePartMessage;

@Service
public class ResultListener {
    // Mapa para guardar fragmentos temporales: JobId -> Lista de fragmentos
    private final Map<String, List<ImagePartMessage>> cache = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    


    @RabbitListener(queues = "sobel.result_queue",containerFactory = "manualAckFactory")
    public void handleResult(Message message, Channel channel) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();

        ImagePartMessage msg = mapper.readValue(message.getBody(), ImagePartMessage.class);
        cache.computeIfAbsent(msg.getJobId(), k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
        
        List<ImagePartMessage> parts = cache.get(msg.getJobId());
        String jobId = msg.getJobId();
        
        // 2. IDEMPOTENCIA (Tolerancia a fallos)
        // Verificamos si ya tenemos este número de secuencia para evitar duplicados 
        // en caso de que un worker haya re-procesado por falta de ACK.
        boolean isDuplicate = parts.stream()
                .anyMatch(p -> p.getSequenceNumber() == msg.getSequenceNumber());

        if (!isDuplicate) {
            parts.add(msg);
        }

        // 3. Verificación de completitud
        if (parts.size() == msg.getTotalParts()) {
            try {
                reconstructImage(parts, msg.getJobId());
            } catch (IOException e) {
                System.err.println("Error al reconstruir JPG para el Job " + jobId + ": " + e.getMessage());
            } finally {
                // 4. LIMPIEZA: Evita fugas de memoria (Memory Leaks)
                cache.remove(jobId);
            }
        }
        
        channel.basicAck(tag,false);
    }

    private void reconstructImage(List<ImagePartMessage> parts, String jobId) throws Exception {
        parts.sort(Comparator.comparingInt(ImagePartMessage::getSequenceNumber));
        
        // Calcular altura total
        int totalHeight = 0;
        List<BufferedImage> images = new ArrayList<>();
        for (ImagePartMessage p : parts) {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(p.getImageData()));
            images.add(img);
            totalHeight += img.getHeight();
        }

        BufferedImage finalImg = new BufferedImage(parts.get(0).getWidth(), totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = finalImg.createGraphics();
        int currentY = 0;
        for (BufferedImage img : images) {
            g.drawImage(img, 0, currentY, null);
            currentY += img.getHeight();
        }
        g.dispose();
        
        File directory = new File("./result");
        // Create directory if it doesn't exist
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File outputFile = new File(directory,"result_" + jobId + ".jpg");
        ImageIO.write(finalImg, "jpg", outputFile);
        cache.remove(jobId);
        System.out.println("Imagen " + jobId + " completada y guardada.");
    }
}