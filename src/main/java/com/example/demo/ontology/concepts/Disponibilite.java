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
public class Disponibilite implements Concept {
    private int id;
    private int idMedecin;
    private Date dateHeure;
    private int duree; // durée en minutes


    @Override
    public String toString() {
        return "Disponibilite{" +
                "id=" + id +
                ", idMedecin=" + idMedecin +
                ", dateHeure=" + dateHeure +
                ", duree=" + duree +
                '}';
    }
}