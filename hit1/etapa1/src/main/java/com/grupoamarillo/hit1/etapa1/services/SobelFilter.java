package com.grupoamarillo.hit1.etapa1.services;

import java.awt.image.BufferedImage;

public class SobelFilter {

    public static BufferedImage apply(BufferedImage input) {
        int width = input.getWidth();
        int height = input.getHeight();

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int[][] gray = new int[height][width];

        // 1. Convertir a escala de grises
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);

                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int intensity = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                gray[y][x] = intensity;
            }
        }

        // 2. Aplicar Sobel (evitando bordes)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {

                int gx =
                        -gray[y-1][x-1] - 2*gray[y][x-1] - gray[y+1][x-1]
                      + gray[y-1][x+1] + 2*gray[y][x+1] + gray[y+1][x+1];

                int gy =
                        -gray[y-1][x-1] - 2*gray[y-1][x] - gray[y-1][x+1]
                      + gray[y+1][x-1] + 2*gray[y+1][x] + gray[y+1][x+1];

                int magnitude = (int)Math.sqrt(gx * gx + gy * gy);

                // clamp [0,255]
                magnitude = Math.min(255, magnitude);

                int pixel = (magnitude << 16) | (magnitude << 8) | magnitude;

                output.setRGB(x, y, pixel);
            }
        }

        return output;
    }
}
