package com.grupoamarillo.hit1.etapa2.service;


import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import com.rabbitmq.client.Channel;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


import javax.imageio.ImageIO;
import java.awt.Color;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.grupoamarillo.hit1.etapa2.dto.ImagePartMessage;



@Service
public class SobelWorker {

    @Autowired
    private RabbitTemplate rabbitTemplate;



    @RabbitListener(queues = "sobel.work_queue",containerFactory = "manualAckFactory")
    public void process(Message msg, Channel channel) throws Exception {
        long tag = msg.getMessageProperties().getDeliveryTag();

        ObjectMapper mapper = new ObjectMapper();
        
        ImagePartMessage image =  mapper.readValue(msg.getBody(), ImagePartMessage.class);
        try{
        
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(image.getImageData()));
        System.out.println("Procesando tile: " + (image.getSequenceNumber() + 1) + " / " + image.getTotalParts() );
        BufferedImage result = applySobel(img);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(result, "jpg", baos);
        image.setImageData(baos.toByteArray());
        String partImage = mapper.writeValueAsString(image);
        System.out.println("Sobel de parte aplicado");

        rabbitTemplate.convertAndSend("image_exchange", "to_aggregator", partImage);
        channel.basicAck(tag, false);
        } catch (Exception e){
            System.err.println("Error en proceso de imagen Sobel");
            channel.basicNack(tag, true, false);
        }
    }

    private BufferedImage applySobel(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int[][] gx = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int[][] gy = {{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int px = 0, py = 0;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        int gray = new Color(image.getRGB(x + j, y + i)).getRed();
                        px += gx[i + 1][j + 1] * gray;
                        py += gy[i + 1][j + 1] * gray;
                    }
                }
                int g = (int) Math.min(255, Math.sqrt(px * px + py * py));
                out.setRGB(x, y, new Color(g, g, g).getRGB());
            }
        }
        return out;
    }
}