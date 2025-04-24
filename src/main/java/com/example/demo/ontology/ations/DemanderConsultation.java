package com.example.demo.ontology.ations;


import com.example.demo.ontology.concepts.Consultation;
import jade.content.AgentAction;


public class DemanderConsultation implements AgentAction {
    private Consultation consultation;

    public DemanderConsultation() {
    }

    public DemanderConsultation(Consultation consultation) {
        this.consultation = consultation;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public void setConsultation(Consultation consultation) {
        this.consultation = consultation;
    }
}
