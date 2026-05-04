package com.grupoamarillo.hit1.etapa3.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/image/health")
    public Map<String, String> health() {
        return Map.of("servicio", "ok");
    }
}
