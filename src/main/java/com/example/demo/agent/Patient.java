package com.example.demo.agent;

import com.example.demo.agent.base.AbstractAgent;
import com.example.demo.ontology.MedicalOntology;
import com.example.demo.ontology.ations.DemanderConsultation;
import com.example.demo.ontology.concepts.Consultation;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents a Patient agent in the medical simulation.
 * This agent can request consultations and receive updates about them.
 * It interacts primarily with the Receptionnist agent.
 */
public class Patient extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(Patient.class);
    private Codec codec = new SLCodec();
    private Ontology ontology = MedicalOntology.getInstance();

    // Getters
    // Setters
    @Getter
    @Setter
    private String nom;
    @Setter
    @Getter
    private String prenom;
    @Setter
    @Getter
    private String informationsPersonnelles;
    @Getter
    private int idPatient;
    private String agentName;

    private List<Consultation> consultations = new ArrayList<>();

    /**
     * Initializes the Patient agent.
     * This method is called by the JADE platform when the agent is started.
     * It registers the SLCodec and MedicalOntology with the content manager,
     * retrieves agent arguments (name, ID, etc.), and adds the
     * {@link ReceptionReponseBehaviour} to handle incoming messages.
     */
    @Override
    protected void setup() {
        // Enregistrement du codec et de l'ontologie
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        // Récupération des arguments
        Object[] args = getArguments();
        if (args != null && args.length > 3) { // nom, prenom, infos, idPatient
            this.nom = (String) args[0];
            this.prenom = (String) args[1];
            this.informationsPersonnelles = (String) args[2];
            this.idPatient = (Integer) args[3]; // Already there
            if (args.length > 4 && args[4] instanceof String) {
                this.agentName = (String) args[4];
            } else {
                this.agentName = getLocalName();
            }
        } else {
            // Default ID and name if arguments are not sufficient
            this.idPatient = (getLocalName()).hashCode(); // Fallback for ID
            this.agentName = getLocalName();
            // Attempt to set nom/prenom from local name if possible, or use defaults
            this.nom = getLocalName(); 
            this.prenom = "";
            this.informationsPersonnelles = "";
        }

        System.out.println("Agent patient " + agentName + " (local name: " + getLocalName() + ") initialisé: " +
                nom + " " + prenom + " (ID: " + idPatient + ")");

        // Ajout des comportements
        addBehaviour(new ReceptionReponseBehaviour());
    }

    /**
     * Called when the agent is being taken down.
     * Logs the termination of the agent.
     */
    @Override
    protected void takeDown() {
        System.out.println("Agent patient " + getLocalName() + " terminé.");
        logger.info("Agent patient {} terminé.", getLocalName());
    }

    /**
     * Initiates a request for a medical consultation.
     * This method adds a {@link DemanderConsultationBehaviour} to the agent,
     * which will handle the process of sending the consultation request to the Receptionnist.
     *
     * @param idMedecin The integer ID of the Medecin (doctor) agent with whom the consultation is requested.
     * @param dateHeure The desired date and time for the consultation.
     */
    public void demanderConsultation(int idMedecin, Date dateHeure) {
        addBehaviour(new DemanderConsultationBehaviour(idMedecin, dateHeure));
    }

    /**
     * A one-shot behaviour responsible for sending a consultation request to the Receptionnist agent.
     * It constructs a {@link Consultation} object, wraps it in a {@link DemanderConsultation} action,
     * and sends it as a REQUEST ACLMessage.
     */
    private class DemanderConsultationBehaviour extends OneShotBehaviour {
        private int idMedecin;
        private Date dateHeure;

        /**
         * Constructor for DemanderConsultationBehaviour.
         *
         * @param idMedecin The ID of the doctor for the consultation.
         * @param dateHeure The desired date and time for the consultation.
         */
        public DemanderConsultationBehaviour(int idMedecin, Date dateHeure) {
            this.idMedecin = idMedecin;
            this.dateHeure = dateHeure;
        }

        /**
         * Executes the behaviour: creates and sends the consultation request message.
         */
        @Override
        public void action() {
            try {
                // 1. Prepare the Consultation object
                Consultation consultation = new Consultation();
                consultation.setId(consultations.size() + 1); // ID provisoire
                consultation.setDateHeure(dateHeure);
                consultation.setStatus("demandée");
                consultation.setIdPatient(idPatient);
                consultation.setIdMedecin(idMedecin);

                // Création de l'action de demande
                DemanderConsultation demanderConsultation = new DemanderConsultation();
                demanderConsultation.setConsultation(consultation);

                // Création du message
                ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                message.addReceiver(new AID("receptionniste", AID.ISLOCALNAME));
                message.setLanguage(codec.getName());
                message.setOntology(ontology.getName());

                // Remplissage du contenu du message avec l'action
                getContentManager().fillContent(message, new Action(getAID(), demanderConsultation));

                // 4. Envoyer le message
                send(message);

                logger.info("Agent {} (Patient {} {}): Demande de consultation envoyée pour le {} avec le médecin #{}",
                        getLocalName(), nom, prenom, dateHeure, idMedecin);

            } catch (Codec.CodecException | OntologyException e) {
                logger.error("Codec or Ontology error in DemanderConsultationBehaviour for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
            }
        }
    }

    /**
     * A cyclic behaviour responsible for receiving and processing messages from other agents.
     * It handles responses to consultation requests (AGREE, REFUSE) and INFORM messages
     * about confirmed consultations.
     */
    private class ReceptionReponseBehaviour extends CyclicBehaviour {
        /**
         * Executes the behaviour: receives and processes messages based on their performative.
         */
        @Override
        public void action() {
            // Template pour filtrer les messages par codec et ontologie
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchLanguage(codec.getName()),
                    MessageTemplate.MatchOntology(ontology.getName())
            );

            ACLMessage message = myAgent.receive(mt); // Tenter de recevoir un message

            if (message != null) {
                try {
                    // Traitement des différents types de messages basé sur le performatif
                    switch (message.getPerformative()) {
                        case ACLMessage.AGREE:
                            // Le réceptionniste a transmis la demande de consultation au médecin
                            logger.info("Agent {} (Patient {} {}): Demande de consultation transmise au médecin par {}.",
                                    getLocalName(), nom, prenom, message.getSender().getLocalName());
                            break;

                        case ACLMessage.REFUSE:
                            // Le réceptionniste n'a pas pu traiter la demande (par exemple, médecin introuvable)
                            // Ou le médecin a refusé directement (ce qui est géré par INFORM avec statut "refusée")
                            logger.warn("Agent {} (Patient {} {}): Demande de consultation refusée par {}. Contenu: {}",
                                    getLocalName(), nom, prenom, message.getSender().getLocalName(), message.getContent());
                            break;

                        case ACLMessage.INFORM:
                            // Le réceptionniste informe d'une mise à jour de la consultation (confirmée, refusée par médecin, etc.)
                            Object contentObject = message.getContentObject();
                            if (contentObject instanceof Consultation) {
                                Consultation consultation = (Consultation) contentObject;
                                logger.info("Agent {} (Patient {} {}): Mise à jour de la consultation #{} reçue: Statut='{}', Date={}, MédecinID={}",
                                        getLocalName(), nom, prenom, consultation.getId(), consultation.getStatus(),
                                        consultation.getDateHeure(), consultation.getIdMedecin());
                                // Mettre à jour la liste des consultations locales
                                updateConsultation(consultation);
                            } else {
                                logger.warn("Agent {} (Patient {} {}): Message INFORM reçu avec contenu inattendu: {}",
                                        getLocalName(), nom, prenom, contentObject);
                            }
                            break;

                        default:
                            logger.warn("Agent {} (Patient {} {}): Message non traité de type {} reçu de {}. Contenu: {}",
                                    getLocalName(), nom, prenom, ACLMessage.getPerformative(message.getPerformative()),
                                    message.getSender().getLocalName(), message.getContent());
                    }
                } catch (Exception e) { // Inclut CodecException, OntologyException de getContentObject
                    logger.error("Error processing message in ReceptionReponseBehaviour for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
                }
            } else {
                block(); // Attendre le prochain message
            }
        }
    }

    /**
     * Updates the local list of consultations with the provided consultation.
     * If a consultation with the same ID exists, it's updated; otherwise, the new consultation is added.
     *
     * @param consultation The {@link Consultation} object to update or add.
     */
    private void updateConsultation(Consultation consultation) {
        // Vérifier si la consultation existe déjà dans la liste locale
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId() == consultation.getId()) {
                // Si elle existe, la remplacer par la nouvelle version
                consultations.set(i, consultation);
                logger.info("Agent {} (Patient {} {}): Consultation #{} mise à jour.", getLocalName(), nom, prenom, consultation.getId());
                return;
            }
        }
        // Si elle n'existe pas, l'ajouter à la liste
        consultations.add(consultation);
        logger.info("Agent {} (Patient {} {}): Nouvelle consultation #{} ajoutée à la liste.", getLocalName(), nom, prenom, consultation.getId());
    }

    /**
     * Returns a copy of the list of consultations associated with this patient.
     *
     * @return A new {@link ArrayList} containing the patient's {@link Consultation} objects.
     */
    public List<Consultation> getConsultations() {
        return new ArrayList<>(consultations); // Retourner une copie pour éviter les modifications externes
    }
}