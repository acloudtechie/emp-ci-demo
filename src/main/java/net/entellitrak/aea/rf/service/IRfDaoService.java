package net.entellitrak.aea.rf.service;

import net.entellitrak.aea.rf.dao.IRfWorkflow;

/**
 * This class is the service class for the Rules Framework DAO classes. It provides methods for retrieving instances of
 * DAOs.
 *
 * @author zachary.miller
 */
public interface IRfDaoService {

    /**
     * Get an RF Workflow DAO object given its Code.
     *
     * @param workflowCode The Code of the RF Workflow object.
     * @return The RF Workflow specified by workflowCode
     * @throws Exception If anything went wrong.
     */
    IRfWorkflow loadRfWorkflowByCode(String workflowCode) throws Exception;
}
