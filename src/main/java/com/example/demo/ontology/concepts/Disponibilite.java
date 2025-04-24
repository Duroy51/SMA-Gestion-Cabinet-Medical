package com.example.demo.ontology.concepts;



import jade.content.Concept;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;



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

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Date getDateHeure() {
        return dateHeure;
    }

    public int getDuree() {
        return duree;
    }

    public int getIdMedecin() {
        return idMedecin;
    }

    public void setDateHeure(Date dateHeure) {
        this.dateHeure = dateHeure;
    }

    public void setIdMedecin(int idMedecin) {
        this.idMedecin = idMedecin;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }


}