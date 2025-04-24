package com.example.demo.ontology.concepts;

import jade.content.Concept;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
public class Patient implements Concept {
    private int id;
    private String nom;
    private String prenom;
    private String informationsPersonnelles;


    @Override
    public String toString() {
        return "Patient{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", informationsPersonnelles='" + informationsPersonnelles + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getInformationsPersonnelles() {
        return informationsPersonnelles;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setInformationsPersonnelles(String informationsPersonnelles) {
        this.informationsPersonnelles = informationsPersonnelles;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

}