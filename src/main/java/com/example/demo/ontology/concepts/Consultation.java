package com.example.demo.ontology.concepts;



import jade.content.Concept;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;



@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Consultation implements Concept {
    private int id;
    private Date dateHeure;
    private String status; // "demandée", "planifiée", "terminée"
    private int idPatient;
    private int idMedecin;

    @Override
    public String toString() {
        return "Consultation{" +
                "id=" + id +
                ", dateHeure=" + dateHeure +
                ", status='" + status + '\'' +
                ", idPatient=" + idPatient +
                ", idMedecin=" + idMedecin +
                '}';
    }
}
