package com.wcs.mq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MqSender {
	
	@Autowired
	private JmsMessagingTemplate jmsMessagingTemplate;
	 
	public void sendMessage(final String queueName, final String message) {
	   jmsMessagingTemplate.convertAndSend("channel1", message);
	}
	
	public void sendTopic(final String topicName, final String message) {
		jmsMessagingTemplate.convertAndSend(topicName, message);
	}
}
