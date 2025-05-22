package com.example.demo.ontology;


import com.example.demo.ontology.ations.DemanderConsultation;
import com.example.demo.ontology.ations.EnregistrerPatient;
import com.example.demo.ontology.ations.OrganiserConsultation;
import com.example.demo.ontology.ations.RedigerDiagnostic;
import com.example.demo.ontology.concepts.Consultation;
import com.example.demo.ontology.concepts.Diagnostic;
import com.example.demo.ontology.concepts.Disponibilite;
import com.example.demo.ontology.concepts.Patient;
import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.*;
import lombok.Getter;

/**
 * Defines the JADE ontology for the medical application.
 * This ontology includes concepts such as Patient, Consultation, Diagnostic, and Disponibilite,
 * as well as agent actions like DemanderConsultation, EnregistrerPatient, etc.
 * It follows a singleton pattern for accessing the ontology instance.
 * The ontology structure (schemas for concepts and actions) is defined in its constructor.
 */
public class MedicalOntology extends Ontology {
    /** The unique name of this ontology. */
    public static final String ONTOLOGY_NAME = "Medical-Ontology";

    // Singleton instance of the MedicalOntology
    private static Ontology instance = new MedicalOntology();

    /**
     * Returns the singleton instance of the MedicalOntology.
     *
     * @return The singleton {@link Ontology} instance.
     */
    public static Ontology getInstance() {
        return instance;
    }

    /**
     * Sets the singleton instance of the ontology.
     * This method is typically used for testing or specific lifecycle management scenarios.
     *
     * @param newInstance The new {@link Ontology} instance to set.
     */
    public static void setInstance(Ontology newInstance) {
        MedicalOntology.instance = newInstance;
    }

    // Noms des concepts de l'ontologie
    /** Concept name for a patient. */
    public static final String PATIENT = "Patient";
    public static final String CONSULTATION = "Consultation";
    public static final String DIAGNOSTIC = "Diagnostic";
    public static final String DISPONIBILITE = "Disponibilite";

    // Noms des attributs
    public static final String PATIENT_ID = "id";
    public static final String PATIENT_NOM = "nom";
    public static final String PATIENT_PRENOM = "prenom";
    public static final String PATIENT_INFOS = "informationsPersonnelles";

    public static final String CONSULTATION_ID = "id";
    public static final String CONSULTATION_DATE_HEURE = "dateHeure";
    public static final String CONSULTATION_STATUS = "status";
    public static final String CONSULTATION_ID_PATIENT = "idPatient";
    public static final String CONSULTATION_ID_MEDECIN = "idMedecin";

    public static final String DIAGNOSTIC_ID = "id";
    public static final String DIAGNOSTIC_DESCRIPTION = "description";
    public static final String DIAGNOSTIC_RECOMMANDATIONS = "recommandations";
    public static final String DIAGNOSTIC_ID_CONSULTATION = "idConsultation";

    public static final String DISPONIBILITE_ID = "id";
    public static final String DISPONIBILITE_ID_MEDECIN = "idMedecin";
    public static final String DISPONIBILITE_DATE_HEURE = "dateHeure";
    public static final String DISPONIBILITE_DUREE = "duree";

    // Noms des actions
    public static final String DEMANDER_CONSULTATION = "DemanderConsultation";
    public static final String ENREGISTRER_PATIENT = "EnregistrerPatient";
    public static final String ORGANISER_CONSULTATION = "OrganiserConsultation";
    public static final String REDIGER_DIAGNOSTIC = "RedigerDiagnostic";

    /**
     * Private constructor to enforce the singleton pattern.
     * Initializes the ontology by calling the superclass constructor and then
     * adding all necessary concept schemas and agent action schemas.
     * It defines the structure (attributes) for each concept and action.
     * Catches {@link OntologyException} if there's an error during schema definition.
     */
    private MedicalOntology() {
        super(ONTOLOGY_NAME, BasicOntology.getInstance()); // Uses BasicOntology as the base

        try {
            // Define Concept Schemas and map them to Java classes
            add(new ConceptSchema(PATIENT), Patient.class);
            add(new ConceptSchema(CONSULTATION), Consultation.class);
            add(new ConceptSchema(DIAGNOSTIC), Diagnostic.class);
            add(new ConceptSchema(DISPONIBILITE), Disponibilite.class);

            add(new AgentActionSchema(DEMANDER_CONSULTATION), DemanderConsultation.class);
            add(new AgentActionSchema(ENREGISTRER_PATIENT), EnregistrerPatient.class);
            add(new AgentActionSchema(ORGANISER_CONSULTATION), OrganiserConsultation.class);
            add(new AgentActionSchema(REDIGER_DIAGNOSTIC), RedigerDiagnostic.class);

            // Define structure (attributes) for Concepts
            ConceptSchema cs; // Reusable ConceptSchema variable

            // Patient Concept Structure
            cs = (ConceptSchema) getSchema(PATIENT);
            cs.add(PATIENT_ID, (PrimitiveSchema) getSchema(BasicOntology.INTEGER)); // Unique ID for the patient
            cs.add(PATIENT_NOM, (PrimitiveSchema) getSchema(BasicOntology.STRING));
            cs.add(PATIENT_PRENOM, (PrimitiveSchema) getSchema(BasicOntology.STRING));
            cs.add(PATIENT_INFOS, (PrimitiveSchema) getSchema(BasicOntology.STRING));

            cs = (ConceptSchema) getSchema(CONSULTATION);
            cs.add(CONSULTATION_ID, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            cs.add(CONSULTATION_DATE_HEURE, (PrimitiveSchema) getSchema(BasicOntology.DATE));
            cs.add(CONSULTATION_STATUS, (PrimitiveSchema) getSchema(BasicOntology.STRING));
            cs.add(CONSULTATION_ID_PATIENT, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            cs.add(CONSULTATION_ID_MEDECIN, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));

            cs = (ConceptSchema) getSchema(DIAGNOSTIC);
            cs.add(DIAGNOSTIC_ID, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            cs.add(DIAGNOSTIC_DESCRIPTION, (PrimitiveSchema) getSchema(BasicOntology.STRING));
            cs.add(DIAGNOSTIC_RECOMMANDATIONS, (PrimitiveSchema) getSchema(BasicOntology.STRING));
            cs.add(DIAGNOSTIC_ID_CONSULTATION, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));

            cs = (ConceptSchema) getSchema(DISPONIBILITE);
            cs.add(DISPONIBILITE_ID, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            cs.add(DISPONIBILITE_ID_MEDECIN, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
            cs.add(DISPONIBILITE_DATE_HEURE, (PrimitiveSchema) getSchema(BasicOntology.DATE));
            cs.add(DISPONIBILITE_DUREE, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));

            // Define structure (attributes) for Agent Actions
            AgentActionSchema as; // Reusable AgentActionSchema variable

            // DemanderConsultation Action Structure
            as = (AgentActionSchema) getSchema(DEMANDER_CONSULTATION);
            as.add(CONSULTATION, (ConceptSchema) getSchema(CONSULTATION)); // The consultation being requested

            // EnregistrerPatient Action Structure
            as = (AgentActionSchema) getSchema(ENREGISTRER_PATIENT);
            as.add(PATIENT, (ConceptSchema) getSchema(PATIENT)); // The patient to be registered

            // OrganiserConsultation Action Structure
            as = (AgentActionSchema) getSchema(ORGANISER_CONSULTATION);
            as.add(CONSULTATION, (ConceptSchema) getSchema(CONSULTATION)); // The consultation being organized
            as.add(DISPONIBILITE.toLowerCase(), (ConceptSchema) getSchema(DISPONIBILITE)); // The availability slot used (using toLowerCase for the slot name for consistency if it was intended)

            // RedigerDiagnostic Action Structure
            as = (AgentActionSchema) getSchema(REDIGER_DIAGNOSTIC);
            as.add(DIAGNOSTIC, (ConceptSchema) getSchema(DIAGNOSTIC)); // The diagnostic being written
            as.add(CONSULTATION.toLowerCase(), (ConceptSchema) getSchema(CONSULTATION)); // The consultation related to the diagnostic (using toLowerCase for the slot name for consistency if it was intended)

        } catch (OntologyException oe) {
            // This error should not happen in production if the ontology is correctly defined.
            // It indicates a fundamental issue with the ontology structure.
            System.err.println("Error defining MedicalOntology schemas: " + oe.getMessage());
            oe.printStackTrace(); // Print stack trace for debugging
        }
    }

}
