package com.example.demo.ontology.ations;


import com.example.demo.ontology.concepts.Consultation;
import com.example.demo.ontology.concepts.Disponibilite;
import jade.content.AgentAction;

public class OrganiserConsultation implements AgentAction {
    private Consultation consultation;
    private Disponibilite disponibilite;

    public OrganiserConsultation() {
    }

    public OrganiserConsultation(Consultation consultation, Disponibilite disponibilite) {
        this.consultation = consultation;
        this.disponibilite = disponibilite;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public void setConsultation(Consultation consultation) {
        this.consultation = consultation;
    }

    public Disponibilite getDisponibilite() {
        return disponibilite;
    }

    public void setDisponibilite(Disponibilite disponibilite) {
        this.disponibilite = disponibilite;
    }
}