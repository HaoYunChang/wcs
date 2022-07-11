package com.wcs.autoEx.service.agv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.greenrobot.eventbus.Subscribe;
import com.atop.autoEx.INormalEndEvent;
import com.atop.autoEx.IStateChangeEvent;
import com.atop.autoEx.ITimeoutEvent;
import com.atop.autoEx.shuttle.IGoCommand;
import com.atop.autoEx.shuttle.IShuttleCommand;
import com.atop.autoEx.shuttle.SettingAgvCommand;
import com.wcs.autoEx.AexServiceBase;
import com.wcs.dao.ActionLogService;
import com.wcs.dao.LocationService;
import com.wcs.mq.MqSender;

//import jusda.autoEx.shuttle.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Scope("singleton")
public class ShuttleDeviceService extends AexServiceBase {

	private Map<String, IGoCommand> goCommandExecMap = new HashMap<String, IGoCommand>();
	
	@Autowired 
	LocationService locationService;
	
	@Autowired
	ActionLogService actionLogService;
	
	@Autowired
	MqSender mqSender;

	@Subscribe
	@Override
	public void onCommandStateChange(IStateChangeEvent event) {
	}

	@Subscribe
	@Override
	public void onCommandTimeOut(ITimeoutEvent event) {
		if (event.getCommand() instanceof IShuttleCommand) {
			IShuttleCommand cmd = (IShuttleCommand) event.getCommand();
			if (cmd instanceof IGoCommand) {
				System.out.println("ShuttleService onCommandTimeOut 箱子:" + ((IGoCommand) cmd).getCarrier() + "逾期未到達");
			} else {
				System.out.println("ShuttleService onCommandTimeOut commandId:" + event.getCommand().getId());
			}
		}
	}

	@Subscribe
	@Override
	public void onCommandNormalEnd(INormalEndEvent event) {
		if (event.getCommand() instanceof IShuttleCommand) {
		}
	}
	
	public String onGoCmd(IGoCommand cmd) {
		Map<String, Object> storageInfo = new HashMap<String, Object>();
		Map<String, Object> capsInfo = new HashMap<String, Object>();
		String result="";
		try {
			capsInfo = locationService.getLocation("CAP101");
			if (cmd.getSourceAddress().equals("CAP101")) {
				storageInfo = locationService.getLocation(cmd.getDestinationAddress());
				if (!storageInfo.get("shelf_num").equals(capsInfo.get("shelf_num"))) {
					return "010,回去的儲位與貨架編號不符";
				}
			} else
				storageInfo = locationService.getLocation(cmd.getSourceAddress());
			
			
			String carrierId = (String) storageInfo.get("shelf_num");
			String direction = (String) storageInfo.get("direction");
			String testLoc = (String) storageInfo.get("test_location");
			String shelfLocation = (String) storageInfo.get("shelf_location");
			cmd.setCarrier(carrierId);
			this.goCommandExecMap.put(carrierId, cmd);
			String command = cmd.getDestinationAddress().equals("CAP101")? "O" : "I";
			if (command == "O") {
				if (cmd.getSourceAddress().equals("CAP101")) {
					command = "T";
					direction = direction.equals("A")? "B" : "A";
				}	
			}	
			
			if (cmd.getDestinationAddress().equals("CAP101")) {
				result = sendRequestToIIs("set_carrier_next_location", List.of(cmd.getId().toString(), cmd.getDestinationAddress(), cmd.getCarrier(), direction, command));
				//result = test("set_carrier_next_location", List.of(cmd.getDestinationAddress(), cmd.getCarrier(), "false"));
			} else {
				result = sendRequestToIIs("set_carrier_next_location", List.of(cmd.getId().toString(), shelfLocation, cmd.getCarrier(), direction, command));
				//result = test("set_carrier_next_location", List.of(testLoc, cmd.getCarrier(), "false"));
			}
			//mqSender.sendTopic("agvArrived", result);
			if (result.contains("000")) {
				//新增action_log
				int saveResult = actionLogService.save((String)storageInfo.get("storage_num"), cmd.getDestinationAddress(), command);
				
			}
		}catch (Exception e) {
			result = e.getLocalizedMessage();
		}
		return result;
	}
	
	public String onSetCmd(SettingAgvCommand sc) {
		String result = "";
		String tag = (String) sc.get_tag();
		List<String> soapParameter = new ArrayList<String>();
		switch (tag) {
			case "get_equipment_status", "set_equipment_status": {
				soapParameter = Arrays.asList(sc.getCarId());
				break;
			}
			case "UpdateAvailableQty": {//number
				soapParameter = Arrays.asList(sc.getCarId());
				break;
			}
			case "ChangeChargingPower" : {
				soapParameter = Arrays.asList(sc.getMin()+","+sc.getMax());
				break;
			}
			case "get_carrier_store_location" : {
				soapParameter = (!sc.getCarId().isBlank()) ? Arrays.asList(sc.getCarId()) : Arrays.asList("1");
				break;
			}
			default: {
				soapParameter = Arrays.asList("1");
				break;
			}
		}
		result = sendRequestToIIs(sc.get_tag().toString(), soapParameter);
		
		return result;
	}
	
	private String test(String soapRequestData, List<String> arr) {
		String result= null;
        try{
        	SoapObject request = new SoapObject("http://tempuri.org/", soapRequestData);
            switch (soapRequestData) {
            	case "set_carrier_next_location": {
            		request.addProperty("next_location", arr.get(0));
                    request.addProperty("carrier_id", arr.get(1));
                    request.addProperty("buffer", arr.get(2));
                    break;
                }
            	default : {
                    request.addProperty("Command", arr.get(0));
                    break;
                }
            }
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            envelope.dotNet = true;
            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://192.168.100.13:8343/WarehouseControlService.asmx");
            //HttpTransportSE androidHttpTransport = new HttpTransportSE("http://10.248.82.110:8090/DepotWeb.asmx");
            androidHttpTransport.call("http://tempuri.org/"+soapRequestData, envelope);
            String resultsRequestSOAP = envelope.getResponse().toString();

            switch (soapRequestData) {
            	case "set_carrier_next_location": {
                    result =  (resultsRequestSOAP.contains("000"))?
                    	resultsRequestSOAP
                    : "回應失敗！錯誤碼："+resultsRequestSOAP;
                    break;
                }
            	case "get_carrier_store_location": {
                    if (!resultsRequestSOAP.isEmpty()) {
                        result = resultsRequestSOAP;
                        System.out.println("getStoreLocation = "+resultsRequestSOAP);
                    } else {
                    	System.out.println("找不到儲位");
                    }
                    break;
                }
                default: {
                    if (resultsRequestSOAP.contains("000")) {
                        result = resultsRequestSOAP;
                        System.out.println("result = "+resultsRequestSOAP);
                    } else {
                    	System.out.println("設定錯誤"+resultsRequestSOAP);
                    }
                    break;
                }
            }
        }catch(Exception e) {
        	e.getMessage();
        }
        
        return result;
    }
	
	private String sendRequestToIIs(String soapRequestData, List<String> arr) {
        String result= null;
        try{
        	SoapObject request = new SoapObject("http://tempuri.org/", soapRequestData);
            switch (soapRequestData) {
            	case "set_carrier_next_location": {
                    request.addProperty("task_num", arr.get(0));
                    request.addProperty("next_location", arr.get(1));
                    request.addProperty("carrier_id", arr.get(2));
                    request.addProperty("direction", arr.get(3));
                    request.addProperty("command", arr.get(4));
                    break;
                }
            	default : {
                    request.addProperty("cmd", arr.get(0));
                }
            }
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            envelope.dotNet = true;
            //HttpTransportSE androidHttpTransport = new HttpTransportSE("http://10.248.82.110:8090/DepotWeb.asmx");
            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://192.168.100.13:8343/WarehouseControlService.asmx");
            androidHttpTransport.call("http://tempuri.org/"+soapRequestData, envelope);
            String resultsRequestSOAP = envelope.getResponse().toString();
            
            switch (soapRequestData) {
            	case "set_carrier_next_location": {
                    result =  (resultsRequestSOAP.contains("000"))?
                    	resultsRequestSOAP
                    : "回應失敗！錯誤碼："+resultsRequestSOAP;
                }
            	case "get_carrier_store_location": {
                    if (!resultsRequestSOAP.isEmpty()) {
                        result = resultsRequestSOAP;
                        System.out.println("getStoreLocation = "+resultsRequestSOAP);
                    } else {
                    	System.out.println("找不到儲位");
                    }
                }
                default: {
                    if (resultsRequestSOAP.contains("000")) {
                        result = resultsRequestSOAP;
                        System.out.println("result = "+resultsRequestSOAP);
                    } else {
                    	System.out.println("設定錯誤"+resultsRequestSOAP);
                    }
                }
            }
        }catch(Exception e) {
        	result = e.getLocalizedMessage();
        }
        
        return result;
    }
	
}
