package com.catify.processengine.core.nodes;

import java.util.Date;
import java.util.List;

import akka.actor.ActorRef;

import com.catify.processengine.core.data.dataobjects.DataObjectService;
import com.catify.processengine.core.data.model.NodeInstaceStates;
import com.catify.processengine.core.messages.ActivationMessage;
import com.catify.processengine.core.messages.DeactivationMessage;
import com.catify.processengine.core.messages.TriggerMessage;
import com.catify.processengine.core.nodes.eventdefinition.EventDefinition;
import com.catify.processengine.core.nodes.eventdefinition.MessageEventDefinition_Catch;
import com.catify.processengine.core.processdefinition.jaxb.TMessageIntegration;
import com.catify.processengine.core.services.NodeInstanceMediatorService;

public class ReceiveTaskNode extends Task {

	private EventDefinition messageEventDefinitionCatch;
	
	public ReceiveTaskNode() {

	}
	
	/**
	 * Instantiates a new receive task node.
	 * 
	 * @param uniqueProcessId
	 *            the process id
	 * @param uniqueFlowNodeId
	 *            the unique flow node id
	 * @param outgoingNodes
	 *            the outgoing nodes
	 */
	public ReceiveTaskNode(String uniqueProcessId, String uniqueFlowNodeId,
			List<ActorRef> outgoingNodes, String actorRefString,
			TMessageIntegration messageIntegration, DataObjectService dataObjectHandling) {
		this.setUniqueProcessId(uniqueProcessId);
		this.setUniqueFlowNodeId(uniqueFlowNodeId);
		this.setOutgoingNodes(outgoingNodes);
		this.setNodeInstanceMediatorService(new NodeInstanceMediatorService(
				uniqueProcessId, uniqueFlowNodeId));
		this.messageEventDefinitionCatch = new MessageEventDefinition_Catch(uniqueProcessId, 
				uniqueFlowNodeId, actorRefString,
				messageIntegration);
		this.setDataObjectHandling(dataObjectHandling);
	}
	
	@Override
	protected void activate(ActivationMessage message) {
		this.getNodeInstanceMediatorService().setState(
				message.getProcessInstanceId(), NodeInstaceStates.ACTIVE_STATE);
		
		messageEventDefinitionCatch.acitivate(message);
		
		this.getNodeInstanceMediatorService().setNodeInstanceStartTime(message.getProcessInstanceId(), new Date());
		
		this.getNodeInstanceMediatorService().persistChanges();
	}

	@Override
	protected void deactivate(DeactivationMessage message) {
		messageEventDefinitionCatch.deactivate(message);
		
		this.getNodeInstanceMediatorService().setNodeInstanceEndTime(message.getProcessInstanceId(), new Date());
		
		this.getNodeInstanceMediatorService().setState(
				message.getProcessInstanceId(),
				NodeInstaceStates.DEACTIVATED_STATE);
		
		this.getNodeInstanceMediatorService().persistChanges();
	}

	@Override
	protected void trigger(TriggerMessage message) {
		this.getDataObjectHandling().saveObject(this.getUniqueProcessId(), message.getProcessInstanceId(), message.getPayload());
		
		messageEventDefinitionCatch.trigger(message);
		
		this.getNodeInstanceMediatorService().setNodeInstanceEndTime(message.getProcessInstanceId(), new Date());
		
		this.getNodeInstanceMediatorService().setState(
				message.getProcessInstanceId(), NodeInstaceStates.PASSED_STATE);
		
		this.getNodeInstanceMediatorService().persistChanges();
		
		this.sendMessageToNodeActors(
				new ActivationMessage(message.getProcessInstanceId()),
				this.getOutgoingNodes());
	}
}