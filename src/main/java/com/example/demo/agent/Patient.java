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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Patient extends AbstractAgent {
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

    private List<Consultation> consultations = new ArrayList<>();

    @Override
    protected void setup() {
        // Enregistrement du codec et de l'ontologie
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);

        // Récupération des arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            if (args[0] instanceof String) {
                this.nom = (String) args[0];
            }
            if (args.length > 1 && args[1] instanceof String) {
                this.prenom = (String) args[1];
            }
            if (args.length > 2 && args[2] instanceof String) {
                this.informationsPersonnelles = (String) args[2];
            }
            if (args.length > 3 && args[3] instanceof Integer) {
                this.idPatient = (Integer) args[3];
            } else {
                // ID par défaut basé sur hashCode du nom et prénom
                this.idPatient = (nom + prenom).hashCode();
            }
        }

        System.out.println("Agent patient " + getLocalName() + " initialisé: " +
                nom + " " + prenom + " (ID: " + idPatient + ")");

        // Ajout des comportements
        addBehaviour(new ReceptionReponseBehaviour());
    }

    @Override
    protected void takeDown() {
        System.out.println("Agent patient " + getLocalName() + " terminé.");
    }

    // Méthode pour demander une consultation
    public void demanderConsultation(int idMedecin, Date dateHeure) {
        addBehaviour(new DemanderConsultationBehaviour(idMedecin, dateHeure));
    }

    // Comportement pour demander une consultation
    private class DemanderConsultationBehaviour extends OneShotBehaviour {
        private int idMedecin;
        private Date dateHeure;

        public DemanderConsultationBehaviour(int idMedecin, Date dateHeure) {
            this.idMedecin = idMedecin;
            this.dateHeure = dateHeure;
        }

        @Override
        public void action() {
            try {
                // Création de l'objet consultation
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

                // Remplissage du contenu
                getContentManager().fillContent(message, new Action(getAID(), demanderConsultation));

                // Envoi du message
                send(message);

                System.out.println("Agent " + getLocalName() + ": Demande de consultation envoyée pour le " +
                        dateHeure + " avec le médecin #" + idMedecin);

            } catch (Codec.CodecException | OntologyException e) {
                e.printStackTrace();
            }
        }
    }

    // Comportement pour réceptionner les réponses
    private class ReceptionReponseBehaviour extends CyclicBehaviour {
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
                        case ACLMessage.AGREE:
                            System.out.println("Agent " + getLocalName() + ": Demande de consultation acceptée");
                            break;

                        case ACLMessage.REFUSE:
                            System.out.println("Agent " + getLocalName() + ": Demande de consultation refusée");
                            break;

                        case ACLMessage.INFORM:
                            // Notification d'une consultation planifiée
                            if (message.getContentObject() instanceof Consultation) {
                                Consultation consultation = (Consultation) message.getContentObject();
                                System.out.println("Agent " + getLocalName() + ": Consultation confirmée pour le " +
                                        consultation.getDateHeure() + " avec le médecin #" + consultation.getIdMedecin());

                                // Mettre à jour la liste des consultations
                                updateConsultation(consultation);
                            }
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

    // Méthode pour mettre à jour une consultation
    private void updateConsultation(Consultation consultation) {
        // Vérifier si la consultation existe déjà
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId() == consultation.getId()) {
                consultations.set(i, consultation);
                return;
            }
        }
        // Si non, l'ajouter
        consultations.add(consultation);
    }

    public List<Consultation> getConsultations() {
        return new ArrayList<>(consultations);
    }

}