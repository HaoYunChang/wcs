package com.wcs.mq;


import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class MqSender {
	
//	@Autowired
//	private JmsMessagingTemplate jmsMessagingTemplate;
	
	@Autowired
	private JmsTemplate jmsMessagingTemplate;
	 
	public void sendMessage(final String queueName, final String message) {
	   jmsMessagingTemplate.convertAndSend("channel1", message);
	}
	
	public void sendTopic(final String topicName, final String message) {
		jmsMessagingTemplate.setDefaultDestinationName(topicName);
		jmsMessagingTemplate.convertAndSend(topicName, message);
	}
	
	public void sendJsonTopic(final String topicName, final JSONObject message) {
		jmsMessagingTemplate.setDefaultDestinationName(topicName);
		jmsMessagingTemplate.convertAndSend(topicName, message);
	}
}
