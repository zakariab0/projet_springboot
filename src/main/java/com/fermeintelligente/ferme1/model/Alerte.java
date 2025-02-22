package com.fermeintelligente.ferme1.model;

import lombok.Data;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "alertes")
public class Alerte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @ManyToOne
    @JoinColumn(name = "capteur_id")
    private Capteur capteur;
    @Column(name = "type_alerte")
    private String typeAlerte;
    private BigDecimal seuil;
    @Column(name = "valeur_mesuree")
    private BigDecimal valeurMesuree;
    private LocalDateTime horodatage = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    private Statut statut = Statut.Non_traite;
    @Column(name = "recommendation") // New column for the tip
    private String recommendation;

    public enum Statut {
        Non_traite, Traite
    }
}