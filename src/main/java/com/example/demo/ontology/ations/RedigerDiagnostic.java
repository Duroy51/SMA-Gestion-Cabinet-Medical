package com.example.demo.ontology.ations;


import com.example.demo.ontology.concepts.Consultation;
import com.example.demo.ontology.concepts.Diagnostic;
import jade.content.AgentAction;


public class RedigerDiagnostic implements AgentAction {
    private Diagnostic diagnostic;
    private Consultation consultation;

    public RedigerDiagnostic() {
    }

    public RedigerDiagnostic(Diagnostic diagnostic, Consultation consultation) {
        this.diagnostic = diagnostic;
        this.consultation = consultation;
    }

    public Diagnostic getDiagnostic() {
        return diagnostic;
    }

    public void setDiagnostic(Diagnostic diagnostic) {
        this.diagnostic = diagnostic;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public void setConsultation(Consultation consultation) {
        this.consultation = consultation;
    }
}
