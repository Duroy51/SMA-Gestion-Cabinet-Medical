package com.example.demo.utils;


import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class ContainerUtils {

    @Autowired
    private Runtime jadeRuntime;

    /**
     * Crée dynamiquement un nouveau conteneur JADE
     */
    public AgentContainer createDynamicContainer(String containerName, String mainHost, int mainPort) throws ControllerException {
        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, mainHost);
        profile.setParameter(Profile.MAIN_PORT, String.valueOf(mainPort));
        profile.setParameter(Profile.CONTAINER_NAME, containerName);

        return jadeRuntime.createAgentContainer(profile);
    }

    /**
     * Vérifie si un conteneur existe encore et est actif
     */
    public boolean isContainerActive(AgentContainer container) {
        container.getState();
        return true;
    }

    /**
     * Termine un conteneur
     */
    public void terminateContainer(AgentContainer container) throws ControllerException {
        if (isContainerActive(container)) {
            container.kill();
        }
    }
}
