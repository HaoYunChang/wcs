package com.wcs.mq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.wcs.autoEx.ErrorMessage;
import com.wcs.autoEx.service.tally.TallyService;
import com.wcs.frame.WcsApplicationFrame;

@Component
public class MqListener {
	@Autowired
	WcsApplicationFrame wcsAppFrame;
	
	@JmsListener(destination="capsClick")
	public void readCaps(String message) {
		System.out.println("capsClick:"+message);
	}
	
	@JmsListener(destination="agvArrived")
	public void readAgv(String message) {
		System.out.println("agvArrived:"+message);
	}
	
	@JmsListener(destination="volumeDetect") //材積辨識
	public void readVolume(String message) {
		System.out.println("volumeDetect:"+message);
	}
	
	@JmsListener(destination="IrReceive")
	public void readIRMessage(String message) {
		System.out.println("IRStatus:"+message);
		if (message.equals("1"))
			TallyService.irStatus = true;
		else
			TallyService.irStatus = false;
	}
	
	@JmsListener(destination="ArmStatus")
	public void readArmMessage(String message) {
		System.out.println("ArmStatus:"+message);

	}
	
	@JmsListener(destination="ArmFinish")
	public void readArmFinishMessage(String message) {
		System.out.println("ArmFinish:"+message);
	}
	
	@JmsListener(destination="labelError")
	public void readLabelError(String message){
		System.out.println("LabelError");
		wcsAppFrame.setError("label", ErrorMessage.LABEL_ERROR.value(), "set");
	}
	
}
