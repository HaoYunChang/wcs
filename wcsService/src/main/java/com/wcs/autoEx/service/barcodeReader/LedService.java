package com.wcs.autoEx.service.barcodeReader;

import org.springframework.stereotype.Service;

import com.pi4j.io.gpio.*;

@Service
public class LedService {
	
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
	
	private void switched(String light) throws InterruptedException {
		GpioController gpio = GpioFactory.getInstance();
		GpioPinDigitalOutput pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_25, "LED", PinState.HIGH);
		gpio.shutdown();
		gpio.unprovisionPin(pin);
		if(light == "0") {
			while(true) {
				System.out.println("開啟繼電器");
				pin.high();
				Thread.sleep(200);
				System.out.println("關閉繼電器");
				pin.low();
				Thread.sleep(200);
			}
		}
		if (light == "1") {
			System.out.println("關閉燈號");
			pin.low();
		}
		if (light == "2") {
			System.out.println("開啟燈號");
			pin.high();
		}
	} 
	

}
