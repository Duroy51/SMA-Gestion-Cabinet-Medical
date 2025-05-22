package com.example.demo.ontology.concepts;

import jade.content.Concept;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
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
}