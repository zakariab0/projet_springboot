package com.fermeintelligente.ferme1.model;

import lombok.Data;
import jakarta.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "capteurs")
public class Capteur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nom_capteur")
    private String nomCapteur;

    @Column(name = "type_capteur")
    private String typeCapteur;

    private String localisation;

    @Column(name = "unite_mesure")
    private String uniteMesure;

    @Column(name = "date_installation")
    @Temporal(TemporalType.DATE)
    private Date dateInstallation;

    @Enumerated(EnumType.STRING)
    private Statut statut = Statut.Actif;

    public enum Statut {
        Actif, En_panne
    }
}