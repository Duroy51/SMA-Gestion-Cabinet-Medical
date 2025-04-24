package com.example.demo.ontology.concepts;



import jade.content.Concept;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;



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

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setIdMedecin(int idMedecin) {
        this.idMedecin = idMedecin;
    }

    public void setDateHeure(Date dateHeure) {
        this.dateHeure = dateHeure;
    }

    public Date getDateHeure() {
        return dateHeure;
    }

    public String getStatus() {
        return status;
    }

    public int getIdMedecin() {
        return idMedecin;
    }

    public int getIdPatient() {
        return idPatient;
    }

    public void setIdPatient(int idPatient) {
        this.idPatient = idPatient;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
