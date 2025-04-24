package com.example.demo.agent;

import com.example.demo.agent.base.AbstractAgent;

public class AgentExample extends AbstractAgent {

    @Override
    public void setup() {
        System.out.println("Agent " + getLocalName() + " started.");
    }
}
