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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SensorService {

    private static final Logger logger = LoggerFactory.getLogger(SensorService.class);

    @Autowired
    private CapteurRepository capteurRepository;
    @Autowired
    private DonneesCapteurRepository donneesCapteurRepository;
    @Autowired
    private AlerteRepository alerteRepository;

    private static final int TEMP_SENSOR_ID = 1;
    private static final int MOISTURE_SENSOR_ID = 2;

    public BigDecimal getTemperature() {
        return BigDecimal.valueOf(Math.random() * 40);
    }

    public BigDecimal getSoilMoisture() {
        return BigDecimal.valueOf(Math.random() * 100);
    }

    //la fonction catreer des nouvelles donnees dyal les capteurs(humidite awela temperature)
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
            logger.info("Nouveau Sensor: ID=" + savedCapteur.getId() + " pour " + typeCapteur);
            return savedCapteur;
        });

        DonneesCapteur data = new DonneesCapteur();
        data.setCapteur(capteur);
        data.setValeur(valeur);
        data.setHorodatage(LocalDateTime.now());
        donneesCapteurRepository.save(data);
        logger.info("Data sauvegarde " + typeCapteur + ": " + valeur + " avec capteur_id=" + capteur.getId());
        checkForAlerts(capteur, valeur);
    }

    //had la fonction katcreer les alertes m3a la recommendation
    private void checkForAlerts(Capteur capteur, BigDecimal valeur) {
        if (capteur.getTypeCapteur().equals("temperature") && valeur.compareTo(BigDecimal.valueOf(35)) > 0) {
            saveAlert(capteur, "Temperature Elevee!", BigDecimal.valueOf(35), valeur, "Ajouter ombre");
        } else if (capteur.getTypeCapteur().equals("soil_moisture") && valeur.compareTo(BigDecimal.valueOf(75)) < 0) {
            saveAlert(capteur, "Humidite baissee!", BigDecimal.valueOf(75), valeur, "Ajouter l'eau");
        }
    }

    //la fonction katsauvegarder l'alerte creee
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

    //fonction dyal marquer chi alerte comme traite/non traite
    public void toggleAlertStatus(int alertId) { // Changed to toggle
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

    //katjbd les donnees des alertes
    public List<Alerte> getLatestAlerts() {
        return alerteRepository.findAll();
    }

    //katjbd les donnees des capteurs
    public List<DonneesCapteur> getRecentSensorData() {
        return donneesCapteurRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "horodatage"))
        ).getContent();
    }

    public List<GraphData> getTemperatureGraphData() {
        List<DonneesCapteur> data = donneesCapteurRepository.findAll(
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "horodatage"))
        ).getContent();

        List<GraphData> tempData = data.stream()
                .filter(d -> d.getCapteur().getId() == TEMP_SENSOR_ID)
                .map(d -> new GraphData(
                        d.getHorodatage().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        d.getValeur().doubleValue()
                ))
                .collect(Collectors.toList());

        if (tempData.isEmpty()) {
            tempData = new ArrayList<>();
            tempData.add(new GraphData("00:00:00", 0.0));
            tempData.add(new GraphData(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), getTemperature().doubleValue()));
        }
        logger.info("Returning " + tempData.size() + " temp points: " + tempData);
        return tempData;
    }

    public List<GraphData> getMoistureGraphData() {
        List<DonneesCapteur> data = donneesCapteurRepository.findAll(
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "horodatage"))
        ).getContent();

        List<GraphData> moistureData = data.stream()
                .filter(d -> d.getCapteur().getId() == MOISTURE_SENSOR_ID)
                .map(d -> new GraphData(
                        d.getHorodatage().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        d.getValeur().doubleValue()
                ))
                .collect(Collectors.toList());

        if (moistureData.isEmpty()) {
            moistureData = new ArrayList<>();
            moistureData.add(new GraphData("00:00:00", 0.0));
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