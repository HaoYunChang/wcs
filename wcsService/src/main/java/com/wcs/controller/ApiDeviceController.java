package com.wcs.controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.atop.autoEx.cAPS.CAPSSeed;
import com.atop.autoEx.cAPS.SeedCommand;
import com.atop.autoEx.cAPS.LightColorEnum;
import com.atop.autoEx.shuttle.GoCommand;
import com.atop.autoEx.shuttle.IGoCommand;
import com.atop.autoEx.shuttle.SettingAgvCommand;
import com.wcs.autoEx.PackageBox;
import com.wcs.autoEx.service.agv.ShuttleDeviceService;
import com.wcs.autoEx.service.caps.CapsService;
import com.wcs.autoEx.service.tally.TallyService;
import com.wcs.dao.ActionLogService;
import com.wcs.dao.DbService;
import com.wcs.frame.WcsApplicationFrame;
import com.wcs.mq.MqSender;

@RestController
@RequestMapping(value = "/autoDevice/" )
public class ApiDeviceController {
	@Autowired
	DbService dbService;
	
	@Autowired
	MqSender mqSender;
	
	@Autowired
	private ShuttleDeviceService shuttleDeviceService;
	
	@Autowired
	private CapsService capsService;
	
	@Autowired
	private TallyService tallyService;
	
	@Autowired
	private ActionLogService actionLogService;
	
	@Autowired
	WcsApplicationFrame wcsAppFrame;
	
	@GetMapping("/shuttle/{s}/{d}")
	public Map<String, String> shuttleTrigger(@PathVariable String s, @PathVariable String d)
			throws Exception {
		IGoCommand goCmd = new GoCommand(s, d, "GO");
		System.out.println("shuttleServiceAPI:"+shuttleDeviceService);
		Map<String, String> result = shuttleDeviceService.onGoCmd(goCmd);
		result.put("url", "http://192.168.100.102:9102/wcs_api/autoDevice/shuttle/"+s+"/"+d);
		return result;
	}
	
	@GetMapping("/shuttle/getCarStatus/")
	public Map<String, Object> getAllAgvStatus() {
		JSONArray idleArray = new JSONArray();
		JSONArray operatingArray = new JSONArray();
		JSONObject json = new JSONObject();
		
		//測試用
//		int random = (int)(Math.random() * 4);
//		Map<String, String> hash = new HashMap<String, String>();
//		switch (random) {
//			case 0:{
//				for (int i=1;i<3;i++) {
//					hash.put("carId", "22"+i);
//					hash.put("status", "忙碌中");
//					operatingArray.put(hash);
//				}
//				break;
//			}
//			case 1:{
//				hash.put("carId", "221");
//				hash.put("status", "閒置中");
//				idleArray.put(hash);
//				hash.clear();
//				hash.put("carId", "222");
//				hash.put("status", "忙碌中");
//				operatingArray.put(hash);
//				hash.clear();
//				break;
//			}
//			case 2:{
//				hash.put("carId", "222");
//				hash.put("status", "閒置中");
//				idleArray.put(hash);
//				hash.clear();
//				hash.put("carId", "221");
//				hash.put("status", "忙碌中");
//				operatingArray.put(hash);
//				hash.clear();
//				break;
//			}
//			case 3:{
//				for (int i=1;i<3;i++) {
//					hash.put("carId", "22"+i);
//					hash.put("status", "閒置中");
//					idleArray.put(hash);
//				}
//				break;
//			}
//		}
		
		//現場用
		for(int i=1;i<3;i++) {
			Map<String, String> hash = new HashMap<String, String>();
			SettingAgvCommand sac = new SettingAgvCommand("22"+i, 0, 0, "get_equipment_status");
			String result = shuttleDeviceService.onSetCmd(sac).get("message").substring(4);
			hash.put("carId", "22"+i);
			hash.put("status", result);
			if(result.equals("閒置中")) 
				idleArray.put(hash);
			else
				operatingArray.put(hash);
		}
		json.put("idleCar", idleArray);
		json.put("operatingCar", operatingArray);
		return json.toMap();
	}
	
	@GetMapping("/shuttle/getCarStatus/{carId}")
	public Map<String, String> getAgvStatusTrigger(@PathVariable String carId) {
		SettingAgvCommand sac = new SettingAgvCommand(carId, 0, 0, "get_equipment_status");
		Map<String, String> result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}
	
	@GetMapping("/shuttle/setAgvCharge/{carId}")
	public Map<String, String> setAgvChargeTrigger(@PathVariable String carId) {
		SettingAgvCommand sac = new SettingAgvCommand(carId, 0, 0, "set_equipment_status");
		Map<String, String> result = shuttleDeviceService.onSetCmd(sac);
		return result;
//		return "OK";
	}
	
	@GetMapping("/shuttle/setChargePower/{power}")
	public Map<String, String> setChargePowerTrigger(@PathVariable String power) {
		String[] powerList = power.split(",");
		SettingAgvCommand sac = new SettingAgvCommand("", Integer.parseInt(powerList[0]),
				Integer.parseInt(powerList[1]), "ChangeChargingPower");
		Map<String, String> result = shuttleDeviceService.onSetCmd(sac);
		return result;
//		return "OK";
	}
	
	@GetMapping("/shuttle/setAgvPark/")
	public Map<String, String> setAgvParkTrigger() {
		SettingAgvCommand sac = new SettingAgvCommand("", 0, 0, "Parking");
		Map<String, String> result = shuttleDeviceService.onSetCmd(sac);
		return result;
//		return "OK";
	}
	
	@GetMapping("/shuttle/getStoreStatus/")
	public Map<String, String> getStoreStatusTrigger() {
		SettingAgvCommand sac = new SettingAgvCommand("", 0, 0, "get_carrier_store_location");
		Map<String, String> result = shuttleDeviceService.onSetCmd(sac);
		return result;
//		return "OK";
	}
	
	@GetMapping("/shuttle/getStoreStatus/{shelf_num}")
	public Map<String, String> getStoreStatusSingleTrigger(@PathVariable String shelf_num) {
		SettingAgvCommand sac = new SettingAgvCommand(shelf_num, 0, 0, "get_carrier_store_location");
		Map<String, String> result = shuttleDeviceService.onSetCmd(sac);
		return result;
//		return "OK";
	}
	
	@GetMapping("/shuttle/updAvailableQty/{num}")
	public Map<String, String> updateAvailableQty(@PathVariable String num) {
		SettingAgvCommand sac = new SettingAgvCommand(num, 0, 0, "UpdateAvailableQty");
		Map<String, String> result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}

    @GetMapping("/caps/seed/{address}/{color}/{num}")
    public String capsSeedTrigger(@PathVariable String address, @PathVariable String color, @PathVariable String num) throws Exception {
    	String[] addrArr = address.split(",");
    	for (int i=0; i<addrArr.length; i++) {
    		addrArr[i] = String.format("%04d", Integer.valueOf(addrArr[i]));
    	}
    	String[] numArr = num.split(",");
    	List<CAPSSeed> capsArr = new ArrayList<CAPSSeed>();
    	for (int i=0;i<addrArr.length;i++) {
    		if (color.equals("RED"))
    			capsArr.add(new CAPSSeed(addrArr[i], LightColorEnum.RED, Integer.parseInt(numArr[i]), null));
    		else
    			capsArr.add(new CAPSSeed(addrArr[i], LightColorEnum.GREEN, Integer.parseInt(numArr[i]), null)); 
    		
    	}
    	SeedCommand sc= new SeedCommand("101", capsArr, null);
    	capsService.onSeedCmd(sc);
        return "Caps "+address+"color "+color +" num "+ num + "Seeding";
    }
    
    @GetMapping("/outbound/package/{orderId}")
    public String receiveOutboundOrder(@PathVariable String orderId) {
    	PackageBox pb = new PackageBox();
    	pb.setOrderId(orderId);
    	pb.setPackageStatus("理貨完成");
    	tallyService.setPackageBox(pb);
    	wcsAppFrame.addPackage(pb);
    	return "orderId:"+orderId+"is received";
    }
    
    @PostMapping(value = "/outbound/package", consumes = "multipart/form-data")
    public String receivePackage(PackageBox packageBox) {
    	if (packageBox.consignNumber.isBlank() || packageBox.orderId.isBlank())
    		return "包裹無訂單邊號/託運單號";
    	if (!packageBox.receiveDate.isEmpty()) {
    		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    		try {
    			df.parse(packageBox.receiveDate);
    		}catch(Exception e) {
    			return "包裹日期格式不對，為yyyy-MM-dd";
    		}
    	} else
    		return "包裹收件日期為空";
    	packageBox.setPackageStatus("理貨完成");
    	tallyService.setPackageBox(packageBox);
    	wcsAppFrame.addPackage(packageBox);
    	return "package received";
    }
    
    @GetMapping("/outbound/barcode/{barcodeId}")
    public String receiveOutboundBarcode(@PathVariable String barcodeId) {
    	System.out.println(barcodeId);
    	PackageBox pb = new PackageBox();
    	pb.setOrderId(barcodeId);
    	pb.setPackageStatus("理貨完成");
    	tallyService.setPackageBox(pb);
    	wcsAppFrame.addPackage(pb);
    	return "orderId:"+barcodeId+"is received";
    }
    
    @GetMapping("/cab/print/")
    public String printCab() {
    	tallyService.soapTransmit(tallyService.fileToBase64("C:\\Users\\ITRI2021\\Downloads\\testAddText.pdf"));
    	return "cab doing";
    }
    
    @GetMapping("/outbound/queueClear/")
    public String queueClear() {
    	tallyService.queue2.clear();
    	tallyService.queue3.clear();
    	return "queue is clear";
    }
    
    @GetMapping("/actionLog/clear")
    public Map<String, String> actionLogClean() {
    	int a = actionLogService.delete();
    	Map<String, String> result = new HashMap<String, String>();
    	if (a == 0)
    		result.put("status", "OK");
    	else 
    		result.put("status", "fail");
    	return result;
    }
    
    @GetMapping("/allDevice/reset")
    public Map<String, Object> clearAllDevice(){
	    Map<String, Object> result = new HashMap<String, Object>();
	    Map<String, String> tallyResult = new HashMap<String, String>();
	    
	    try {
	    	tallyService.reset();
	    	tallyResult.put("status", "OK");
	    	tallyResult.put("message", "allReset");
	    }catch (Exception e) {
	    	tallyResult.put("status", "fail");
	    	tallyResult.put("message", e.getLocalizedMessage());
	    }
	    result.put("tallyReset", tallyResult);
	    
	    try {
	    	shuttleDeviceService.reset();
	    }catch(Exception e) {
	    	
	    }
    	return result;
    }
}