package com.example.demo.ontology.ations;


import com.example.demo.ontology.concepts.Patient;
import jade.content.AgentAction;

public class EnregistrerPatient implements AgentAction {
    private Patient patient;

    public EnregistrerPatient() {
    }

    public EnregistrerPatient(Patient patient) {
        this.patient = patient;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }
}
