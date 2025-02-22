package com.fermeintelligente.ferme1.repository;

import com.fermeintelligente.ferme1.model.Capteur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapteurRepository extends JpaRepository<Capteur, Integer> {
}