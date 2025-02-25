package com.fermeintelligente.ferme1.service;

import com.fermeintelligente.ferme1.model.Alerte;
import com.fermeintelligente.ferme1.model.Capteur;
import com.fermeintelligente.ferme1.model.DonneesCapteur;
import com.fermeintelligente.ferme1.repository.AlerteRepository;
import com.fermeintelligente.ferme1.repository.CapteurRepository;
import com.fermeintelligente.ferme1.repository.DonneesCapteurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Component
public class SensorService {

    private static final Logger logger = LoggerFactory.getLogger(SensorService.class);

    @Autowired
    private CapteurRepository capteurRepository;
    @Autowired
    private DonneesCapteurRepository donneesCapteurRepository;
    @Autowired
    private AlerteRepository alerteRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final int TEMP_SENSOR_ID = 1;
    private static final int MOISTURE_SENSOR_ID = 2;

    private final BlockingQueue<BigDecimal> temperatureQueue = new LinkedBlockingQueue<>(1);
    private final BlockingQueue<BigDecimal> moistureQueue = new LinkedBlockingQueue<>(1);

    private final Random random = new Random();

    public BigDecimal getTemperature() {
        BigDecimal temp = temperatureQueue.poll(); // Get and remove latest, or null if empty
        if (temp == null) {
            // Simulate real sensor data with slight variation around 25.5°C
            double newTemp = 25.5 + (random.nextDouble() - 0.5) * 5; // ±2.5°C around 25.5
            temp = BigDecimal.valueOf(newTemp).setScale(1, BigDecimal.ROUND_HALF_UP);
            logger.info("pas de donneses de temperature par kafka, on utilise les donnees par defaut: " + temp + "°C");
            produceSensorData("temperature", temp);
        }
        return temp;
    }

    public BigDecimal getSoilMoisture() {
        BigDecimal moisture = moistureQueue.poll(); // Get and remove latest, or null if empty
        if (moisture == null) {
            // Simulate real sensor data with slight variation around 80.0%
            double newMoisture = 80.0 + (random.nextDouble() - 0.5) * 10; // ±5% around 80.0
            moisture = BigDecimal.valueOf(newMoisture).setScale(1, BigDecimal.ROUND_HALF_UP);
            logger.info("pas de donneses d'humidite par kafka, on utilise les donnees par defaut: " + moisture + "%");
            produceSensorData("soil_moisture", moisture);
        }
        return moisture;
    }

    private void produceSensorData(String type, BigDecimal value) {
        kafkaTemplate.send("farm-data", type + "," + value.toString());
        logger.info("envoye a Kafka: " + type + "," + value);
    }

    @KafkaListener(topics = "farm-data", groupId = "smart-farm-group")
    public void listenFarmData(String message) {
        try {
            String[] parts = message.split(",", 2);
            if (parts.length == 2) {
                String type = parts[0];
                BigDecimal value = new BigDecimal(parts[1]);
                if ("temperature".equals(type)) {
                    temperatureQueue.poll(); // Clear old value
                    temperatureQueue.offer(value);
                    logger.info("temperature envoyee par kafka: " + value);
                } else if ("soil_moisture".equals(type)) {
                    moistureQueue.poll(); // Clear old value
                    moistureQueue.offer(value);
                    logger.info("temperature envoyee a kafka: " + value);
                }
            }
        } catch (Exception e) {
            logger.error("erreur au niveau de kafka: " + message, e);
        }
    }

    public void saveSensorData(String typeCapteur, BigDecimal valeur) {
        int sensorId = "temperature".equals(typeCapteur) ? TEMP_SENSOR_ID : MOISTURE_SENSOR_ID;
        Capteur capteur = capteurRepository.findById(sensorId).orElseGet(() -> {
            Capteur newCapteur = new Capteur();
            newCapteur.setNomCapteur(typeCapteur + "_sensor");
            newCapteur.setTypeCapteur(typeCapteur);
            newCapteur.setLocalisation("Tomato Field");
            newCapteur.setUniteMesure(typeCapteur.equals("temperature") ? "°C" : "%");
            newCapteur.setDateInstallation(new Date());
            newCapteur.setStatut(Capteur.Statut.Actif);
            Capteur savedCapteur = capteurRepository.save(newCapteur);
            logger.info("Created new sensor: ID=" + savedCapteur.getId() + " for " + typeCapteur);
            return savedCapteur;
        });

        DonneesCapteur data = new DonneesCapteur();
        data.setCapteur(capteur);
        data.setValeur(valeur);
        data.setHorodatage(LocalDateTime.now());
        donneesCapteurRepository.save(data);
        logger.info("Saved data for " + typeCapteur + ": " + valeur + " with capteur_id=" + capteur.getId());
        checkForAlerts(capteur, valeur);
    }

    private void checkForAlerts(Capteur capteur, BigDecimal valeur) {
        if (capteur.getTypeCapteur().equals("temperature") && valeur.compareTo(BigDecimal.valueOf(35)) > 0) {
            saveAlert(capteur, "Chaud!", BigDecimal.valueOf(35), valeur, "ajouter l'ombre");
        } else if (capteur.getTypeCapteur().equals("soil_moisture") && valeur.compareTo(BigDecimal.valueOf(75)) < 0) {
            saveAlert(capteur, "manque d'humidite!", BigDecimal.valueOf(75), valeur, "ajouter d'eau!");
        }
    }

    private void saveAlert(Capteur capteur, String typeAlerte, BigDecimal seuil, BigDecimal valeur, String recommendation) {
        Alerte alerte = new Alerte();
        alerte.setCapteur(capteur);
        alerte.setTypeAlerte(typeAlerte);
        alerte.setSeuil(seuil);
        alerte.setValeurMesuree(valeur);
        alerte.setHorodatage(LocalDateTime.now());
        alerte.setStatut(Alerte.Statut.Non_traite);
        alerte.setRecommendation(recommendation);
        alerteRepository.save(alerte);
    }

    public void toggleAlertStatus(int alertId) {
        Alerte alerte = alerteRepository.findById(alertId).orElseThrow(() -> new RuntimeException("Alert not found"));
        if (alerte.getStatut() == Alerte.Statut.Non_traite) {
            alerte.setStatut(Alerte.Statut.Traite);
            logger.info("Marked alert ID=" + alertId + " as Traité");
        } else {
            alerte.setStatut(Alerte.Statut.Non_traite);
            logger.info("Marked alert ID=" + alertId + " as Non_traite");
        }
        alerteRepository.save(alerte);
    }

    public List<Alerte> getLatestAlerts() {
        return alerteRepository.findAll();
    }

    public List<DonneesCapteur> getRecentSensorData() {
        return donneesCapteurRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "horodatage"))
        ).getContent();
    }

    public List<GraphData> getTemperatureGraphData() {
        List<DonneesCapteur> data = donneesCapteurRepository.findAll(
                Sort.by(Sort.Direction.ASC, "horodatage")
        ); // Use List directly, no Page

        List<GraphData> tempData = data.stream()
                .filter(d -> d.getCapteur().getId() == TEMP_SENSOR_ID)
                .map(d -> new GraphData(
                        d.getHorodatage().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        d.getValeur().doubleValue()
                ))
                .collect(Collectors.toList());

        if (tempData.isEmpty()) {
            tempData = new ArrayList<>();
            tempData.add(new GraphData("00:00:00", getTemperature().doubleValue()));
            tempData.add(new GraphData(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), getTemperature().doubleValue()));
        }
        logger.info("Returning " + tempData.size() + " temp points: " + tempData);
        return tempData;
    }

    public List<GraphData> getMoistureGraphData() {
        List<DonneesCapteur> data = donneesCapteurRepository.findAll(
                Sort.by(Sort.Direction.ASC, "horodatage")
        ); // Use List directly, no Page

        List<GraphData> moistureData = data.stream()
                .filter(d -> d.getCapteur().getId() == MOISTURE_SENSOR_ID)
                .map(d -> new GraphData(
                        d.getHorodatage().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        d.getValeur().doubleValue()
                ))
                .collect(Collectors.toList());

        if (moistureData.isEmpty()) {
            moistureData = new ArrayList<>();
            moistureData.add(new GraphData("00:00:00", getSoilMoisture().doubleValue()));
            moistureData.add(new GraphData(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), getSoilMoisture().doubleValue()));
        }
        logger.info("Returning " + moistureData.size() + " moisture points: " + moistureData);
        return moistureData;
    }

    public static class GraphData {
        private String time;
        private double value;

        public GraphData(String time, double value) {
            this.time = time;
            this.value = value;
        }

        public String getTime() { return time; }
        public double getValue() { return value; }

        @Override
        public String toString() { return "{time=" + time + ", value=" + value + "}"; }
    }
}