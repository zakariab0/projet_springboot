package com.fermeintelligente.ferme1.controller;

import com.fermeintelligente.ferme1.model.Alerte;
import com.fermeintelligente.ferme1.model.DonneesCapteur;
import com.fermeintelligente.ferme1.service.SensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private SensorService sensorService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        BigDecimal temp = sensorService.getTemperature();
        BigDecimal moisture = sensorService.getSoilMoisture();

        sensorService.saveSensorData("temperature", temp);
        sensorService.saveSensorData("soil_moisture", moisture);

        model.addAttribute("temperature", temp.toString());
        model.addAttribute("soilMoisture", moisture.toString());

        List<Alerte> alerts = sensorService.getLatestAlerts();
        model.addAttribute("alerts", alerts);

        List<DonneesCapteur> recentData = sensorService.getRecentSensorData();
        model.addAttribute("recentData", recentData);

        // Send simple lists for Google Charts
        List<SensorService.GraphData> tempGraphData = sensorService.getTemperatureGraphData();
        List<SensorService.GraphData> moistureGraphData = sensorService.getMoistureGraphData();

        model.addAttribute("tempTimes", tempGraphData.stream().map(SensorService.GraphData::getTime).collect(Collectors.toList()));
        model.addAttribute("tempValues", tempGraphData.stream().map(SensorService.GraphData::getValue).collect(Collectors.toList()));
        model.addAttribute("moistureTimes", moistureGraphData.stream().map(SensorService.GraphData::getTime).collect(Collectors.toList()));
        model.addAttribute("moistureValues", moistureGraphData.stream().map(SensorService.GraphData::getValue).collect(Collectors.toList()));

        logger.info("Temp Graph Data: " + tempGraphData.size() + " points");
        logger.info("Moisture Graph Data: " + moistureGraphData.size() + " points");

        return "dashboard";
    }

    @PostMapping("/toggle-alert-status")
    public String toggleAlertStatus(@RequestParam("alertId") int alertId) {
        sensorService.toggleAlertStatus(alertId);
        return "redirect:/dashboard";
    }
}