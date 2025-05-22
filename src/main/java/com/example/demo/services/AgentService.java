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

/**
 * Service responsible for managing JADE agents within different containers.
 * This includes starting, stopping, suspending, and resuming agents,
 * as well as listing currently running agents.
 * It relies on a map of pre-configured JADE agent containers.
 */
@Service
public class AgentService {

    private Map<String, AgentContainer> agentContainers; // Map of available agent containers, keyed by name
    private final Map<String, AgentController> runningAgents = new HashMap<>(); // Map to keep track of running agents and their controllers

    /**
     * Constructs the AgentService with a map of available agent containers.
     * This map is typically injected by Spring, containing containers defined in {@link com.example.demo.config.JadeConfig}.
     *
     * @param agentContainers A map where keys are container names and values are {@link AgentContainer} instances.
     */
    public AgentService(Map<String, AgentContainer> agentContainers) {
        this.agentContainers = agentContainers;
    }

    /**
     * Creates and starts a new agent within a specified JADE container.
     *
     * @param containerName The name of the container where the agent should be started.
     *                      This name must exist as a key in the `agentContainers` map.
     * @param agentName     The local name to be assigned to the new agent.
     * @param agentClass    The fully qualified class name of the agent to be created.
     * @param args          An array of objects to be passed as arguments to the agent's `setup()` method.
     * @throws StaleProxyException      If there is an issue with the agent controller proxy (e.g., agent already killed).
     * @throws IllegalArgumentException If the specified `containerName` is not found.
     */
    public void startAgentInContainer(String containerName, String agentName, String agentClass, Object[] args)
            throws StaleProxyException {
        AgentContainer container = agentContainers.get(containerName); // Retrieve the specified container
        if (container == null) {
            throw new IllegalArgumentException("Container not found: " + containerName); // Throw exception if container doesn't exist
        }

        // Create the new agent within the container
        AgentController controller = container.createNewAgent(agentName, agentClass, args);
        controller.start(); // Start the newly created agent
        runningAgents.put(agentName, controller); // Store the agent's controller for future management
    }

    /**
     * Stops a running agent.
     * If the agent is found, it is killed and removed from the list of running agents.
     *
     * @param agentName The local name of the agent to be stopped.
     * @throws StaleProxyException If there is an issue with the agent controller proxy.
     */
    public void stopAgent(String agentName) throws StaleProxyException {
        AgentController controller = runningAgents.get(agentName); // Get the controller for the named agent
        if (controller != null) {
            controller.kill(); // Terminate the agent
            runningAgents.remove(agentName); // Remove from the tracking map
        }
    }

    /**
     * Suspends a running agent.
     * If the agent is found and is not already suspended, its execution is paused.
     *
     * @param agentName The local name of the agent to be suspended.
     * @throws StaleProxyException If there is an issue with the agent controller proxy.
     */
    public void suspendAgent(String agentName) throws StaleProxyException {
        AgentController controller = runningAgents.get(agentName); // Get the controller
        if (controller != null) {
            controller.suspend(); // Suspend the agent
        }
    }

    /**
     * Resumes a previously suspended agent.
     * If the agent is found and is suspended, its execution is resumed.
     *
     * @param agentName The local name of the agent to be resumed.
     * @throws StaleProxyException If there is an issue with the agent controller proxy.
     */
    public void resumeAgent(String agentName) throws StaleProxyException {
        AgentController controller = runningAgents.get(agentName); // Get the controller
        if (controller != null) {
            controller.activate(); // Resume the agent
        }
    }

    /**
     * Retrieves an array of names of all currently running agents managed by this service.
     *
     * @return A string array containing the local names of all running agents.
     *         Returns an empty array if no agents are currently running.
     */
    public String[] getRunningAgents() {
        return runningAgents.keySet().toArray(new String[0]); // Return the set of agent names as an array
    }
}
