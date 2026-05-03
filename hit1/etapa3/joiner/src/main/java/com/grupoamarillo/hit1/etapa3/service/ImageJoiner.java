package  com.grupoamarillo.hit1.etapa3.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupoamarillo.hit1.etapa3.dto.ResultChunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ImageJoiner {

    private final String OUTPUT_DIR = "result";
    // Almacén temporal: jobId -> Lista de fragmentos
    private final Map<String, List<ResultChunk>> pendingImages = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(ImageJoiner.class);

    @RabbitListener(queues = "sobel.result_queue",containerFactory = "manualAckFactory")
    public void receiveResult(Message msg, Channel channel) throws IOException {
        long tag = msg.getMessageProperties().getDeliveryTag();
        ObjectMapper mapper = new ObjectMapper();
        ResultChunk chunk = mapper.readValue(msg.getBody(),ResultChunk.class);

        String id = chunk.getJobId();
        
        pendingImages.putIfAbsent(id, Collections.synchronizedList(new ArrayList<>()));
        List<ResultChunk> chunks = pendingImages.get(id);
        boolean isDuplicate = chunks.stream()
                .anyMatch(c -> c.getChunkId() == chunk.getChunkId());

        if (!isDuplicate) {
            chunks.add(chunk);
        }
        

        if (chunks.size() == chunk.getTotalChunks()) {
            stitchAndSave(id, chunks);
            pendingImages.remove(id);
        }
        channel.basicAck(tag,false);
    }

    private void stitchAndSave(String id, List<ResultChunk> chunks) throws IOException {
        log.info("Init Joining");
        // Ordenar fragmentos por su ID de posición
        chunks.sort(Comparator.comparingInt(ResultChunk::getChunkId));

        int totalHeight = 0;
        int width = 0;
        List<BufferedImage> images = new ArrayList<>();

        for (ResultChunk c : chunks) {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(c.getData()));
            images.add(img);
            totalHeight += img.getHeight();
            width = img.getWidth();
        }

        // Crear lienzo final
        BufferedImage finalImage = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = finalImage.createGraphics();

        int currentY = 0;
        for (BufferedImage img : images) {
            g2.drawImage(img, 0, currentY, null);
            currentY += img.getHeight();
        }
        g2.dispose();

        // Asegurar directorio y guardar
        Files.createDirectories(Paths.get(OUTPUT_DIR));
        String format = chunks.get(0).getFormat();
        File outputFile = new File(OUTPUT_DIR + "/result_" + id + "." + format);
        ImageIO.write(finalImage, format, outputFile);
        log.info("Image created");
        System.out.println("Imagen reconstruida y guardada: " + outputFile.getAbsolutePath());
    }
}