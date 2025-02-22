package com.fermeintelligente.ferme1.repository;

import com.fermeintelligente.ferme1.model.DonneesCapteur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonneesCapteurRepository extends JpaRepository<DonneesCapteur, Integer> {
}