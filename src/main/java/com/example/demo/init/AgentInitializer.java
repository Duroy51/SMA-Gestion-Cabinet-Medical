package com.example.demo.init;


import com.example.demo.agent.AgentExample;
import com.example.demo.agent.Medecin;
import com.example.demo.agent.Patient;
import com.example.demo.agent.Receptionnist;
import com.example.demo.services.AgentService;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AgentInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AgentInitializer.class);
    private AgentService agentService;

    public  AgentInitializer(AgentService agentService) {
        this.agentService = agentService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeAgents() {
        try {
            // Démarrage des agents médecins avec différentes spécialités
            /*agentService.startAgentInContainer("Container-2",
                    "example",
                    AgentExample.class.getName(),
                    new Object[]{"Généraliste"});*/


            //Démarrage de l'agent Patient
            agentService.startAgentInContainer("Container-Patient",
                    "patient",
                    Patient.class.getName(),
                    new Object[]{"Patient"});


            //Demarrage de l'agent Receptionnist
            agentService.startAgentInContainer("Container-Receptionnist",
                    "receptionnist",
                    Receptionnist.class.getName(),
                    new Object[]{"Recptionnist"});


            //Demarrage de l'agent Medecin
            agentService.startAgentInContainer("Container-Medecin",
                    "Medecin",
                    Medecin.class.getName(),
                    new Object[]{"Généraliste"});

            logger.info("Tous les agents ont été démarrés avec succès");
        } catch (StaleProxyException e) {
            logger.error("Erreur lors de l'initialisation des agents: {}", e.getMessage());
        }
    }
}
