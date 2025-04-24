package com.example.demo.services;


import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AgentService {

   /* @Autowired
    @Qualifier("mainContainer")
    private AgentContainer mainContainer;*/


    private Map<String, AgentContainer> agentContainers;

    private final Map<String, AgentController> runningAgents = new HashMap<>();


    public AgentService(Map<String, AgentContainer> agentContainers) {
        this.agentContainers = agentContainers;
    }


    /**
     * Crée et démarre un agent dans le conteneur principal
     */
    /*public void startAgent(String agentName, String agentClass, Object[] args) throws StaleProxyException {
        AgentController controller = mainContainer.createNewAgent(agentName, agentClass, args);
        controller.start();
        runningAgents.put(agentName, controller);
    }*/

    /**
     * Crée et démarre un agent dans un conteneur spécifique
     */
    public void startAgentInContainer(String containerName, String agentName, String agentClass, Object[] args)
            throws StaleProxyException {
        AgentContainer container = agentContainers.get(containerName);
        if (container == null) {
            throw new IllegalArgumentException("Container not found: " + containerName);
        }

        AgentController controller = container.createNewAgent(agentName, agentClass, args);
        controller.start();
        runningAgents.put(agentName, controller);
    }

    /**
     * Arrête un agent
     */
    public void stopAgent(String agentName) throws StaleProxyException {
        AgentController controller = runningAgents.get(agentName);
        if (controller != null) {
            controller.kill();
            runningAgents.remove(agentName);
        }
    }

    /**
     * Suspendre un agent
     */
    public void suspendAgent(String agentName) throws StaleProxyException {
        AgentController controller = runningAgents.get(agentName);
        if (controller != null) {
            controller.suspend();
        }
    }

    /**
     * Réactiver un agent suspendu
     */
    public void resumeAgent(String agentName) throws StaleProxyException {
        AgentController controller = runningAgents.get(agentName);
        if (controller != null) {
            controller.activate();
        }
    }

    /**
     * Obtenir la liste des noms d'agents en cours d'exécution
     */
    public String[] getRunningAgents() {
        return runningAgents.keySet().toArray(new String[0]);
    }
}
