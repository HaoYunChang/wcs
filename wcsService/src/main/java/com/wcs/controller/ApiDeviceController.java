package com.wcs.controller;

import java.util.ArrayList;
import java.util.List;
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
	WcsApplicationFrame wcsAppFrame;
	
	@GetMapping("/shuttle/{s}/{d}")
	public String shuttleTrigger(@PathVariable String s, @PathVariable String d)
			throws Exception {
		IGoCommand goCmd = new GoCommand(s, d, "GO");
		String result = shuttleDeviceService.onGoCmd(goCmd);
		return result;
	}
	
	@GetMapping("/shuttle/getCarStatus/{carId}")
	public String getAgvStatusTrigger(@PathVariable String carId) {
		SettingAgvCommand sac = new SettingAgvCommand(carId, 0, 0, "get_equipment_status");
		String result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}
	
	@GetMapping("/shuttle/setAgvCharge/{carId}")
	public String setAgvChargeTrigger(@PathVariable String carId) {
		SettingAgvCommand sac = new SettingAgvCommand(carId, 0, 0, "set_equipment_status");
		String result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}
	
	@GetMapping("/shuttle/setChargePower/{power}")
	public String setChargePowerTrigger(@PathVariable String power) {
		String[] powerList = power.split(",");
		SettingAgvCommand sac = new SettingAgvCommand("", Integer.parseInt(powerList[0]),
				Integer.parseInt(powerList[1]), "ChangeChargingPower");
		String result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}
	
	@GetMapping("/shuttle/setAgvPark/")
	public String setAgvParkTrigger() {
		SettingAgvCommand sac = new SettingAgvCommand("", 0, 0, "Parking");
		String result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}
	
	@GetMapping("/shuttle/getStoreStatus/")
	public String getStoreStatusTrigger() {
		SettingAgvCommand sac = new SettingAgvCommand("", 0, 0, "get_carrier_store_location");
		String result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}
	
	@GetMapping("/shuttle/getStoreStatus/{shelf_num}")
	public String getStoreStatusSingleTrigger(@PathVariable String shelf_num) {
		SettingAgvCommand sac = new SettingAgvCommand(shelf_num, 0, 0, "get_carrier_store_location");
		String result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}
	
	@GetMapping("/shuttle/updAvailableQty/{num}")
	public String updateAvailableQty(@PathVariable String num) {
		SettingAgvCommand sac = new SettingAgvCommand(num, 0, 0, "UpdateAvailableQty");
		String result = shuttleDeviceService.onSetCmd(sac);
		return result;
	}

    @GetMapping("/caps/seed/{address}/{color}/{num}")
    public String capsSeedTrigger(@PathVariable String address, @PathVariable String color, @PathVariable String num) throws Exception {
//    	System.out.println("Caps "+address+"color "+color +" num "+ num + "Seeding");
    	String[] addrArr = address.split(",");
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
    	//wcsAppFrame.addPackage();
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
}
