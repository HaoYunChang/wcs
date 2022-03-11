package com.wcs.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.wcs.dao.DbService;
import com.wcs.mq.MqSender;

@RestController
@RequestMapping(value = "/api/test" )
public class ApiTestController {
	
	@Autowired
	DbService dbService;
	
	
	@Autowired
	MqSender mqSender;
	
	@GetMapping(value = "/getCustomer" ,produces="application/json; charset=utf-8")
	public @ResponseBody Object getCustomer(
			@RequestParam (name="customerId") Long customerId) {
		return dbService.getCustomer(customerId);
	}

	@PostMapping(value = "/updateCustomer")
	@ResponseBody
    public Map<String, Object> updateCustomer(
    		@RequestBody Map<?,?> dataMap) {
		
		long customerId = dataMap.get("customerId")!=null?Long.parseLong(dataMap.get("customerId").toString()):0;
		dbService.updateCustomer(customerId);
		Map<String, Object> rs= new HashMap<>();
		rs.put("rsMsg", "success");
		mqSender.sendMessage("channel1", "post data:" + customerId );
        return rs;
    }

}