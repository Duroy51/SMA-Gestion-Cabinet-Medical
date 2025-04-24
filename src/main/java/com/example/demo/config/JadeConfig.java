package com.example.demo.config;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.ControllerException;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


@Configuration
@PropertySource("classpath:application.properties")
public class JadeConfig {

    @Value("${jade.main.host}")
    private String mainHost;

    @Value("${jade.main.port}")
    private int mainPort;

    @Value("${jade.platform.id}")
    private String platformId;

    @Value("${jade.gui:false}")
    private boolean useGUI;

    @Bean(name = "jadeRuntime")
    public Runtime jadeRuntime() {
        return Runtime.instance();
    }

    /*@Bean(name = "mainContainer")
    public AgentContainer mainContainer() throws ControllerException {
        Profile mainProfile = createMainProfile();
        return jadeRuntime().createMainContainer(mainProfile);
    }*/

    private Profile createMainProfile(){
        ProfileImpl profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, mainHost);
        profile.setParameter(Profile.MAIN_PORT, String.valueOf(mainPort));
        profile.setParameter(Profile.PLATFORM_ID, platformId);

        if (useGUI) {
            profile.setParameter(Profile.GUI, "true");
        }

        return profile;
    }
}
