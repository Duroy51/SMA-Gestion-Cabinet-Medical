package com.example.demo.agent;

import com.example.demo.agent.base.AbstractAgent;
import com.example.demo.ontology.MedicalOntology;
import com.example.demo.ontology.ations.DemanderConsultation;
import com.example.demo.ontology.ations.EnregistrerPatient;
import com.example.demo.ontology.ations.OrganiserConsultation;
import com.example.demo.ontology.ations.RedigerDiagnostic;
import com.example.demo.ontology.concepts.Consultation;
import com.example.demo.ontology.concepts.Diagnostic;
import com.example.demo.ontology.concepts.Disponibilite;
import com.example.demo.ontology.concepts.Patient;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a Receptionnist agent in the medical simulation.
 * This agent acts as a central coordinator, managing patient registration,
 * consultation requests, and communication between Patient and Medecin agents.
 * It also maintains lists of patients, consultations, and doctor availabilities.
 */
public class Receptionnist extends AbstractAgent {
    private static final Logger logger = LoggerFactory.getLogger(Receptionnist.class);
    private Codec codec = new SLCodec();
    private Ontology ontology = MedicalOntology.getInstance();

    private String nom;
    private String agentName;

    private List<Patient> patients = new ArrayList<>();
    private List<Consultation> consultations = new ArrayList<>();
    private Map<Integer, List<Disponibilite>> disponibilitesMedecins = new HashMap<>();
    private Map<Integer, List<Diagnostic>> diagnosticsPatients = new HashMap<>();

    private int nextConsultationId = 1;
    private int nextPatientId = 1;

    /**
     * Initializes the Receptionnist agent.
     * This method is called by the JADE platform when the agent is started.
     * It registers the SLCodec and MedicalOntology, retrieves agent arguments (name),
     * registers the agent with the Directory Facilitator (DF), and adds the
     * {@link ReceptionMessageBehaviour} to handle incoming messages.
     */
    @Override
    protected void setup() {
        // Register the language codec and ontology with the content manager
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            this.nom = (String) args[0];
            if (args.length > 1 && args[1] instanceof String) {
                this.agentName = (String) args[1];
            } else {
                this.agentName = getLocalName();
            }
        } else {
            this.nom = "Reception Desk Default";
            this.agentName = getLocalName();
        }

        System.out.println("Agent réceptionniste " + agentName + " (local name: " + getLocalName() + ") initialisé. Nom: " + this.nom);

        // Enregistrement auprès du Directory Facilitator
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("receptionniste");
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            logger.error("Error registering Receptionnist agent {} with DF: {}", getLocalName(), e.getMessage(), e);
        }

        // Ajout des comportements
        addBehaviour(new ReceptionMessageBehaviour());
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
            logger.info("Agent réceptionniste {} (Nom: {}) désenregistré du DF.", getLocalName(), nom);
        } catch (FIPAException e) {
            logger.error("Error deregistering Receptionnist agent {} from DF: {}", getLocalName(), e.getMessage(), e);
        }
        logger.info("Agent réceptionniste {} (Nom: {}) terminé.", getLocalName(), nom);
    }

    /**
     * A cyclic behaviour responsible for receiving and processing messages from other agents.
     * It handles various requests and informs based on the MedicalOntology.
     */
    private class ReceptionMessageBehaviour extends CyclicBehaviour {
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
                    logger.debug("Agent {} (Réceptionniste {}): Message reçu de {} (Performative: {})",
                            getLocalName(), nom, message.getSender().getLocalName(), ACLMessage.getPerformative(message.getPerformative()));

                    // Traitement des différents types de messages selon le performatif
                    switch (message.getPerformative()) {
                        case ACLMessage.REQUEST:
                            // Extraction du contenu de l'action
                            ContentElement ce = getContentManager().extractContent(message);
                            if (ce instanceof Action) {
                                Action act = (Action) ce;
                                // Traitement selon le type d'action spécifique
                                if (act.getAction() instanceof DemanderConsultation) {
                                    // Demande de consultation d'un patient
                                    processDemandeConsultation(message, (DemanderConsultation) act.getAction());
                                } else if (act.getAction() instanceof EnregistrerPatient) {
                                    // Demande d'enregistrement d'un nouveau patient
                                    processEnregistrementPatient(message, (EnregistrerPatient) act.getAction());
                                } else {
                                    logger.warn("Agent {} (Réceptionniste {}): Received REQUEST with unknown action type: {}",
                                            getLocalName(), nom, act.getAction().getClass().getName());
                                }
                            } else {
                                logger.warn("Agent {} (Réceptionniste {}): Received REQUEST with non-Action content: {}",
                                        getLocalName(), nom, ce);
                            }
                            break;

                        case ACLMessage.INFORM:
                            // Traitement des notifications (informations diverses)
                            Object content = message.getContentObject(); // getContentObject() est plus simple pour les INFORM non-Action
                            if (content instanceof Disponibilite) {
                                // Un médecin informe de ses disponibilités
                                updateDisponibiliteMedecin((Disponibilite) content);
                            } else if (message.getContentObject() instanceof Action && ((Action) message.getContentObject()).getAction() instanceof RedigerDiagnostic) {
                                // Un médecin a envoyé un diagnostic (enveloppé dans une Action)
                                // Note: getContentManager().extractContent(message) serait plus cohérent si le contenu est une Action
                                Action diagnosticAction = (Action) message.getContentObject();
                                RedigerDiagnostic rd = (RedigerDiagnostic) diagnosticAction.getAction();
                                enregistrerDiagnostic(rd.getDiagnostic());
                            } else {
                                logger.warn("Agent {} (Réceptionniste {}): Received INFORM with unexpected content: {}",
                                        getLocalName(), nom, content != null ? content.getClass().getName() : "null");
                            }
                            break;

                        case ACLMessage.AGREE: // Réponse positive d'un médecin à une demande de consultation
                        case ACLMessage.REFUSE: // Réponse négative d'un médecin
                            // Traiter la réponse du médecin (AGREE ou REFUSE) à une demande de consultation
                            processReponseMedecin(message);
                            break;

                        default:
                            logger.warn("Agent {} (Réceptionniste {}): Message non traité de type {} reçu de {}. Contenu: {}",
                                    getLocalName(), nom, ACLMessage.getPerformative(message.getPerformative()),
                                    message.getSender().getLocalName(), message.getContent());
                    }
                } catch (Exception e) { // Inclut CodecException, OntologyException pour extractContent/getContentObject
                    logger.error("Error processing message in ReceptionMessageBehaviour for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
                }
            } else {
                block(); // Attendre le prochain message
            }
        }
    }

    /**
     * Processes a {@link DemanderConsultation} request from a Patient agent.
     * It assigns a unique ID to the consultation, records it, forwards the request to the
     * specified Medecin agent, and sends an AGREE to the Patient indicating the request is being processed.
     *
     * @param message The original ACLMessage from the Patient.
     * @param dc      The {@link DemanderConsultation} action extracted from the message.
     */
    private void processDemandeConsultation(ACLMessage message, DemanderConsultation dc) {
        try {
            Consultation consultation = dc.getConsultation();

            // 1. Attribuer un ID unique à la consultation et l'enregistrer localement
            consultation.setId(nextConsultationId++);
            consultations.add(consultation); // Garder une trace de toutes les demandes

            logger.info("Agent {} (Réceptionniste {}): Demande de consultation #{} reçue du patient #{} pour le médecin #{} à la date {}",
                    getLocalName(), nom, consultation.getId(), consultation.getIdPatient(), consultation.getIdMedecin(), consultation.getDateHeure());

            // 2. Transférer la demande de consultation au médecin concerné
            ACLMessage forwardMsg = new ACLMessage(ACLMessage.REQUEST);
            // Le nom local du médecin est construit par convention: "medecin" + ID
            forwardMsg.addReceiver(new AID("medecin" + consultation.getIdMedecin(), AID.ISLOCALNAME));
            forwardMsg.setLanguage(codec.getName());
            forwardMsg.setOntology(ontology.getName());
            // L'action DemanderConsultation (dc) est encapsulée dans une Action JADE
            getContentManager().fillContent(forwardMsg, new Action(getAID(), dc));
            send(forwardMsg);
            logger.debug("Agent {} (Réceptionniste {}): Demande de consultation #{} transmise au médecin AID [medecin{}]",
                    getLocalName(), nom, consultation.getId(), consultation.getIdMedecin());

            // 3. Répondre au patient pour l'informer que sa demande a été transmise (AGREE)
            ACLMessage reply = message.createReply();
            reply.setPerformative(ACLMessage.AGREE);
            reply.setContent("Votre demande de consultation (#" + consultation.getId() + ") a été transmise au médecin.");
            send(reply);

        } catch (Exception e) { // Inclut CodecException, OntologyException pour fillContent
            logger.error("Error processing DemandeConsultation for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
            // Optionnel: Informer le patient de l'échec du traitement de sa demande
            ACLMessage errorReply = message.createReply();
            errorReply.setPerformative(ACLMessage.FAILURE);
            errorReply.setContent("Erreur interne lors du traitement de votre demande de consultation.");
            send(errorReply);
        }
    }

    /**
     * Processes an {@link EnregistrerPatient} request.
     * Assigns a unique ID to the new patient (if not already set), checks for duplicates,
     * records the patient, and informs the requester of the outcome.
     *
     * @param message The original ACLMessage containing the request.
     * @param ep      The {@link EnregistrerPatient} action extracted from the message.
     */
    private void processEnregistrementPatient(ACLMessage message, EnregistrerPatient ep) {
        try {
            Patient patientToRegister = ep.getPatient(); // L'objet Patient de l'ontologie

            // Attribuer un ID unique si l'ID du patient est 0 (ou non défini)
            if (patientToRegister.getId() == 0) {
                patientToRegister.setId(nextPatientId++);
            }

            // Vérifier si un patient avec le même nom et prénom existe déjà
            boolean patientExists = false;
            for (Patient p : patients) { // 'patients' est la liste List<com.example.demo.ontology.concepts.Patient>
                if (p.getNom().equals(patientToRegister.getNom()) && p.getPrenom().equals(patientToRegister.getPrenom())) {
                    patientExists = true;
                    logger.warn("Agent {} (Réceptionniste {}): Tentative d'enregistrement d'un patient déjà existant: {} {} (ID: {})",
                            getLocalName(), nom, patientToRegister.getPrenom(), patientToRegister.getNom(), p.getId());
                    // Mettre à jour l'ID du patient à enregistrer avec l'ID existant pour la réponse
                    patientToRegister.setId(p.getId());
                    break;
                }
            }

            // Enregistrer le patient s'il n'existe pas déjà
            if (!patientExists) {
                patients.add(patientToRegister);
                logger.info("Agent {} (Réceptionniste {}): Nouveau patient enregistré: {} {} (ID: {})",
                        getLocalName(), nom, patientToRegister.getPrenom(), patientToRegister.getNom(), patientToRegister.getId());
            }

            // Répondre au demandeur (généralement l'agent qui a initié l'enregistrement)
            ACLMessage reply = message.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("Patient " + (patientExists ? "déjà " : "") + "enregistré avec l'ID: " + patientToRegister.getId());
            send(reply);

        } catch (Exception e) {
            logger.error("Error processing EnregistrementPatient for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
            ACLMessage errorReply = message.createReply();
            errorReply.setPerformative(ACLMessage.FAILURE);
            errorReply.setContent("Erreur interne lors de l'enregistrement du patient.");
            send(errorReply);
        }
    }

    /**
     * Processes a response (AGREE or REFUSE) from a Medecin agent regarding a consultation request.
     * Updates the status of the consultation and notifies the concerned Patient agent.
     *
     * @param message The ACLMessage from the Medecin (AGREE or REFUSE).
     */
    private void processReponseMedecin(ACLMessage message) {
        try {
            // Le médecin renvoie l'objet Consultation dans le contenu de sa réponse
            ContentElement ce = getContentManager().extractContent(message);
            if (ce instanceof Consultation) {
                Consultation repliedConsultation = (Consultation) ce;
                int consultationId = repliedConsultation.getId(); // Obtenir l'ID de l'objet Consultation

                // Rechercher la consultation originale dans la liste locale en utilisant l'ID
                Consultation originalConsultation = null;
                for (Consultation c : consultations) {
                    if (c.getId() == consultationId) {
                        originalConsultation = c;
                        break;
                    }
                }

                if (originalConsultation != null) {
                    String medecinName = message.getSender().getLocalName(); // Nom de l'agent médecin
                    // Mettre à jour le statut de la consultation originale
                    if (message.getPerformative() == ACLMessage.AGREE) {
                        originalConsultation.setStatus("planifiée"); // Ou un statut plus spécifique comme "approuvée par médecin"
                        logger.info("Agent {} (Réceptionniste {}): Consultation #{} confirmée (planifiée) par le médecin {}.",
                                getLocalName(), nom, consultationId, medecinName);

                        // Notifier le patient que la consultation est planifiée
                        ACLMessage notifMsg = new ACLMessage(ACLMessage.INFORM);
                        // Le nom local du patient est construit par convention: "patient" + ID
                        notifMsg.addReceiver(new AID("patient" + originalConsultation.getIdPatient(), AID.ISLOCALNAME));
                        notifMsg.setLanguage(codec.getName());
                        notifMsg.setOntology(ontology.getName());
                        getContentManager().fillContent(notifMsg, originalConsultation); // Envoyer l'objet Consultation mis à jour
                        send(notifMsg);

                    } else if (message.getPerformative() == ACLMessage.REFUSE) {
                        originalConsultation.setStatus("refusée par médecin");
                        logger.info("Agent {} (Réceptionniste {}): Consultation #{} refusée par le médecin {}.",
                                getLocalName(), nom, consultationId, medecinName);
                        
                        // Notifier le patient que la consultation a été refusée
                        ACLMessage notifMsg = new ACLMessage(ACLMessage.INFORM); // Ou ACLMessage.REFUSE au patient
                        notifMsg.addReceiver(new AID("patient" + originalConsultation.getIdPatient(), AID.ISLOCALNAME));
                        notifMsg.setLanguage(codec.getName());
                        notifMsg.setOntology(ontology.getName());
                        // Envoyer l'objet Consultation avec le statut "refusée"
                        getContentManager().fillContent(notifMsg, originalConsultation);
                        send(notifMsg);
                    }
                } else {
                    logger.warn("Agent {} (Réceptionniste {}): Réponse AGREE/REFUSE reçue pour une consultation inconnue ID: {}",
                            getLocalName(), nom, consultationId);
                }
            } else {
                logger.warn("Agent {} (Réceptionniste {}): Réponse AGREE/REFUSE reçue avec un contenu inattendu: {}",
                        getLocalName(), nom, (ce != null ? ce.getClass().getName() : "null"));
            }
        } catch (CodecException | OntologyException e) {
            logger.error("Codec or Ontology error processing AGREE/REFUSE from Medecin for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
        } catch (Exception e) { // Capturer d'autres exceptions potentielles
            logger.error("Unexpected error processing AGREE/REFUSE from Medecin for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
        }
    }

    /**
     * Updates the list of availabilities for a specific Medecin.
     * This is typically called when a Medecin agent sends an INFORM message with its new {@link Disponibilite}.
     *
     * @param disponibilite The {@link Disponibilite} object received from the Medecin.
     */
    private void updateDisponibiliteMedecin(Disponibilite disponibilite) {
        int idMedecin = disponibilite.getIdMedecin();
        // Utiliser computeIfAbsent pour initialiser la liste si le médecin n'est pas encore dans la map
        List<Disponibilite> medecinDispos = disponibilitesMedecins.computeIfAbsent(idMedecin, k -> new ArrayList<>());

        // Vérifier si cette disponibilité spécifique (même date/heure) existe déjà pour éviter les doublons ou la mettre à jour
        boolean existeDeja = false;
        for (int i = 0; i < medecinDispos.size(); i++) {
            if (medecinDispos.get(i).getDateHeure().equals(disponibilite.getDateHeure())) {
                medecinDispos.set(i, disponibilite); // Mettre à jour la disponibilité existante (par exemple, si la durée change)
                existeDeja = true;
                logger.debug("Agent {} (Réceptionniste {}): Disponibilité du médecin #{} mise à jour pour le {}",
                        getLocalName(), nom, idMedecin, disponibilite.getDateHeure());
                break;
            }
        }

        // Si la disponibilité n'existe pas, l'ajouter
        if (!existeDeja) {
            medecinDispos.add(disponibilite);
            logger.info("Agent {} (Réceptionniste {}): Nouvelle disponibilité du médecin #{} ajoutée pour le {}",
                    getLocalName(), nom, idMedecin, disponibilite.getDateHeure());
        }
    }

    /**
     * Records a {@link Diagnostic} received from a Medecin agent.
     * Updates the status of the corresponding consultation to "terminée" and notifies the Patient.
     *
     * @param diagnostic The {@link Diagnostic} object received.
     */
    private void enregistrerDiagnostic(Diagnostic diagnostic) {
        // Rechercher la consultation associée au diagnostic
        Consultation consultationAssociee = null;
        for (Consultation c : consultations) {
            if (c.getId() == diagnostic.getIdConsultation()) {
                consultationAssociee = c;
                break;
            }
        }

        if (consultationAssociee != null) {
            int idPatient = consultationAssociee.getIdPatient();
            // Mettre à jour le statut de la consultation
            consultationAssociee.setStatus("terminée");

            // Enregistrer le diagnostic dans l'historique du patient
            diagnosticsPatients.computeIfAbsent(idPatient, k -> new ArrayList<>()).add(diagnostic);

            logger.info("Agent {} (Réceptionniste {}): Diagnostic enregistré pour le patient #{} (consultation #{}, statut: {})",
                    getLocalName(), nom, idPatient, diagnostic.getIdConsultation(), consultationAssociee.getStatus());

            // Notifier le patient que son diagnostic est disponible
            try {
                ACLMessage notifMsg = new ACLMessage(ACLMessage.INFORM);
                notifMsg.addReceiver(new AID("patient" + idPatient, AID.ISLOCALNAME));
                // Le contenu pourrait être l'objet Diagnostic lui-même ou un message textuel simple.
                // Pour l'instant, un message textuel simple.
                notifMsg.setContent("Votre consultation (#" + diagnostic.getIdConsultation() + ") a été complétée. Un diagnostic est disponible.");
                // Si l'on veut envoyer l'objet Diagnostic:
                // notifMsg.setLanguage(codec.getName());
                // notifMsg.setOntology(ontology.getName());
                // getContentManager().fillContent(notifMsg, diagnostic);
                send(notifMsg);
            } catch (Exception e) { // Catch potentiel pour fillContent si l'objet Diagnostic est envoyé
                logger.error("Error sending diagnostic notification for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
            }
        } else {
            logger.warn("Agent {} (Réceptionniste {}): Diagnostic reçu pour une consultation inconnue #{}",
                    getLocalName(), nom, diagnostic.getIdConsultation());
        }
    }

    /**
     * Initiates the process of organizing a consultation.
     * This method adds an {@link OrganiserConsultationBehaviour} to the agent.
     * This method might be called internally or via an API if the Receptionnist had a controller.
     *
     * @param consultation The {@link Consultation} details to be organized.
     * @param idMedecin    The ID of the Medecin for the consultation.
     */
    public void organiserConsultation(Consultation consultation, int idMedecin) {
        addBehaviour(new OrganiserConsultationBehaviour(consultation, idMedecin));
    }

    /**
     * A one-shot behaviour responsible for finding an available slot for a consultation
     * and, if found, notifying both the Patient and the Medecin.
     * This behaviour seems to be an alternative way to schedule, perhaps initiated by the Receptionnist itself.
     */
    private class OrganiserConsultationBehaviour extends OneShotBehaviour {
        private Consultation consultationDetails; // Renamed for clarity
        private int idMedecinCible;       // Renamed for clarity

        /**
         * Constructor for OrganiserConsultationBehaviour.
         *
         * @param consultation The consultation to be organized.
         * @param idMedecin    The ID of the target doctor.
         */
        public OrganiserConsultationBehaviour(Consultation consultation, int idMedecin) {
            this.consultationDetails = consultation;
            this.idMedecinCible = idMedecin;
        }

        /**
         * Executes the behaviour: tries to find an availability and schedules the consultation.
         */
        @Override
        public void action() {
            try {
                // 1. Rechercher une disponibilité pour le médecin cible
                List<Disponibilite> medecinDispos = disponibilitesMedecins.getOrDefault(idMedecinCible, new ArrayList<>());
                Disponibilite disponibiliteChoisie = null;

                for (Disponibilite dispo : medecinDispos) {
                    // Vérifier si la disponibilité est encore valide (non utilisée par une autre consultation)
                    if (isDisponible(dispo)) { // isDisponible should check against 'idMedecinCible'
                        disponibiliteChoisie = dispo;
                        break;
                    }
                }

                if (disponibiliteChoisie != null) {
                    // 2. Mettre à jour les détails de la consultation avec la disponibilité choisie
                    if (consultationDetails.getId() == 0) { // Si la consultation n'a pas encore d'ID
                        consultationDetails.setId(nextConsultationId++);
                    }
                    consultationDetails.setDateHeure(disponibiliteChoisie.getDateHeure());
                    consultationDetails.setIdMedecin(idMedecinCible);
                    consultationDetails.setStatus("planifiée");

                    // 3. Enregistrer la consultation (ou la mettre à jour si elle existait déjà)
                    // This part might need refinement: is it adding a new one or updating based on patient ID?
                    // For now, it adds it to the main 'consultations' list.
                    consultations.add(consultationDetails); // Assumes adding a new confirmed consultation

                    // 4. Créer l'action OrganiserConsultation pour informer le médecin
                    OrganiserConsultation organiserConsultationAction = new OrganiserConsultation();
                    organiserConsultationAction.setConsultation(consultationDetails);
                    organiserConsultationAction.setDisponibilite(disponibiliteChoisie); // Le médecin peut vouloir connaître la dispo utilisée

                    // 5. Notifier le médecin de la consultation planifiée
                    ACLMessage msgMedecin = new ACLMessage(ACLMessage.INFORM);
                    msgMedecin.addReceiver(new AID("medecin" + idMedecinCible, AID.ISLOCALNAME));
                    msgMedecin.setLanguage(codec.getName());
                    msgMedecin.setOntology(ontology.getName());
                    getContentManager().fillContent(msgMedecin, new Action(getAID(), organiserConsultationAction));
                    send(msgMedecin);

                    // 6. Notifier le patient de la consultation planifiée
                    ACLMessage msgPatient = new ACLMessage(ACLMessage.INFORM);
                    msgPatient.addReceiver(new AID("patient" + consultationDetails.getIdPatient(), AID.ISLOCALNAME));
                    msgPatient.setLanguage(codec.getName());
                    msgPatient.setOntology(ontology.getName());
                    // Envoyer l'objet Consultation complet au patient
                    getContentManager().fillContent(msgPatient, consultationDetails);
                    send(msgPatient);

                    logger.info("Agent {} (Réceptionniste {}): Consultation #{} organisée pour le patient #{} avec le médecin #{} à la date {}",
                            getLocalName(), nom, consultationDetails.getId(), consultationDetails.getIdPatient(),
                            idMedecinCible, consultationDetails.getDateHeure());
                } else {
                    logger.warn("Agent {} (Réceptionniste {}): Impossible d'organiser la consultation pour patient #{}. Aucune disponibilité trouvée pour le médecin #{}.",
                            getLocalName(), nom, consultationDetails.getIdPatient(), idMedecinCible);
                    // Optionnel: Informer le patient de l'échec de l'organisation
                }

            } catch (Exception e) { // Inclut CodecException, OntologyException pour fillContent/setContentObject
                logger.error("Error in OrganiserConsultationBehaviour for agent {}: {}", myAgent.getLocalName(), e.getMessage(), e);
            }
        }

        /**
         * Checks if a specific availability slot is still valid (i.e., not already booked).
         *
         * @param disponibilite The {@link Disponibilite} slot to check.
         * @return {@code true} if the slot is available, {@code false} otherwise.
         */
        private boolean isDisponible(Disponibilite disponibilite) {
            // Vérifier que la disponibilité n'est pas déjà utilisée pour une autre consultation
            // par le MÊME médecin à la MÊME date/heure.
            for (Consultation c : consultations) { // 'consultations' est la liste globale des consultations gérées
                if (c.getIdMedecin() == this.idMedecinCible && // Check against the target doctor of this behaviour
                        c.getDateHeure().equals(disponibilite.getDateHeure()) &&
                        ("planifiée".equals(c.getStatus()) || "confirmée".equals(c.getStatus()))) { // Statuts indiquant que le créneau est pris
                    return false; // Le créneau est déjà pris
                }
            }
            return true; // Le créneau est libre
        }
    }

    // Getters pour accéder aux données internes (principalement pour tests ou potentiels services externes)

    /**
     * Gets a copy of the list of registered patients.
     * @return A new {@link ArrayList} of {@link Patient} (ontology concept).
     */
    public List<Patient> getPatients() {
        return new ArrayList<>(patients); // Retourner une copie pour éviter les modifications externes
    }

    /**
     * Gets a copy of the list of all consultations managed by the receptionnist.
     * @return A new {@link ArrayList} of {@link Consultation}.
     */
    public List<Consultation> getConsultations() {
        return new ArrayList<>(consultations); // Retourner une copie
    }

    /**
     * Gets a copy of the list of availabilities for a specific doctor.
     * @param idMedecin The ID of the doctor.
     * @return A new {@link ArrayList} of {@link Disponibilite}, or an empty list if the doctor has no recorded availabilities.
     */
    public List<Disponibilite> getDisponibilitesMedecin(int idMedecin) {
        return new ArrayList<>(disponibilitesMedecins.getOrDefault(idMedecin, new ArrayList<>())); // Retourner une copie
    }

    /**
     * Gets a copy of the list of diagnostics for a specific patient.
     * @param idPatient The ID of the patient.
     * @return A new {@link ArrayList} of {@link Diagnostic}, or an empty list if the patient has no diagnostics.
     */
    public List<Diagnostic> getDiagnosticsPatient(int idPatient) {
        return new ArrayList<>(diagnosticsPatients.getOrDefault(idPatient, new ArrayList<>())); // Retourner une copie
    }
}