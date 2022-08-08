package com.wcs.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.wcs.autoEx.service.agv.ShuttleDeviceService;
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
	
	@Autowired
	ShuttleDeviceService shuttleService;

	@PostMapping("/device/device/device/htc")
	public String deviceCallback(@RequestBody String result) throws Exception {
		if (result.contains("now_location")) {
			try {
				//測試用
	//            String nowLocation = result.subSequence(result.indexOf("<now_location xmlns=\"\">")+23, result.indexOf("</now_location>")).toString();
	//            String carrierId = result.subSequence(result.indexOf("<carrier_id xmlns=\"\">")+21, result.indexOf("</carrier_id>")).toString();
				//準時達用
				String nowLocation = result.subSequence(result.indexOf("<now_location>")+14, result.indexOf("</now_location>")).toString();
				String carrierId = result.subSequence(result.indexOf("<carrier_id>")+12, result.indexOf("</carrier_id>")).toString();
//				String carId =  result.subSequence(result.indexOf("<car_id>")+8, result.indexOf("</car_id>")).toString();
				String direction =  result.subSequence(result.indexOf("<direction>")+11, result.indexOf("</direction>")).toString();
	
				Map<String,Object> locationInfo = new HashMap<String, Object>();
				locationInfo = actionLogService.getLocation(carrierId);
				if (!nowLocation.contains("BCR") && !nowLocation.contains("STL")) {
					//測試用
	//				locationService.update((String)locationInfo.get("shelf_num"), "D");
					//準時答用
					if (nowLocation.equals("CAP101")) {
						System.out.println("shuttleCallback"+shuttleService);
						shuttleService.carQueue.poll();
						locationService.update((String)locationInfo.get("shelf_num"), direction);
					} else {
						Map<String,Object> capInfo = locationService.getLocation("CAP101");
						if (capInfo.get("shelf_num").equals(carrierId))
							locationService.update("", "");
					}
					actionLogService.update((String)locationInfo.get("storage_num"), (String)locationInfo.get("destination"));
					if (locationInfo.get("command").equals("I"))
						mqSender.sendTopic("agvArrived", "nowLocation:" + locationInfo.get("destination") + ",shelfId:" + carrierId +",source:CAP101");
					else
						mqSender.sendTopic("agvArrived", "nowLocation:" + locationInfo.get("destination") + ",shelfId:" + carrierId +",source:" + locationInfo.get("storage_num"));
				}
			} catch(Exception e) {
				System.out.println("agv callBack錯誤:"+e.getStackTrace());
			}
		}
		else if (result.contains("command")) {
			//測試用
//            String seedAddr = result.subSequence(result.indexOf("<command xmlns=\"\">")+18, result.indexOf("</command>")).toString();
//            String message = seedAddr.substring(7,9);
//            mqSender.sendTopic("capsClick", String.format("%04d", Integer.valueOf(message)));
            //準時達用
			String seedAddr = result.subSequence(result.indexOf("<command>")+9, result.indexOf("</command>")).toString();
			String [] message = seedAddr.substring(8).split(",");
			mqSender.sendTopic("capsClick", String.format("%02d", Integer.valueOf(message[0])));
		}
		else {
			mqSender.sendTopic("volumeDetect", result);
			tallyService.setVolumeDetect(result);
			wcsAppFrame.setPackageVolumn(result);
		}

		return "000,OK";
	}
	
	@GetMapping("/device/volume/{message}")
	public String volumeSettingTrigger(@PathVariable String message) {
		mqSender.sendTopic("volumeDetect", message);
		tallyService.setVolumeDetect(message);
		wcsAppFrame.setPackageVolumn(message);
		return "message已傳送";
	}
	
	@PostMapping(value = "/device/volume/", consumes = "multipart/form-data")
	public String volumeSettingPostTrigger(String message) {
		System.out.println("message:"+message);
		mqSender.sendTopic("volumeDetect", message);
		tallyService.setVolumeDetect(message);
		wcsAppFrame.setPackageVolumn(message);
		return "message已傳送";
	}
	
	@GetMapping("/device/IRReceive")
	public String IrReceiveTrigger() {
		System.out.println("123");
		mqSender.sendTopic("IrReceive", "1");
		return "OK";
	}
}
