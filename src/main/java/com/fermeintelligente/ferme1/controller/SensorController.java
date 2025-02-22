package com.fermeintelligente.ferme1.controller;

import com.fermeintelligente.ferme1.model.DonneesCapteur; // Add this import
import com.fermeintelligente.ferme1.service.SensorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List; // Add this import

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    @Autowired
    private SensorService sensorService;

    @GetMapping("/temperature")
    public BigDecimal getTemperature() {
        BigDecimal temp = sensorService.getTemperature();
        sensorService.saveSensorData("temperature", temp);
        return temp;
    }

    @GetMapping("/soil-moisture")
    public BigDecimal getSoilMoisture() {
        BigDecimal moisture = sensorService.getSoilMoisture();
        sensorService.saveSensorData("soil_moisture", moisture);
        return moisture;
    }

    // New endpoint for recent data report
    @GetMapping("/recent-data")
    public List<DonneesCapteur> getRecentData() {
        return sensorService.getRecentSensorData();
    }
}