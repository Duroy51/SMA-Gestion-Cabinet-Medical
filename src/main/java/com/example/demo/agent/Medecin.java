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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

    public class Medecin extends AbstractAgent {
        private Codec codec = new SLCodec();
        private Ontology ontology = MedicalOntology.getInstance();
        private String nom;
        private String specialite;
        private List<Disponibilite> disponibilites = new ArrayList<>();
        private List<Consultation> consultationsAcceptees = new ArrayList<>();
        private Map<Integer, List<Diagnostic>> historiquePatients = new HashMap<>();

        @Override
        protected void setup() {
            // Enregistrement du codec et de l'ontologie
            getContentManager().registerLanguage(codec);
            getContentManager().registerOntology(ontology);

            // Récupération des arguments
            Object[] args = getArguments();
            if (args != null && args.length > 1) {
                nom = (String) args[0];
                specialite = (String) args[1];
                System.out.println("Agent médecin " + getLocalName() + " initialisé: Dr. " + nom +
                        " (Spécialité: " + specialite + ")");
            } else {
                System.out.println("Agent médecin initialisé sans informations");
                nom = "Inconnu";
                specialite = "Généraliste";
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
                e.printStackTrace();
            }

            // Ajout des comportements
            addBehaviour(new ReceptionConsultationBehaviour());
        }

        @Override
        protected void takeDown() {
            // Désenregistrement du DF
            try {
                DFService.deregister(this);
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }

        // Comportement pour réceptionner les consultations
        private class ReceptionConsultationBehaviour extends CyclicBehaviour {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchLanguage(codec.getName()),
                        MessageTemplate.MatchOntology(ontology.getName())
                );

                ACLMessage message = myAgent.receive(mt);

                if (message != null) {
                    try {
                        // Traitement des différents types de messages
                        switch (message.getPerformative()) {
                            case ACLMessage.REQUEST:
                                // Demande d'approbation pour une consultation
                                ContentElement ce = getContentManager().extractContent(message);

                                if (ce instanceof Action) {
                                    Action act = (Action) ce;
                                    if (act.getAction() instanceof DemanderConsultation) {
                                        processDemandeConsultation(message, act);
                                    }
                                }
                                break;

                            case ACLMessage.INFORM:
                                // Notification d'une consultation planifiée
                                Object content = getContentManager().extractContent(message);
                                if (content instanceof Consultation) {
                                    Consultation consultation = (Consultation) content;
                                    System.out.println("Agent " + getLocalName() + ": Notification de consultation reçue pour le " +
                                            consultation.getDateHeure());

                                    // Enregistrement de la consultation
                                    updateConsultation(consultation);
                                }
                                break;

                            default:
                                System.out.println("Agent " + getLocalName() + ": Message non traité de type " +
                                        ACLMessage.getPerformative(message.getPerformative()));
                        }
                    } catch (CodecException | OntologyException e) {
                        e.printStackTrace();
                    }
                } else {
                    block();
                }
            }
        }

        // Traitement d'une demande de consultation
        private void processDemandeConsultation(ACLMessage message, Action act) {
            try {
                DemanderConsultation dc = (DemanderConsultation) act.getAction();
                Consultation consultation = dc.getConsultation();

                // Vérification de la disponibilité
                boolean estDisponible = verifierDisponibilite(consultation.getDateHeure());

                ACLMessage reply = message.createReply();

                if (estDisponible) {
                    // Acceptation de la consultation
                    reply.setPerformative(ACLMessage.AGREE);
                    System.out.println("Agent " + getLocalName() + ": Consultation acceptée pour la date " +
                            consultation.getDateHeure());
                } else {
                    // Refus de la consultation
                    reply.setPerformative(ACLMessage.REFUSE);
                    System.out.println("Agent " + getLocalName() + ": Consultation refusée pour la date " +
                            consultation.getDateHeure() + " (indisponible)");
                }

                // Envoi de la réponse
                send(reply);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Méthode pour vérifier la disponibilité
        private boolean verifierDisponibilite(Date dateConsultation) {
            for (Disponibilite dispo : disponibilites) {
                // Vérifier si la date de consultation correspond à une disponibilité
                if (dispo.getDateHeure().equals(dateConsultation)) {
                    return true;
                }
            }
            // Si aucune disponibilité n'est définie, on accepte par défaut (à des fins de test)
            return disponibilites.isEmpty();
        }

        // Méthode pour mettre à jour la liste des consultations
        private void updateConsultation(Consultation consultation) {
            // Vérifier si la consultation existe déjà
            for (int i = 0; i < consultationsAcceptees.size(); i++) {
                if (consultationsAcceptees.get(i).getId() == consultation.getId()) {
                    consultationsAcceptees.set(i, consultation);
                    return;
                }
            }
            // Si non, l'ajouter
            consultationsAcceptees.add(consultation);
        }

        // Comportement pour rédiger un diagnostic
        public class RedigerDiagnosticBehaviour extends OneShotBehaviour {
            private int idConsultation;
            private String description;
            private String recommandations;

            public RedigerDiagnosticBehaviour(int idConsultation, String description, String recommandations) {
                this.idConsultation = idConsultation;
                this.description = description;
                this.recommandations = recommandations;
            }

            @Override
            public void action() {
                try {
                    // Recherche de la consultation correspondante
                    Consultation consultation = null;
                    for (Consultation c : consultationsAcceptees) {
                        if (c.getId() == idConsultation) {
                            consultation = c;
                            break;
                        }
                    }

                    if (consultation == null) {
                        System.out.println("Agent " + getLocalName() + ": Consultation #" + idConsultation + " non trouvée");
                        return;
                    }

                    // Création de l'objet diagnostic
                    Diagnostic diagnostic = new Diagnostic();
                    diagnostic.setIdConsultation(idConsultation);
                    diagnostic.setDescription(description);
                    diagnostic.setRecommandations(recommandations);

                    // Création de l'action
                    RedigerDiagnostic redigerDiagnostic = new RedigerDiagnostic();
                    redigerDiagnostic.setDiagnostic(diagnostic);
                    redigerDiagnostic.setConsultation(consultation);

                    // Enregistrement dans l'historique local
                    int idPatient = consultation.getIdPatient();
                    if (!historiquePatients.containsKey(idPatient)) {
                        historiquePatients.put(idPatient, new ArrayList<>());
                    }
                    historiquePatients.get(idPatient).add(diagnostic);

                    // Notification au réceptionniste
                    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                    message.addReceiver(new AID("receptionniste", AID.ISLOCALNAME));
                    message.setLanguage(codec.getName());
                    message.setOntology(ontology.getName());

                    // Remplissage du contenu du message
                    getContentManager().fillContent(message, new Action(getAID(), redigerDiagnostic));

                    // Envoi du message
                    send(message);

                    System.out.println("Agent " + getLocalName() + ": Diagnostic rédigé pour la consultation #" + idConsultation);
                } catch (CodecException | OntologyException e) {
                    e.printStackTrace();
                }
            }
        }

        // Méthode pour ajouter une disponibilité
        public void ajouterDisponibilite(Date dateHeure, int duree) {
            Disponibilite disponibilite = new Disponibilite();
            disponibilite.setIdMedecin(Integer.parseInt(getLocalName().replace("medecin", "")));
            disponibilite.setDateHeure(dateHeure);
            disponibilite.setDuree(duree);
            disponibilites.add(disponibilite);

            // Informer le réceptionniste de la nouvelle disponibilité
            informerDisponibilite(disponibilite);

            System.out.println("Agent " + getLocalName() + ": Nouvelle disponibilité ajoutée: " +
                    dateHeure + " (durée: " + duree + " minutes)");
        }

        // Méthode pour informer le réceptionniste d'une disponibilité
        private void informerDisponibilite(Disponibilite disponibilite) {
            try {
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.addReceiver(new AID("receptionniste", AID.ISLOCALNAME));
                message.setLanguage(codec.getName());
                message.setOntology(ontology.getName());

                // Remplissage du contenu du message
                getContentManager().fillContent(message, (AbsContentElement) disponibilite);

                // Envoi du message
                send(message);
            } catch (CodecException | OntologyException e) {
                e.printStackTrace();
            }
        }

        // Méthode publique pour rédiger un diagnostic
        public void redigerDiagnostic(int idConsultation, String description, String recommandations) {
            addBehaviour(new RedigerDiagnosticBehaviour(idConsultation, description, recommandations));
        }

        // Méthodes d'accès aux données
        public List<Disponibilite> getDisponibilites() {
            return new ArrayList<>(disponibilites);
        }

        public List<Consultation> getConsultations() {
            return new ArrayList<>(consultationsAcceptees);
        }

        public List<Diagnostic> getHistoriquePatient(int idPatient) {
            return historiquePatients.getOrDefault(idPatient, new ArrayList<>());
        }
    }

