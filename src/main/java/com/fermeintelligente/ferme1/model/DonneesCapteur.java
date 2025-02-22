package com.fermeintelligente.ferme1.model;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "donnees_capteurs")
public class DonneesCapteur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne // This says each data belongs to one sensor
    @JoinColumn(name = "capteur_id") // Links to the sensor’s id
    private Capteur capteur;

    private BigDecimal valeur; // The number the sensor measures, like 25.50

    private LocalDateTime horodatage = LocalDateTime.now(); // When it was measured
}