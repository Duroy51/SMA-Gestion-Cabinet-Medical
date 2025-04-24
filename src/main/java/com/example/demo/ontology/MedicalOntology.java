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


public class MedicalOntology extends Ontology {
    // Le nom de l'ontologie
    public static final String ONTOLOGY_NAME = "Medical-Ontology";

    // Méthode d'accès au singleton
    // Singleton instance

    public static Ontology getInstance() {
        return instance;
    }

    public static void setInstance(Ontology instance) {
        MedicalOntology.instance = instance;
    }

    private static Ontology instance = new MedicalOntology();

    // Noms des concepts de l'ontologie
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

    // Constructeur privé pour le singleton
    private MedicalOntology() {
        super(ONTOLOGY_NAME, BasicOntology.getInstance());

        try {
            // Ajout des schemas
            add(new ConceptSchema(PATIENT), Patient.class);
            add(new ConceptSchema(CONSULTATION), Consultation.class);
            add(new ConceptSchema(DIAGNOSTIC), Diagnostic.class);
            add(new ConceptSchema(DISPONIBILITE), Disponibilite.class);

            add(new AgentActionSchema(DEMANDER_CONSULTATION), DemanderConsultation.class);
            add(new AgentActionSchema(ENREGISTRER_PATIENT), EnregistrerPatient.class);
            add(new AgentActionSchema(ORGANISER_CONSULTATION), OrganiserConsultation.class);
            add(new AgentActionSchema(REDIGER_DIAGNOSTIC), RedigerDiagnostic.class);

            // Structure des concepts
            ConceptSchema cs = (ConceptSchema) getSchema(PATIENT);
            cs.add(PATIENT_ID, (PrimitiveSchema) getSchema(BasicOntology.INTEGER));
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

            // Structure des actions
            AgentActionSchema as = (AgentActionSchema) getSchema(DEMANDER_CONSULTATION);
            as.add(CONSULTATION, (ConceptSchema) getSchema(CONSULTATION));

            as = (AgentActionSchema) getSchema(ENREGISTRER_PATIENT);
            as.add(PATIENT, (ConceptSchema) getSchema(PATIENT));

            as = (AgentActionSchema) getSchema(ORGANISER_CONSULTATION);
            as.add(CONSULTATION, (ConceptSchema) getSchema(CONSULTATION));
            as.add(DISPONIBILITE.toLowerCase(), (ConceptSchema) getSchema(DISPONIBILITE));

            as = (AgentActionSchema) getSchema(REDIGER_DIAGNOSTIC);
            as.add(DIAGNOSTIC, (ConceptSchema) getSchema(DIAGNOSTIC));
            as.add(CONSULTATION.toLowerCase(), (ConceptSchema) getSchema(CONSULTATION));

        } catch (OntologyException oe) {
            oe.printStackTrace();
        }
    }

}
