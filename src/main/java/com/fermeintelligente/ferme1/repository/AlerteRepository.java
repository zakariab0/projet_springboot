package com.fermeintelligente.ferme1.repository;

import com.fermeintelligente.ferme1.model.Alerte;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlerteRepository extends JpaRepository<Alerte, Integer> {
}