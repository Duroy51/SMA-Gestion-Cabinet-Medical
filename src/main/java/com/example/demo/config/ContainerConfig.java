package com.example.demo.config;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@PropertySource("classpath:application.properties")
public class ContainerConfig {

    private Runtime jadeRuntime;

    @Value("${jade.main.host}")
    private String mainHost;

    @Value("${jade.main.port}")
    private int mainPort;

    @Value("${jade.container.names}")
    private String containerNames;

    public ContainerConfig(Runtime jadeRuntime) {
        this.jadeRuntime = jadeRuntime;
    }

    @Bean
    public Map<String, AgentContainer> agentContainers() throws ControllerException {
        Map<String, AgentContainer> containers = new HashMap<>();

        String[] names = containerNames.split(",");
        for (String name : names) {
            String trimmedName = name.trim();
            AgentContainer container = createContainer(trimmedName);
            containers.put(trimmedName, container);
        }

        return containers;
    }

    private AgentContainer createContainer(String containerName) throws ControllerException {
        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, mainHost);
        profile.setParameter(Profile.MAIN_PORT, String.valueOf(mainPort));
        profile.setParameter(Profile.CONTAINER_NAME, containerName);

        return jadeRuntime.createAgentContainer(profile);
    }
}
