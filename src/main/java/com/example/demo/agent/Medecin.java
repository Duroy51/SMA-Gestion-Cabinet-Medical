package com.example.demo.agent;

import com.example.demo.agent.base.AbstractAgent;
import jade.content.abs.AbsContentElement;
import jade.core.Location;

import com.example.demo.ontology.MedicalOntology;
import com.example.demo.ontology.ations.*;
import com.example.demo.ontology.concepts.*;
import com.example.demo.ontology.concepts.Diagnostic;
import com.example.demo.ontology.concepts.Disponibilite;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Medecin (Doctor) agent in the medical simulation.
 * This agent manages its availabilities, receives consultation requests,
 * accepts or refuses them, and provides diagnostics.
 * It interacts primarily with the Receptionnist agent.
 */
public class Medecin extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(Medecin.class);
    private Codec codec = new SLCodec();
    private Ontology ontology = MedicalOntology.getInstance();

    // Fields to store Medecin's information
    private String nom; // Doctor's name
    private String specialite; // Doctor's specialty
    private int idMedecin; // Unique integer ID for the doctor
    private String agentName; // AID's local name

    // Data structures to manage doctor's state
    private List<Disponibilite> disponibilites = new ArrayList<>(); // List of availabilities
    private List<Consultation> consultationsAcceptees = new ArrayList<>(); // List of accepted consultations
    private Map<Integer, List<Diagnostic>> historiquePatients = new HashMap<>(); // History of diagnostics per patient ID

    /**
     * Initializes the Medecin agent.
     * This method is called by the JADE platform when the agent is started.
     * It registers the SLCodec and MedicalOntology, retrieves agent arguments (name, specialty, ID),
     * registers the agent with the Directory Facilitator (DF), and adds the
     * {@link ReceptionConsultationBehaviour} to handle incoming messages.
     */
    @Override
    protected void setup() {
        // Register the language codec and ontology with the content manager
        getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(ontology);

            // Récupération des arguments
            Object[] args = getArguments();
            if (args != null && args.length > 2) { // Expected arguments: nom, specialite, idMedecin, [agentName]
                this.nom = (String) args[0];
                this.specialite = (String) args[1];
                this.idMedecin = (Integer) args[2];
                if (args.length > 3 && args[3] instanceof String) {
                    this.agentName = (String) args[3];
                } else {
                    this.agentName = getLocalName();
                }
                System.out.println("Agent médecin " + agentName + " (local name: " + getLocalName() + ") initialisé: Dr. " + nom +
                        " (Spécialité: " + specialite + ", ID: " + idMedecin + ")");
            } else {
                System.out.println("Agent médecin " + getLocalName() + " initialisé avec informations par défaut.");
                this.nom = "Inconnu";
                this.specialite = "Généraliste";
                this.idMedecin = getLocalName().hashCode(); // Fallback ID
                this.agentName = getLocalName();
            }

            // Enregistrement auprès du Directory Facilitator
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("medecin");
            sd.setName(getLocalName());

            dfd.addServices(sd);

            try {
                DFService.register(this, dfd);
            } catch (FIPAException e) {
                logger.error("Error registering Medecin agent {} with DF: {}", getLocalName(), e.getMessage(), e);
            }

            // Ajout des comportements
            addBehaviour(new ReceptionConsultationBehaviour()); // Add main behaviour for handling messages
        }

    /**
     * Called when the agent is being taken down.
     * It deregisters the agent from the Directory Facilitator (DF).
     */
    @Override
    protected void takeDown() {
        // Désenregistrement du DF
        try {
            DFService.deregister(this);
            logger.info("Agent médecin {} (Dr. {}) désenregistré du DF.", getLocalName(), nom);
        } catch (FIPAException e) {
            logger.error("Error deregistering Medecin agent {} from DF: {}", getLocalName(), e.getMessage(), e);
        }
    }

    /**
     * A cyclic behaviour responsible for receiving and processing messages from other agents.
     * It handles requests for consultations and notifications of planned consultations.
     */
    private class ReceptionConsultationBehaviour extends CyclicBehaviour {
        /**
         * Executes the behaviour: receives and processes messages based on their performative and content.
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
                    // Traitement des différents types de messages
                    switch (message.getPerformative()) {
                        case ACLMessage.REQUEST:
                            // Demande d'approbation pour une consultation (typiquement du réceptionniste)
                            ContentElement ce = getContentManager().extractContent(message);
                            if (ce instanceof Action) {
                                Action act = (Action) ce;
                                if (act.getAction() instanceof DemanderConsultation) {
                                    // C'est une demande de consultation, la traiter
                                    processDemandeConsultation(message, act);
                                } else {
                                    logger.warn("Agent {} (Dr. {}): Received REQUEST with unknown action: {}", getLocalName(), nom, act.getAction().getClass().getName());
                                }
                            } else {
                                logger.warn("Agent {} (Dr. {}): Received REQUEST with non-Action content: {}", getLocalName(), nom, ce);
                            }
                            break;

                        case ACLMessage.INFORM:
                            // Notification d'une consultation planifiée (typiquement du réceptionniste)
                            Object content = getContentManager().extractContent(message); // Use extractContent for better handling with ontology
                            if (content instanceof Consultation) { // This should be Action<OrganiserConsultation> from Receptionnist
                                Consultation consultation = (Consultation) content; // Assuming direct Consultation for now based on old code
                                logger.info("Agent {} (Dr. {}): Notification de consultation #{} reçue pour le {}",
                                        getLocalName(), nom, consultation.getId(), consultation.getDateHeure());
                                // Enregistrement de la consultation dans la liste des consultations acceptées
                                updateConsultation(consultation);
                            } else if (content instanceof Action && ((Action) content).getAction() instanceof OrganiserConsultation) {
                                // Correct handling if Receptionnist sends Action(OrganiserConsultation)
                                OrganiserConsultation oc = (OrganiserConsultation) ((Action) content).getAction();
                                Consultation consultation = oc.getConsultation();
                                logger.info("Agent {} (Dr. {}): Notification de consultation (via OrganiserConsultation) #{} reçue pour le {}",
                                        getLocalName(), nom, consultation.getId(), consultation.getDateHeure());
                                updateConsultation(consultation);
                            } else {
                                logger.warn("Agent {} (Dr. {}): Received INFORM with unexpected content: {}", getLocalName(), nom, content);
                            }
                            break;

                        default:
                            logger.warn("Agent {} (Dr. {}): Message non traité de type {} reçu de {}. Contenu: {}",
                                    getLocalName(), nom, ACLMessage.getPerformative(message.getPerformative()),
                                    message.getSender().getLocalName(), message.getContent());
                    }
                } catch (CodecException | OntologyException e) {
                    logger.error("Error processing message in ReceptionConsultationBehaviour for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
                }
            } else {
                block(); // Attendre le prochain message
            }
        }
    }

    /**
     * Processes a {@link DemanderConsultation} request received from another agent (typically Receptionnist).
     * It checks availability for the requested date and replies with AGREE or REFUSE,
     * sending back the original {@link Consultation} object in the content.
     *
     * @param message The incoming ACLMessage containing the request.
     * @param act     The {@link Action} object extracted from the message, containing the DemanderConsultation.
     */
    private void processDemandeConsultation(ACLMessage message, Action act) {
        try {
            DemanderConsultation dc = (DemanderConsultation) act.getAction();
            Consultation consultation = dc.getConsultation(); // The consultation proposed by the Receptionnist

            // 1. Vérifier la disponibilité du médecin pour la date demandée
            boolean estDisponible = verifierDisponibilite(consultation.getDateHeure());

            ACLMessage reply = message.createReply(); // Créer une réponse au message original

            // 2. Préparer la réponse (AGREE ou REFUSE)
            if (estDisponible) {
                reply.setPerformative(ACLMessage.AGREE);
                logger.info("Agent {} (Dr. {}): Consultation AGREE pour la date {} (Consultation ID: {})",
                        getLocalName(), nom, consultation.getDateHeure(), consultation.getId());
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                logger.info("Agent {} (Dr. {}): Consultation REFUSE pour la date {} (Consultation ID: {}) - Indisponible",
                        getLocalName(), nom, consultation.getDateHeure(), consultation.getId());
            }

            // 3. Remplir le contenu de la réponse avec l'objet Consultation original (qui inclut l'ID)
            // Ceci permet au réceptionniste d'identifier à quelle demande cette réponse correspond.
            try {
                reply.setLanguage(codec.getName());
                reply.setOntology(ontology.getName());
                getContentManager().fillContent(reply, consultation); // Envoyer l'objet Consultation original
            } catch (CodecException | OntologyException e) {
                logger.error("Codec or Ontology error filling AGREE/REFUSE reply for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
                // En cas d'erreur lors du remplissage, envoyer une réponse d'échec
                reply.setPerformative(ACLMessage.FAILURE);
                reply.setContent("Error processing consultation response content.");
            }
            send(reply); // 4. Envoyer la réponse

        } catch (Exception e) { // Capturer d'autres exceptions potentielles
             logger.error("Unexpected error in processDemandeConsultation for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
        }
    }

    /**
     * Checks if the doctor is available at the given consultation date.
     *
     * @param dateConsultation The date and time of the proposed consultation.
     * @return {@code true} if the doctor is available, {@code false} otherwise.
     *         Currently, it returns true if any matching availability slot is found, or if no availabilities are defined (defaulting to available).
     */
    private boolean verifierDisponibilite(Date dateConsultation) {
        for (Disponibilite dispo : disponibilites) {
            // Vérifier si la date de consultation correspond à une disponibilité enregistrée
            if (dispo.getDateHeure().equals(dateConsultation)) {
                return true; // Disponible si une correspondance est trouvée
            }
        }
        // Si aucune disponibilité n'est explicitement définie, on considère le médecin disponible (comportement par défaut pour test).
        // Dans un système réel, cela pourrait être false si aucune disponibilité n'est trouvée.
        return disponibilites.isEmpty();
    }

    /**
     * Updates the local list of accepted consultations.
     * If a consultation with the same ID exists, it's updated; otherwise, the new consultation is added.
     *
     * @param consultation The {@link Consultation} to add or update in the accepted list.
     */
    private void updateConsultation(Consultation consultation) {
        // Vérifier si la consultation existe déjà dans la liste des consultations acceptées
        for (int i = 0; i < consultationsAcceptees.size(); i++) {
            if (consultationsAcceptees.get(i).getId() == consultation.getId()) {
                consultationsAcceptees.set(i, consultation); // Mettre à jour la consultation existante
                logger.info("Agent {} (Dr. {}): Consultation acceptée #{} mise à jour.", getLocalName(), nom, consultation.getId());
                return;
            }
        }
        // Si la consultation n'existe pas, l'ajouter
        consultationsAcceptees.add(consultation);
        logger.info("Agent {} (Dr. {}): Nouvelle consultation acceptée #{} ajoutée.", getLocalName(), nom, consultation.getId());
    }

    /**
     * A one-shot behaviour responsible for creating a diagnostic for a consultation
     * and sending it to the Receptionnist agent.
     */
    public class RedigerDiagnosticBehaviour extends OneShotBehaviour {
        private int idConsultation;
        private String description;
        private String recommandations;

        /**
         * Constructor for RedigerDiagnosticBehaviour.
         *
         * @param idConsultation  The ID of the consultation for which the diagnostic is being written.
         * @param description     The textual description of the diagnostic.
         * @param recommandations Any recommendations for the patient.
         */
        public RedigerDiagnosticBehaviour(int idConsultation, String description, String recommandations) {
            this.idConsultation = idConsultation;
            this.description = description;
            this.recommandations = recommandations;
        }

        /**
         * Executes the behaviour: creates the diagnostic, stores it locally,
         * and sends it to the Receptionnist.
         */
        @Override
        public void action() {
            try {
                // 1. Rechercher la consultation correspondante dans les consultations acceptées
                Consultation consultation = null;
                for (Consultation c : consultationsAcceptees) {
                    if (c.getId() == idConsultation) {
                        consultation = c;
                        break;
                    }
                }

                if (consultation == null) {
                    logger.warn("Agent {} (Dr. {}): Consultation #{} non trouvée pour rédiger un diagnostic.", getLocalName(), nom, idConsultation);
                    return;
                }

                // 2. Créer l'objet Diagnostic
                Diagnostic diagnostic = new Diagnostic();
                diagnostic.setIdConsultation(idConsultation);
                diagnostic.setDescription(description);
                diagnostic.setRecommandations(recommandations);
                // L'ID du diagnostic lui-même pourrait être généré ici si nécessaire, par exemple: diagnostic.setId(new Random().nextInt());

                // 3. Créer l'action RedigerDiagnostic pour l'envoyer
                RedigerDiagnostic redigerDiagnosticAction = new RedigerDiagnostic();
                redigerDiagnosticAction.setDiagnostic(diagnostic);
                redigerDiagnosticAction.setConsultation(consultation); // Inclure la consultation pour contexte

                // 4. Enregistrer le diagnostic dans l'historique local du médecin
                int idPatient = consultation.getIdPatient();
                historiquePatients.computeIfAbsent(idPatient, k -> new ArrayList<>()).add(diagnostic);
                logger.info("Agent {} (Dr. {}): Diagnostic pour consultation #{} (Patient ID: {}) enregistré localement.", getLocalName(), nom, idConsultation, idPatient);

                // 5. Notifier le réceptionniste en envoyant l'action RedigerDiagnostic
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.addReceiver(new AID("receptionniste", AID.ISLOCALNAME)); // Cible le réceptionniste
                message.setLanguage(codec.getName());
                message.setOntology(ontology.getName());
                getContentManager().fillContent(message, new Action(getAID(), redigerDiagnosticAction)); // Envelopper l'action
                send(message);

                logger.info("Agent {} (Dr. {}): Diagnostic rédigé et envoyé au réceptionniste pour la consultation #{}.", getLocalName(), nom, idConsultation);

            } catch (CodecException | OntologyException e) {
                logger.error("Codec or Ontology error in RedigerDiagnosticBehaviour for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Adds a new availability slot for the doctor and informs the Receptionnist.
     *
     * @param dateHeure The date and time of the availability.
     * @param duree     The duration of the availability slot in minutes.
     */
    public void ajouterDisponibilite(Date dateHeure, int duree) {
        Disponibilite disponibilite = new Disponibilite();
        disponibilite.setIdMedecin(this.idMedecin); // Utiliser l'ID entier stocké
        disponibilite.setDateHeure(dateHeure);
        disponibilite.setDuree(duree);
        // L'ID de la disponibilité elle-même pourrait être généré ici si nécessaire
        disponibilites.add(disponibilite);

        // Informer le réceptionniste de la nouvelle disponibilité
        informerDisponibilite(disponibilite);

        logger.info("Agent {} (Dr. {}): Nouvelle disponibilité ajoutée: {} (durée: {} minutes). Réceptionniste informé.",
                getLocalName(), nom, dateHeure, duree);
    }

    /**
     * Informs the Receptionnist agent about a new availability.
     * Sends an INFORM ACLMessage containing the {@link Disponibilite} object.
     *
     * @param disponibilite The {@link Disponibilite} object to send.
     */
    private void informerDisponibilite(Disponibilite disponibilite) {
        try {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(new AID("receptionniste", AID.ISLOCALNAME)); // Cible le réceptionniste
            message.setLanguage(codec.getName());
            message.setOntology(ontology.getName());

            // Remplir le contenu du message avec l'objet Disponibilite
            // Note: JADE's fillContent works with Concepts directly for INFORM messages if the ontology supports it.
            // If sending an Action, it would be new Action(getAID(), anActionPredicate);
            // Here, sending the concept directly.
            getContentManager().fillContent(message, (AbsContentElement) disponibilite);
            send(message);
        } catch (CodecException | OntologyException e) {
            logger.error("Codec or Ontology error in informerDisponibilite for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
        }
    }

    /**
     * Initiates the process of writing a diagnostic for a given consultation.
     * This method adds a {@link RedigerDiagnosticBehaviour} to the agent.
     *
     * @param idConsultation  The ID of the consultation.
     * @param description     The description of the diagnostic.
     * @param recommandations Recommendations for the patient.
     */
    public void redigerDiagnostic(int idConsultation, String description, String recommandations) {
        addBehaviour(new RedigerDiagnosticBehaviour(idConsultation, description, recommandations));
    }

    // Getter methods for agent data (primarily for potential external queries or testing)

    /**
     * Gets a copy of the list of availabilities for this doctor.
     * @return A new {@link ArrayList} of {@link Disponibilite}.
     */
    public List<Disponibilite> getDisponibilites() {
        return new ArrayList<>(disponibilites); // Retourner une copie
    }

    /**
     * Gets a copy of the list of accepted consultations for this doctor.
     * @return A new {@link ArrayList} of {@link Consultation}.
     */
    public List<Consultation> getConsultations() {
        return new ArrayList<>(consultationsAcceptees); // Retourner une copie
    }

    /**
     * Gets a copy of the diagnostic history for a specific patient.
     * @param idPatient The ID of the patient.
     * @return A new {@link ArrayList} of {@link Diagnostic}, or an empty list if no history exists.
     */
    public List<Diagnostic> getHistoriquePatient(int idPatient) {
        return new ArrayList<>(historiquePatients.getOrDefault(idPatient, new ArrayList<>())); // Retourner une copie
    }
}

