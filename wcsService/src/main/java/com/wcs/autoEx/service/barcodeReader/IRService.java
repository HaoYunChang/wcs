package com.wcs.autoEx.service.barcodeReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.wcs.mq.MqSender;

@Service
public class IRService implements Runnable {
	String root="";
	String path="object/eye/0c22ab5de7de410ab8e077162afd5b19";
	
	@Autowired
	MqSender mqSender;
	
//	@PostConstruct
//	private void init() {
//		String inputFileName = "aex_setting.json";
//		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
//		InputStream inputStream = classloader.getResourceAsStream(inputFileName);
//
//		try {
//			InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
//			BufferedReader reader = new BufferedReader(isr);
//
//			StringBuilder resultStringBuilder = new StringBuilder();
//			String aexSetting;
//			while ((aexSetting = reader.readLine()) != null) {
////				System.out.println(aexSetting);
//				resultStringBuilder.append(aexSetting).append("\n");
//			}
//			
//			JSONObject dc_setting_obj = new JSONObject(resultStringBuilder.toString());
//			
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		GpioController gpio = GpioFactory.getInstance();
		GpioPinDigitalInput myButton = gpio.provisionDigitalInputPin(RaspiPin.GPIO_02, PinPullResistance.PULL_DOWN);
		myButton.setShutdownOptions(true);
		myButton.addListener(new GpioPinListenerDigital() { 
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				// TODO Auto-generated method stub
                if(event.getState().isHigh()){
                    System.out.println("物品通過");
                	mqSender.sendMessage("irDetect", "0");
                  }
                  else{
                	System.out.println("物品離開");
                    mqSender.sendMessage("irDetect", "1");
                  }
			}
		});
		
	}
}
