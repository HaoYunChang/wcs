package com.wcs.controller;

//import javax.annotation.PostConstruct;
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
import com.wcs.mq.MqSender;

@RestController
public class ApiDeviceCallbackController {

	@Autowired
	public EventBus eventBus;
	
	@Autowired
	MqSender mqSender;
	
	@Autowired
	TallyService tallyService;

	@Subscribe
	public void onCommandGoodEnd(IGoodEndEvent event) {

	}

	@PostMapping("/device/device/device/htc")
	public String deviceCallback(@RequestBody String result) throws Exception {
		
		if (result.contains("now_location")) {
			//準時達用
			//String nowLocation = result.subSequence(result.indexOf("<now_location>")+14, result.indexOf("</now_location>")).toString();
			//String carrierId = result.subSequence(result.indexOf("<carrier_id>")+12, result.indexOf("</carrier_id>")).toString();
			String nowLocation = result.subSequence(result.indexOf("<now_location xmlns=\"\">")+23, result.indexOf("</now_location>")).toString();
			String carrierId = result.subSequence(result.indexOf("<carrier_id xmlns=\"\">")+21, result.indexOf("</carrier_id>")).toString();
			//判斷callBack是否抵達站點 分別丟goodEndevent or stateChangeEvent給WMS
			//CarrierArriveEvent carrierArriveEvent = new CarrierArriveEvent(nowLocation, carrierId ,false);
			//this.eventBus.post(carrierArriveEvent);
			mqSender.sendTopic("agvArrived", "nowLocation:" + nowLocation + ",carrierId:" + carrierId);
		}
		else if (result.contains("command")) {
			//準時達用
			//String seedAddr = result.subSequence(result.indexOf("<command>")+9, result.indexOf("</command>")).toString();
			String seedAddr = result.subSequence(result.indexOf("<command xmlns=\"\">")+18, result.indexOf("</command>")).toString();
			//ButtonClickCallBackEvent buttonClickCallBackEvent = new ButtonClickCallBackEvent(seedAddr);
			//this.eventBus.post(buttonClickCallBackEvent);
			mqSender.sendTopic("capsClick", seedAddr);
		}
		else {
			mqSender.sendTopic("volumeDetect", result);
			tallyService.setVolumeDetect(result);
		}

		return "000,OK";
	}

	@GetMapping("/device/shuttle/carrier/{nowLocation}/{carrierId}")
	public String carrierArriveCallbackTrigger(@PathVariable String nowLocation, @PathVariable String carrierId) {
		// API 負責開放Endpoint 給外面溝通用,訊息封包內容與串接由aex layer 負責
		//return super.carrierArriveCallbackTrigger(nowLocation, carrierId); // 將訊息傳至aex layer
		mqSender.sendTopic("agvArrived", "nowLocation:" + nowLocation + ",carrierId:" + carrierId);
		return "agvArrived: nowLocation:" + nowLocation + ",carrierId:" + carrierId;
	}
	
	@GetMapping("/device/volume/{message}")
	public String volumeSettingTrigger(@PathVariable String message) {
		mqSender.sendTopic("volumeDetect", message);
		tallyService.setVolumeDetect(message);
		
		return "message以傳送";
	}
}
