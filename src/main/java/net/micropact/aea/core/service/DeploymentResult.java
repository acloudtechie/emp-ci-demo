package net.micropact.aea.core.service;

import java.util.ArrayList;
import java.util.List;

import net.entellitrak.aea.core.service.IDeploymentResult;
import net.micropact.aea.core.utility.StringUtils;

/**
 * Implementation of the public {@link IDeploymentResult} interface.
 *
 * This class is mutable. Messages should be added using the {@link #addMessage(String)} method
 * and then at the end, {@link #getSummaryString()} should be called to get a newline-separated list of messages.
 *
 * @author Zachary.Miller
 */
public class DeploymentResult implements IDeploymentResult {

    private final List<String> messages;

    /**
     * Constructor.
     */
    public DeploymentResult(){
        messages = new ArrayList<>();
    }

    /**
     * Add a new message.
     *
     * @param message the message
     */
    public void addMessage(final String message){
        messages.add(message);
    }

    @Override
    public String getSummaryString() {
        return StringUtils.join(messages, "\n");
    }
}
