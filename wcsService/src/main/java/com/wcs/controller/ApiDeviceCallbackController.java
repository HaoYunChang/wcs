package com.wcs.controller;

import java.util.HashMap;
import java.util.Map;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.atop.autoEx.IGoodEndEvent;
import com.wcs.autoEx.service.tally.TallyService;
import com.wcs.dao.ActionLogService;
import com.wcs.dao.LocationService;
import com.wcs.frame.WcsApplicationFrame;
import com.wcs.mq.MqSender;

@RestController
public class ApiDeviceCallbackController {
	
	@Autowired
	MqSender mqSender;
	
	@Autowired
	TallyService tallyService;
	
	@Autowired
	WcsApplicationFrame wcsAppFrame;
	
	@Autowired
	LocationService locationService;
	
	@Autowired
	ActionLogService actionLogService;

	@PostMapping("/device/device/device/htc")
	public String deviceCallback(@RequestBody String result) throws Exception {
		if (result.contains("now_location")) {
			//準時達用
			String nowLocation = result.subSequence(result.indexOf("<now_location>")+14, result.indexOf("</now_location>")).toString();
			String carrierId = result.subSequence(result.indexOf("<carrier_id>")+12, result.indexOf("</carrier_id>")).toString();
			String carId =  result.subSequence(result.indexOf("<car_id>")+8, result.indexOf("</car_id>")).toString();
			String direction =  result.subSequence(result.indexOf("<direction>")+11, result.indexOf("</direction>")).toString();
//			String nowLocation = result.subSequence(result.indexOf("<now_location xmlns=\"\">")+23, result.indexOf("</now_location>")).toString();
//			String carrierId = result.subSequence(result.indexOf("<carrier_id xmlns=\"\">")+21, result.indexOf("</carrier_id>")).toString();

			Map<String,Object> locationInfo = new HashMap<String, Object>();
			locationInfo = actionLogService.getLocation(carrierId);
			if (!nowLocation.contains("BCR") && !nowLocation.contains("STL")) {
				locationService.update((String)locationInfo.get("shelf_num"), direction);
				actionLogService.update((String)locationInfo.get("storage_num"), (String)locationInfo.get("destination"));
				//mqSender.sendTopic("agvArrived", "nowLocation:" + locationInfo.get("destination") + ",shelfId:" + carrierId);
			}else {
				//mqSender.sendTopic("agvArrived", "nowLocation:" + nowLocation + ",shelfId:" + carrierId);
			}
			System.out.println(nowLocation);
			System.out.println(carrierId);
			System.out.println(carId);
			System.out.println(direction);
//			mqSender.sendTopic("agvArrived", "nowLocation:" + nowLocation + ",shelfId:" + carrierId +",carId:"+ carId);
		}
		else if (result.contains("command")) {
			//準時達用
			String seedAddr = result.subSequence(result.indexOf("<command>")+9, result.indexOf("</command>")).toString();
//			String seedAddr = result.subSequence(result.indexOf("<command xmlns=\"\">")+18, result.indexOf("</command>")).toString();
			//,CAP101,1,1,finished
//			System.out.println("seedAddr:"+seedAddr);
			String [] message = seedAddr.substring(8).split(",");
			System.out.println("capsClick:"+message);
			//mqSender.sendTopic("capsClick", message);
//			mqSender.sendTopic("capsClick", String.format("%04d", message[0])+","+message[2]);
		}
		else {
			//mqSender.sendTopic("volumeDetect", result);
			tallyService.setVolumeDetect(result);
			wcsAppFrame.setPackageVolumn(result);
		}

		return "000,OK";
	}

	@GetMapping("/device/shuttle/carrier/{nowLocation}/{carrierId}")
	public String carrierArriveCallbackTrigger(@PathVariable String nowLocation, @PathVariable String carrierId) {
		// API 負責開放Endpoint 給外面溝通用,訊息封包內容與串接由aex layer 負責
		mqSender.sendTopic("agvArrived", "nowLocation:" + nowLocation + ",carrierId:" + carrierId);
		return "agvArrived: nowLocation:" + nowLocation + ",carrierId:" + carrierId;
	}
	
	@GetMapping("/device/volume/{message}")
	public String volumeSettingTrigger(@PathVariable String message) {
		mqSender.sendTopic("volumeDetect", message);
		tallyService.setVolumeDetect(message);
		wcsAppFrame.setPackageVolumn(message);
		return "message以傳送";
	}
	
	@PostMapping(value = "/device/volume/", consumes = "multipart/form-data")
	public String volumeSettingPostTrigger(String message) {
		System.out.println("message:"+message);
		mqSender.sendTopic("volumeDetect", message);
		tallyService.setVolumeDetect(message);
		wcsAppFrame.setPackageVolumn(message);
		return "message以傳送";
	}
	
	@GetMapping("/device/IRReceive")
	public String IrReceiveTrigger() {
		System.out.println("123");
		mqSender.sendTopic("IrReceive", "1");
		return "OK";
	}
}
