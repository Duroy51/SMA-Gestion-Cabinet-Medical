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

/**
 * Component responsible for initializing and starting JADE agents when the Spring application is ready.
 * It uses {@link AgentService} to create and start instances of {@link Patient}, {@link Receptionnist},
 * and {@link Medecin} agents in their respective pre-configured containers.
 */
@Component
public class AgentInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AgentInitializer.class);
    private AgentService agentService; // Service for interacting with JADE agent containers

    /**
     * Constructs the AgentInitializer with the necessary {@link AgentService}.
     *
     * @param agentService The service used to start agents in JADE containers.
     */
    public  AgentInitializer(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Initializes and starts all predefined JADE agents upon application startup.
     * This method is triggered by the {@link ApplicationReadyEvent}.
     * It creates two Patient agents, one Receptionnist agent, and two Medecin agents,
     * each with specific arguments (name, specialty, ID, etc.) and starts them in
     * their designated containers ("Container-Patient", "Container-Receptionnist", "Container-Medecin").
     * Logs success or errors encountered during agent initialization.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeAgents() {
        try {
            // Initialize Patient 1
            logger.info("Initializing Patient 1...");
            agentService.startAgentInContainer("Container-Patient",
                    "patient1", // Agent's local name
                    Patient.class.getName(),
                    new Object[]{"John", "Doe", "123 Main St", 1, "patient1"}); // name, prenom, info, int ID, AID name

            // Initialize Patient 2
            logger.info("Initializing Patient 2...");
            agentService.startAgentInContainer("Container-Patient",
                    "patient2", // Agent's local name
                    Patient.class.getName(),
                    new Object[]{"Jane", "Smith", "456 Oak Ave", 2, "patient2"}); // name, prenom, info, int ID, AID name

            // Initialize Receptionnist
            logger.info("Initializing Receptionnist...");
            agentService.startAgentInContainer("Container-Receptionnist",
                    "receptionnist", // Agent's local name
                    Receptionnist.class.getName(),
                    new Object[]{"Reception Desk", "receptionnist"}); // name, AID name

            // Initialize Medecin 1
            logger.info("Initializing Medecin 1 (Dr. House)...");
            agentService.startAgentInContainer("Container-Medecin",
                    "medecin1", // Agent's local name
                    Medecin.class.getName(),
                    new Object[]{"Dr. House", "General Practice", 1, "medecin1"}); // name, specialty, int ID, AID name

            // Initialize Medecin 2
            logger.info("Initializing Medecin 2 (Dr. Cuddy)...");
            agentService.startAgentInContainer("Container-Medecin", // Assumes Medecins can share a container or have separate ones if configured
                    "medecin2", // Agent's local name
                    Medecin.class.getName(),
                    new Object[]{"Dr. Cuddy", "Cardiology", 2, "medecin2"}); // name, specialty, int ID, AID name

            logger.info("All agents (patients, receptionnist, medecins) have been successfully started.");
        } catch (StaleProxyException e) {
            // This exception can occur if the JADE platform or container is not available or if there's an issue with the agent lifecycle.
            logger.error("Error during agent initialization (StaleProxyException): {}. This might indicate issues with JADE container setup or agent lifecycle.", e.getMessage(), e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions during agent initialization
            logger.error("An unexpected error occurred during agent initialization: {}", e.getMessage(), e);
        }
    }
}
