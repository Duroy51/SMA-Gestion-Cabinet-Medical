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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Receptionnist extends AbstractAgent {
    private Codec codec = new SLCodec();
    private Ontology ontology = MedicalOntology.getInstance();

    private List<Patient> patients = new ArrayList<>();
    private List<Consultation> consultations = new ArrayList<>();
    private Map<Integer, List<Disponibilite>> disponibilitesMedecins = new HashMap<>();
    private Map<Integer, List<Diagnostic>> diagnosticsPatients = new HashMap<>();

    private int nextConsultationId = 1;
    private int nextPatientId = 1;

    @Override
    protected void setup() {
        // Enregistrement du codec et de l'ontologie
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        System.out.println("Agent réceptionniste " + getLocalName() + " initialisé.");

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
            e.printStackTrace();
        }

        // Ajout des comportements
        addBehaviour(new ReceptionMessageBehaviour());
    }

    @Override
    protected void takeDown() {
        // Désenregistrement du DF
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println("Agent réceptionniste " + getLocalName() + " terminé.");
    }

    // Comportement pour réceptionner les messages
    private class ReceptionMessageBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchLanguage(codec.getName()),
                    MessageTemplate.MatchOntology(ontology.getName())
            );

            ACLMessage message = myAgent.receive(mt);

            if (message != null) {
                try {
                    // Traitement des différents types de messages selon le performatif
                    switch (message.getPerformative()) {
                        case ACLMessage.REQUEST:
                            // Extraction du contenu
                            ContentElement ce = getContentManager().extractContent(message);

                            if (ce instanceof Action) {
                                Action act = (Action) ce;

                                // Traitement selon le type d'action
                                if (act.getAction() instanceof DemanderConsultation) {
                                    processDemandeConsultation(message, (DemanderConsultation) act.getAction());
                                } else if (act.getAction() instanceof EnregistrerPatient) {
                                    processEnregistrementPatient(message, (EnregistrerPatient) act.getAction());
                                }
                            }
                            break;

                        case ACLMessage.INFORM:
                            // Traitement des notifications
                            Object content = message.getContentObject();

                            if (content instanceof Disponibilite) {
                                // Mise à jour des disponibilités des médecins
                                updateDisponibiliteMedecin((Disponibilite) content);
                            } else if (content instanceof Action && ((Action) content).getAction() instanceof RedigerDiagnostic) {
                                // Enregistrement d'un diagnostic
                                RedigerDiagnostic rd = (RedigerDiagnostic) ((Action) content).getAction();
                                enregistrerDiagnostic(rd.getDiagnostic());
                            }
                            break;

                        case ACLMessage.AGREE:
                        case ACLMessage.REFUSE:
                            // Réponse d'un médecin à une demande de consultation
                            processReponseMedecin(message);
                            break;

                        default:
                            System.out.println("Agent " + getLocalName() + ": Message non traité de type " +
                                    ACLMessage.getPerformative(message.getPerformative()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                block();
            }
        }
    }

    // Traitement d'une demande de consultation
    private void processDemandeConsultation(ACLMessage message, DemanderConsultation dc) {
        try {
            Consultation consultation = dc.getConsultation();

            // Attribution d'un ID unique
            consultation.setId(nextConsultationId++);

            // Enregistrement de la consultation
            consultations.add(consultation);

            System.out.println("Agent " + getLocalName() + ": Demande de consultation reçue du patient #" +
                    consultation.getIdPatient() + " pour le médecin #" + consultation.getIdMedecin() +
                    " à la date " + consultation.getDateHeure());

            // Transfert de la demande au médecin concerné
            ACLMessage forwardMsg = new ACLMessage(ACLMessage.REQUEST);
            forwardMsg.addReceiver(new AID("medecin" + consultation.getIdMedecin(), AID.ISLOCALNAME));
            forwardMsg.setLanguage(codec.getName());
            forwardMsg.setOntology(ontology.getName());

            // Création de l'action
            Action action = new Action(getAID(), dc);
            getContentManager().fillContent(forwardMsg, action);

            // Envoi du message
            send(forwardMsg);

            // Réponse au patient
            ACLMessage reply = message.createReply();
            reply.setPerformative(ACLMessage.AGREE);
            reply.setContent("Demande de consultation transmise au médecin");
            send(reply);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Traitement d'une demande d'enregistrement de patient
    private void processEnregistrementPatient(ACLMessage message, EnregistrerPatient ep) {
        try {
            Patient patient = ep.getPatient();

            // Attribution d'un ID unique si nécessaire
            if (patient.getId() == 0) {
                patient.setId(nextPatientId++);
            }

            // Vérification si le patient existe déjà
            boolean patientExiste = false;
            for (Patient p : patients) {
                if (p.getNom().equals(patient.getNom()) && p.getPrenom().equals(patient.getPrenom())) {
                    patientExiste = true;
                    break;
                }
            }

            // Enregistrement du patient s'il n'existe pas
            if (!patientExiste) {
                patients.add(patient);
                System.out.println("Agent " + getLocalName() + ": Nouveau patient enregistré: " +
                        patient.getPrenom() + " " + patient.getNom() + " (ID: " + patient.getId() + ")");
            } else {
                System.out.println("Agent " + getLocalName() + ": Patient déjà enregistré: " +
                        patient.getPrenom() + " " + patient.getNom());
            }

            // Réponse au demandeur
            ACLMessage reply = message.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent("Patient enregistré avec l'ID: " + patient.getId());
            send(reply);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Traitement d'une réponse d'un médecin à une demande de consultation
    private void processReponseMedecin(ACLMessage message) {
        try {
            // Extraction de l'ID de la consultation depuis le contenu du message
            String content = message.getContent();
            int consultationId = -1;

            if (content != null && content.contains("consultation")) {
                // Extraction de l'ID de consultation (dépend du format du message)
                // Logique d'extraction à adapter selon les besoins
                try {
                    consultationId = Integer.parseInt(content.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException e) {
                    System.out.println("Impossible d'extraire l'ID de consultation du message");
                }
            }

            // Recherche de la consultation concernée
            Consultation consultation = null;
            for (Consultation c : consultations) {
                if (c.getId() == consultationId) {
                    consultation = c;
                    break;
                }
            }

            if (consultation != null) {
                // Mise à jour du statut de la consultation
                if (message.getPerformative() == ACLMessage.AGREE) {
                    consultation.setStatus("planifiée");
                    System.out.println("Agent " + getLocalName() + ": Consultation #" + consultationId +
                            " confirmée par le médecin");

                    // Notification au patient
                    ACLMessage notifMsg = new ACLMessage(ACLMessage.INFORM);
                    notifMsg.addReceiver(new AID("patient" + consultation.getIdPatient(), AID.ISLOCALNAME));
                    notifMsg.setLanguage(codec.getName());
                    notifMsg.setOntology(ontology.getName());
                    notifMsg.setContentObject(consultation);
                    send(notifMsg);

                } else if (message.getPerformative() == ACLMessage.REFUSE) {
                    consultation.setStatus("refusée");
                    System.out.println("Agent " + getLocalName() + ": Consultation #" + consultationId +
                            " refusée par le médecin");

                    // Notification au patient
                    ACLMessage notifMsg = new ACLMessage(ACLMessage.INFORM);
                    notifMsg.addReceiver(new AID("patient" + consultation.getIdPatient(), AID.ISLOCALNAME));
                    notifMsg.setContent("Consultation refusée par le médecin");
                    send(notifMsg);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Mise à jour des disponibilités d'un médecin
    private void updateDisponibiliteMedecin(Disponibilite disponibilite) {
        int idMedecin = disponibilite.getIdMedecin();

        if (!disponibilitesMedecins.containsKey(idMedecin)) {
            disponibilitesMedecins.put(idMedecin, new ArrayList<>());
        }

        List<Disponibilite> disponibilites = disponibilitesMedecins.get(idMedecin);

        // Vérifier si la disponibilité existe déjà
        boolean existe = false;
        for (int i = 0; i < disponibilites.size(); i++) {
            if (disponibilites.get(i).getDateHeure().equals(disponibilite.getDateHeure())) {
                disponibilites.set(i, disponibilite);
                existe = true;
                break;
            }
        }

        // Si non, l'ajouter
        if (!existe) {
            disponibilites.add(disponibilite);
        }

        System.out.println("Agent " + getLocalName() + ": Disponibilité du médecin #" + idMedecin +
                " mise à jour pour le " + disponibilite.getDateHeure());
    }

    // Enregistrement d'un diagnostic
    private void enregistrerDiagnostic(Diagnostic diagnostic) {
        // Recherche de la consultation associée
        Consultation consultation = null;
        for (Consultation c : consultations) {
            if (c.getId() == diagnostic.getIdConsultation()) {
                consultation = c;
                break;
            }
        }

        if (consultation != null) {
            int idPatient = consultation.getIdPatient();

            // Mise à jour du statut de la consultation
            consultation.setStatus("terminée");

            // Enregistrement du diagnostic dans l'historique du patient
            if (!diagnosticsPatients.containsKey(idPatient)) {
                diagnosticsPatients.put(idPatient, new ArrayList<>());
            }
            diagnosticsPatients.get(idPatient).add(diagnostic);

            System.out.println("Agent " + getLocalName() + ": Diagnostic enregistré pour le patient #" +
                    idPatient + " (consultation #" + diagnostic.getIdConsultation() + ")");

            // Notification au patient
            try {
                ACLMessage notifMsg = new ACLMessage(ACLMessage.INFORM);
                notifMsg.addReceiver(new AID("patient" + idPatient, AID.ISLOCALNAME));
                notifMsg.setContent("Votre consultation a été complétée. Un diagnostic est disponible.");
                send(notifMsg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Méthode pour organiser une consultation
    public void organiserConsultation(Consultation consultation, int idMedecin) {
        addBehaviour(new OrganiserConsultationBehaviour(consultation, idMedecin));
    }

    // Comportement pour organiser une consultation
    private class OrganiserConsultationBehaviour extends OneShotBehaviour {
        private Consultation consultation;
        private int idMedecin;

        public OrganiserConsultationBehaviour(Consultation consultation, int idMedecin) {
            this.consultation = consultation;
            this.idMedecin = idMedecin;
        }

        @Override
        public void action() {
            try {
                // Recherche d'une disponibilité du médecin
                List<Disponibilite> disponibilites = disponibilitesMedecins.getOrDefault(idMedecin, new ArrayList<>());
                Disponibilite disponibiliteChoisie = null;

                for (Disponibilite disponibilite : disponibilites) {
                    // Vérification de la disponibilité
                    if (isDisponible(disponibilite)) {
                        disponibiliteChoisie = disponibilite;
                        break;
                    }
                }

                if (disponibiliteChoisie != null) {
                    // Mise à jour de la consultation
                    if (consultation.getId() == 0) {
                        consultation.setId(nextConsultationId++);
                    }
                    consultation.setDateHeure(disponibiliteChoisie.getDateHeure());
                    consultation.setIdMedecin(idMedecin);
                    consultation.setStatus("planifiée");

                    // Enregistrement de la consultation
                    consultations.add(consultation);

                    // Création de l'action OrganiserConsultation
                    OrganiserConsultation organiserConsultation = new OrganiserConsultation();
                    organiserConsultation.setConsultation(consultation);
                    organiserConsultation.setDisponibilite(disponibiliteChoisie);

                    // Notification au médecin
                    ACLMessage msgMedecin = new ACLMessage(ACLMessage.INFORM);
                    msgMedecin.addReceiver(new AID("medecin" + idMedecin, AID.ISLOCALNAME));
                    msgMedecin.setLanguage(codec.getName());
                    msgMedecin.setOntology(ontology.getName());
                    getContentManager().fillContent(msgMedecin, new Action(getAID(), organiserConsultation));
                    send(msgMedecin);

                    // Notification au patient
                    ACLMessage msgPatient = new ACLMessage(ACLMessage.INFORM);
                    msgPatient.addReceiver(new AID("patient" + consultation.getIdPatient(), AID.ISLOCALNAME));
                    msgPatient.setLanguage(codec.getName());
                    msgPatient.setOntology(ontology.getName());
                    msgPatient.setContentObject(consultation);
                    send(msgPatient);

                    System.out.println("Agent " + getLocalName() + ": Consultation organisée pour le patient #" +
                            consultation.getIdPatient() + " avec le médecin #" + idMedecin +
                            " à la date " + consultation.getDateHeure());
                } else {
                    System.out.println("Agent " + getLocalName() + ": Impossible d'organiser la consultation. " +
                            "Aucune disponibilité trouvée pour le médecin #" + idMedecin);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Vérification si une disponibilité est toujours valide
        private boolean isDisponible(Disponibilite disponibilite) {
            // Vérifier que la disponibilité n'est pas déjà utilisée pour une autre consultation
            for (Consultation c : consultations) {
                if (c.getIdMedecin() == idMedecin &&
                        c.getDateHeure().equals(disponibilite.getDateHeure()) &&
                        !c.getStatus().equals("refusée") && !c.getStatus().equals("annulée")) {
                    return false;
                }
            }
            return true;
        }
    }

    // Getters
    public List<Patient> getPatients() {
        return new ArrayList<>(patients);
    }

    public List<Consultation> getConsultations() {
        return new ArrayList<>(consultations);
    }

    public List<Disponibilite> getDisponibilitesMedecin(int idMedecin) {
        return new ArrayList<>(disponibilitesMedecins.getOrDefault(idMedecin, new ArrayList<>()));
    }

    public List<Diagnostic> getDiagnosticsPatient(int idPatient) {
        return new ArrayList<>(diagnosticsPatients.getOrDefault(idPatient, new ArrayList<>()));
    }
}