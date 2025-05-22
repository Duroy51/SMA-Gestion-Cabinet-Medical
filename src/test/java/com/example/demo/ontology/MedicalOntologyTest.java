package com.example.demo.ontology;

import com.example.demo.ontology.concepts.Consultation;
import com.example.demo.ontology.concepts.Patient;
import jade.content.onto.BasicOntology; // Added import
import jade.content.onto.Ontology;
import jade.content.schema.ConceptSchema;
import jade.content.schema.PrimitiveSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MedicalOntologyTest {

    @Test
    void testOntologySingleton() {
        Ontology instance1 = MedicalOntology.getInstance();
        Ontology instance2 = MedicalOntology.getInstance();
        assertSame(instance1, instance2, "getInstance should return the same instance");
        assertNotNull(instance1, "Ontology instance should not be null");
    }

    @Test
    void testOntologyName() {
        Ontology ontology = MedicalOntology.getInstance();
        assertEquals(MedicalOntology.ONTOLOGY_NAME, ontology.getName());
    }

    @Test
    void testPatientConceptSchema() throws Exception {
        Ontology ontology = MedicalOntology.getInstance();
        ConceptSchema patientSchema = (ConceptSchema) ontology.getSchema(MedicalOntology.PATIENT);
        assertNotNull(patientSchema, "Patient schema should exist");

        // Check attributes
        assertNotNull(patientSchema.getSchema(MedicalOntology.PATIENT_ID));
        assertEquals(BasicOntology.INTEGER, ((PrimitiveSchema)patientSchema.getSchema(MedicalOntology.PATIENT_ID)).getTypeName());
        assertNotNull(patientSchema.getSchema(MedicalOntology.PATIENT_NOM));
        assertEquals(BasicOntology.STRING, ((PrimitiveSchema)patientSchema.getSchema(MedicalOntology.PATIENT_NOM)).getTypeName());

        // Instantiate a Patient concept via ontology (optional, advanced test)
        Patient patientConcept = new Patient();
        patientConcept.setId(1);
        patientConcept.setNom("Test");
        // ... you could try to use the ontology to fill/extract, but that's more ContentManager testing
    }

    @Test
    void testConsultationConceptSchema() throws Exception {
        Ontology ontology = MedicalOntology.getInstance();
        ConceptSchema consultationSchema = (ConceptSchema) ontology.getSchema(MedicalOntology.CONSULTATION);
        assertNotNull(consultationSchema, "Consultation schema should exist");
        assertNotNull(consultationSchema.getSchema(MedicalOntology.CONSULTATION_ID));
        assertEquals(BasicOntology.INTEGER, ((PrimitiveSchema)consultationSchema.getSchema(MedicalOntology.CONSULTATION_ID)).getTypeName());
        assertNotNull(consultationSchema.getSchema(MedicalOntology.CONSULTATION_DATE_HEURE));
        assertEquals(BasicOntology.DATE, ((PrimitiveSchema)consultationSchema.getSchema(MedicalOntology.CONSULTATION_DATE_HEURE)).getTypeName()); // BasicOntology.DATE
    }

    // Add more tests for other concepts and agent actions if desired.
}
