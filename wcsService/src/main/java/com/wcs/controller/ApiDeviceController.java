package com.wcs.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atop.autoEx.cAPS.CAPSSeed;
import com.atop.autoEx.cAPS.SeedCommand;
import com.atop.autoEx.cAPS.LightColorEnum;
import com.atop.autoEx.shuttle.GoCommand;
import com.atop.autoEx.shuttle.IGoCommand;
import com.atop.autoEx.shuttle.IShuttleCommand;
import com.atop.autoEx.shuttle.PullCommand;
import com.wcs.autoEx.PackageBox;
import com.wcs.autoEx.service.agv.ShuttleDeviceService;
import com.wcs.autoEx.service.caps.CapsService;
import com.wcs.autoEx.service.tally.TallyService;
import com.wcs.dao.DbService;
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
	
	@GetMapping("/shuttle/{s}/{d}")
	public String shuttleTrigger(@PathVariable String s, @PathVariable String d)
			throws Exception {
		IGoCommand goCmd = new GoCommand(s, d, "123");
		shuttleDeviceService.onGoCmd(goCmd);
		return "shuttle: go" + s + "to" + d + "runId:" + goCmd.getId();
	}

    @GetMapping("/caps/seed/{address}/{color}/{num}")
    public String capsSeedTrigger(@PathVariable String address, @PathVariable String color, @PathVariable String num) throws Exception {
    	System.out.println("Caps "+address+"color "+color +" num "+ num + "Seeding");
    	//CAPSSeed seed0002 = new CAPSSeed(address, LightColorEnum.RED, num, null);
    	String[] addrArr = address.split(",");
    	String[] numArr = num.split(",");
    	List<CAPSSeed> capsArr = new ArrayList<CAPSSeed>();
    	for (int i=0;i<addrArr.length;i++) {
    		if (color.equals("RED"))
    			capsArr.add(new CAPSSeed(addrArr[i], LightColorEnum.RED, Integer.parseInt(numArr[i]), null));
    		else
    			capsArr.add(new CAPSSeed(addrArr[i], LightColorEnum.GREEN, Integer.parseInt(numArr[i]), null)); 
    		
    	}
    	//SeedCommand sc= new SeedCommand("101", Arrays.asList( new CAPSSeed[] {seed0001, seed0003, seed0005}), null);
    	SeedCommand sc= new SeedCommand("101", capsArr, null);
    	//capsService.asyncExeCmd(sc);
    	capsService.onSeedCmd(sc);
//        cAPSService!!.doSomething()
        return "Caps "+address+"color "+color +" num "+ num + "Seeding";
    }
    
    @GetMapping("/outbound/package/{orderId}")
    public String receiveOutboundOrder(@PathVariable String orderId) {
    	System.out.println(orderId);
    	PackageBox pb = new PackageBox();
    	pb.setOrderId(orderId);
    	tallyService.setPackageBox(pb);
    	return "orderId:"+orderId+"is received";
    }
    
    @GetMapping("/outbound/barcode/{barcodeId}")
    public String receiveOutboundBarcode(@PathVariable String barcodeId) {
    	System.out.println(barcodeId);
    	PackageBox pb = new PackageBox();
    	pb.setOrderId(barcodeId);
    	tallyService.setPackageBox(pb);
    	return "orderId:"+barcodeId+"is received";
    }
}
