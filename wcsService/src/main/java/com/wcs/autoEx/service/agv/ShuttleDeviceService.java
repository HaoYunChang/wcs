package com.wcs.autoEx.service.agv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import com.atop.autoEx.INormalEndEvent;
import com.atop.autoEx.IStateChangeEvent;
import com.atop.autoEx.ITimeoutEvent;
import com.atop.autoEx.shuttle.IGoCommand;
import com.atop.autoEx.shuttle.IShuttleCommand;
import com.atop.autoEx.shuttle.IShuttleDeviceController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wcs.autoEx.AexServiceBase;
import com.wcs.dao.LocationService;
import com.wcs.mq.MqSender;

//import jusda.autoEx.shuttle.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Scope("singleton")
public class ShuttleDeviceService extends AexServiceBase {

	private IShuttleDeviceController shuttleDevice = null;
	private Map<String, IGoCommand> goCommandExecMap = new HashMap();
	private Map<String, String> locationCarrierMap = new HashMap();
	private String preShelfNum = null;
	
	@Autowired 
	LocationService locationService;
	
	@Autowired
	MqSender mqSender;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		//this.shuttleDevice = new ShuttleDeviceController(this.eventBus);
		//this.shuttleDevice.eventBusRegister();

		String inputFileName = "aex_setting.json";

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream inputStream = classloader.getResourceAsStream(inputFileName);

		try (InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(isr)) {

			StringBuilder resultStringBuilder = new StringBuilder();
			String aexSetting;
			while ((aexSetting = reader.readLine()) != null) {
				resultStringBuilder.append(aexSetting).append("\n");
			}

			JSONObject dc_setting_obj = new JSONObject(resultStringBuilder.toString());
			//this.shuttleDevice.deviceInit(dc_setting_obj);
			//this.shuttleDevice.connection();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

//	public void asyncExeCmd(IShuttleCommand cmd) throws Exception {
//		this.shuttleDevice.asyncExeCmd(cmd);
//	}

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
	
	public void onGoCmd(IGoCommand cmd) {
		Map<String, Object> storageInfo = new HashMap();
		if (cmd.getSourceAddress().equals("CAP101"))
			storageInfo = locationService.getLocation(cmd.getDestinationAddress());
		else
			storageInfo = locationService.getLocation(cmd.getSourceAddress());
			
		String carrierId = (String) storageInfo.get("shelf_num");
		String direction = (String) storageInfo.get("direction");
		String testLoc = (String) storageInfo.get("test_location");
		cmd.setCarrier(carrierId);
		this.goCommandExecMap.put(carrierId, cmd);
		String command = cmd.getDestinationAddress().equals("CAP101")? "O" : "I";
		String result="";
		if (cmd.getDestinationAddress().equals("CAP101")) {
			//String result = sendRequestToIIs("set_carrier_next_location", List.of(cmd.getId().toString(), cmd.getDestinationAddress(), cmd.getCarrier(), direction, command));
			result = test("set_carrier_next_location", List.of(cmd.getDestinationAddress(), cmd.getCarrier(), "false"));
		} else {
			//String result = sendRequestToIIs("set_carrier_next_location", List.of(cmd.getId().toString(), cmd.getDestinationAddress(), cmd.getCarrier(), direction, command));
			result = test("set_carrier_next_location", List.of(testLoc, cmd.getCarrier(), "false"));
		}
		System.out.println("result:"+result);
		mqSender.sendTopic("agvArrived", result);
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
            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://10.248.82.110:8090/DepotWeb.asmx");
            androidHttpTransport.call("http://tempuri.org/"+soapRequestData, envelope);
            String resultsRequestSOAP = envelope.getResponse().toString();

            switch (soapRequestData) {
            	case "set_carrier_next_location": {
                    result =  (resultsRequestSOAP.contains("000"))?
                        "時間${getTime()} 物流箱:${arr[1]} 000,OK"
                    : "時間${getTime()} 物流箱:${arr[1]} 回應失敗！錯誤碼："+resultsRequestSOAP;
                    break;
                }
            	case "get_carrier_store_location": {
                    if (!resultsRequestSOAP.isEmpty()) {
                        result = "000";
                        System.out.println("getStoreLocation = "+resultsRequestSOAP);
                    } else {
                    	System.out.println("找不到儲位");
                    }
                    break;
                }
                default: {
                    if (resultsRequestSOAP.contains("000")) {
                        result = "000,OK";
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
                }
            	default : {
                    request.addProperty("Command", arr.get(0));
                }
            }
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            envelope.dotNet = true;
            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://10.248.82.110:8090/DepotWeb.asmx");
            androidHttpTransport.call("http://tempuri.org/"+soapRequestData, envelope);
            String resultsRequestSOAP = envelope.getResponse().toString();
            
            switch (soapRequestData) {
            	case "set_carrier_next_location": {
                    result =  (resultsRequestSOAP.contains("000"))?
                        "000,OK"
                    : "回應失敗！錯誤碼："+resultsRequestSOAP;
                }
            	case "get_carrier_store_location": {
                    if (!resultsRequestSOAP.isEmpty()) {
                        result = "000";
                        System.out.println("getStoreLocation = "+resultsRequestSOAP);
                    } else {
                    	System.out.println("找不到儲位");
                    }
                }
                default: {
                    if (resultsRequestSOAP.contains("000")) {
                        result = "000,OK";
                        System.out.println("result = "+resultsRequestSOAP);
                    } else {
                    	System.out.println("設定錯誤"+resultsRequestSOAP);
                    }
                }
            }
        }catch(Exception e) {
        	e.getMessage();
        }
        
        return result;
    }
	
}
