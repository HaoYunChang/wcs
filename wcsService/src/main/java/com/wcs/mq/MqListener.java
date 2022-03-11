package com.wcs.mq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.wcs.autoEx.service.tally.TallyService;

@Component
public class MqListener {
	
	@Autowired
	TallyService tallyService;
	
	@JmsListener(destination="channel1", containerFactory="queueListener")
    public void readActiveQueue(String message) {
        System.out.println("receive" + message);
    }
	
	@JmsListener(destination="capsClick", containerFactory="queueListener")
	public void readCaps(String message) {
		System.out.println("capsClick:"+message);
	}
	
	@JmsListener(destination="agvArrived", containerFactory="queueListener")
	public void readAgv(String message) {
		System.out.println("agvArrived:"+message);
	}
	
	@JmsListener(destination="volumeDetect", containerFactory="queueListener")
	public void readVolume(String message) {
		System.out.println("volumeDetect:"+message);
		tallyService.irStatus = true;
	}
	
	@JmsListener(destination="IrReceive", containerFactory="queueListener")
	public void readIRMessage(String message) {
		System.out.println("IRStatus:"+message);
	}
	
	@JmsListener(destination="ArmStatus", containerFactory="queueListener")
	public void readArmMessage(String message) {
		System.out.println("ArmStatus:"+message);
		tallyService.sendFinish();
	}
}
