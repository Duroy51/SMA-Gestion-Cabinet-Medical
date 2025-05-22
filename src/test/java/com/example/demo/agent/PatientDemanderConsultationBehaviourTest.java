package com.example.demo.agent;

import com.example.demo.ontology.MedicalOntology;
import com.example.demo.ontology.ations.DemanderConsultation;
import com.example.demo.ontology.concepts.Consultation;
import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.AID;
// import jade.core.Agent; // Agent class itself is not directly mocked here
import jade.lang.acl.ACLMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// import org.mockito.ArgumentCaptor; // Not used in the final version of this test

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*; // Mockito not used for the agent itself

class PatientDemanderConsultationBehaviourTest {

    private TestablePatientAgent patientAgent; // Use the inner class
    private ContentManager contentManager;
    private static final Codec codec = new SLCodec();
    private static final Ontology ontology = MedicalOntology.getInstance();

    // Inner class extending Patient for testing protected setup method or direct behavior adding
    // This is a common pattern for testing agent internals or behaviors.
    static class TestablePatientAgent extends Patient {
        public ACLMessage sentMessage;

        // Constructor to set arguments for Patient's setup
        public TestablePatientAgent(Object[] args) {
            super(); // Call Patient constructor
            this.setArguments(args); // Set arguments before setup is called
        }

        @Override
        protected void setup() {
            // Call Patient's actual setup to initialize fields like idPatient
            super.setup();
            // Then register language and ontology for testing purposes
            // Note: Patient's setup also registers these, so this might be redundant
            // but ensures they are registered if super.setup() changes.
            getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(ontology);
        }

        // Override send to capture the message
        @Override
        public void send(ACLMessage msg) {
            this.sentMessage = msg;
        }

        // Expose content manager for setup/verification
        public ContentManager getCM() {
            return getContentManager();
        }

        // Expose getAID for the behaviour if it needs it explicitly
        // Though Action constructor will use agent.getAID()
        public AID getAgentAID() {
            return super.getAID();
        }
    }


    @BeforeEach
    void setUp() {
        // Initialize the agent with arguments, this will also call its setup()
        Object[] args = new Object[]{"TestNom", "TestPrenom", "TestInfo", 123, "testPatient1"}; // Name, Prenom, Info, ID, AgentName
        patientAgent = new TestablePatientAgent(args);
        
        // The TestablePatientAgent's constructor calls setArguments,
        // and its setup() method calls super.setup() which should initialize idPatient.
        // We need to manually call setup because we are not in a JADE container.
        patientAgent.setup(); // This will execute the setup logic in TestablePatientAgent and Patient

        contentManager = patientAgent.getCM();
        // Ensure codec and ontology are registered (Patient's setup should do this)
        // If not, uncomment the following:
        // contentManager.registerLanguage(codec);
        // contentManager.registerOntology(ontology);

        // The AID will be null by default for an agent not in a container.
        // The Action class in JADE uses the agent's AID.
        // We can manually set a default AID for testing if needed, or ensure the agent gets one.
        // For this test, patientAgent.getAID() will be called by the Action constructor.
        // Patient's setup does not name the agent, so getAID() might return a default anonymous AID.
        // This is usually fine for testing the behavior's direct actions.
        // If a specific AID is required for test logic, it would need to be set.
        // Here, we assume the default AID is acceptable for the Action wrapper.
    }

    @Test
    void testDemanderConsultationBehaviourAction() throws Exception {
        // Given
        int medecinId = 1;
        Date consultationDate = new Date(); // Use current date for simplicity

        // The Patient.DemanderConsultationBehaviour is an inner class.
        Patient.DemanderConsultationBehaviour behaviour = patientAgent.new DemanderConsultationBehaviour(medecinId, consultationDate);
        // behaviour.setAgent(patientAgent); // Implicitly set

        // When
        behaviour.action(); // This will call patientAgent.send()

        // Then
        ACLMessage sentMessage = patientAgent.sentMessage; // Access captured message
        assertNotNull(sentMessage, "Message should have been sent");
        assertEquals(ACLMessage.REQUEST, sentMessage.getPerformative());
        assertEquals(1, sentMessage.getAllReceiver().length);
        AID receiverAid = (AID) sentMessage.getAllReceiver().next();
        assertEquals("receptionniste", receiverAid.getLocalName());
        assertEquals(codec.getName(), sentMessage.getLanguage());
        assertEquals(ontology.getName(), sentMessage.getOntology());

        // Verify content
        // Use the agent's content manager as it's configured
        ContentElement content = patientAgent.getContentManager().extractContent(sentMessage);
        assertTrue(content instanceof Action, "Content should be an Action");
        Action action = (Action) content;
        assertTrue(action.getAction() instanceof DemanderConsultation, "Action should be DemanderConsultation");

        DemanderConsultation dc = (DemanderConsultation) action.getAction();
        Consultation consultation = dc.getConsultation();
        assertNotNull(consultation);
        assertEquals(consultationDate, consultation.getDateHeure());
        assertEquals(medecinId, consultation.getIdMedecin());
        
        // Check if patient ID is set correctly from arguments provided in setUp
        assertEquals(123, consultation.getIdPatient()); // patientAgent.getIdPatient() should be 123
        
        // The ID of the consultation itself is set by the behaviour (e.g. size + 1)
        // Patient.java's DemanderConsultationBehaviour uses `consultations.size() + 1`
        // Since 'consultations' list is empty at the start of this test for a new agent, ID should be 1.
        assertEquals(1, consultation.getId(), "Consultation ID should be 1 for the first consultation");
        assertEquals("demandée", consultation.getStatus());
    }
}
