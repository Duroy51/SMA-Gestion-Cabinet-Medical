package com.example.demo.agent.base;

import jade.core.Agent;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;



public abstract class AbstractAgent extends Agent implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    protected static ApplicationContext getContext() {
        return context;
    }

    protected <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    protected Object getBean(String beanName) {
        return context.getBean(beanName);
    }
}