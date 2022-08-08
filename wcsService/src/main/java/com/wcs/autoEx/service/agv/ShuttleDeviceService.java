package com.wcs.autoEx.service.agv;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
import com.wcs.autoEx.PackageBox;
import com.wcs.dao.ActionLogService;
import com.wcs.dao.LocationService;
import com.wcs.mq.MqSender;

@Service
@Scope("singleton")
public class ShuttleDeviceService extends AexServiceBase {

	public static Queue<IGoCommand> carQueue = new LinkedList<IGoCommand>();
	
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
	
	public void reset() {
		//先取入庫執行中任務，兩秒檢查一次是否完成
		List<Map<String, Object>> undoBackCmd = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> undoGoCmd = new ArrayList<Map<String, Object>>();
		do{
			undoBackCmd = actionLogService.getUndoCmd("I");
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}while(undoBackCmd.size() > 0);
		
	}
	
	public Map<String, String> onGoCmd(IGoCommand cmd) {
		Map<String, Object> storageInfo = new HashMap<String, Object>();
		Map<String, Object> capsInfo = new HashMap<String, Object>();
		Map<String, String> result = new HashMap<String, String>();
		String strResult="";
		try {
			capsInfo = locationService.getLocation("CAP101");
			System.out.println("caps_shelf_num:"+capsInfo.get("shelf_num"));
			System.out.println("caps_direction:"+capsInfo.get("direction"));
			if (cmd.getSourceAddress().equals("CAP101")) {
				storageInfo = locationService.getLocation(cmd.getDestinationAddress());
				System.out.println("回storage_shelf_num:"+storageInfo.get("shelf_num"));
				System.out.println("回storage_direction:"+storageInfo.get("direction"));
				if (!storageInfo.get("shelf_num").equals(capsInfo.get("shelf_num"))) {
					result.put("status", "fail");
					result.put("message", "回去的儲位與貨架編號不符");
					return result;
				}
			} else {
				try {
					storageInfo = locationService.getLocation(cmd.getSourceAddress());
					System.out.println("去storage_shelf_num:"+storageInfo.get("shelf_num"));
					System.out.println("去storage_direction:"+storageInfo.get("direction"));
					if (capsInfo.get("direction").equals(storageInfo.get("direction")) && capsInfo.get("shelf_num").equals(storageInfo.get("shelf_num"))) {
						System.out.println("貨架道撿貨站:");
						result.put("status", "fail");
						result.put("message", "該貨架已在撿貨站");
						return result;
					}
					if (!carQueue.isEmpty()) {
						Map<String, Object> queueInfo = locationService.getLocation(carQueue.element().getSourceAddress());
						if (queueInfo.get("shelf_num").equals(storageInfo.get("shelf_num"))) {
							System.out.println("該貨架正在執行撿貨任務queue:"+queueInfo.get("shelf_num"));
							result.put("status", "fail");
							result.put("message", "該貨架正在執行撿貨任務");
							return result;
						}
					}
					
				}catch (Exception e) {
					result.put("status", "fail");
					result.put("message", "查無此儲位");
					if(!carQueue.isEmpty())
						if(carQueue.element().equals(cmd))
							carQueue.poll();
					return result;
				}
			}
			String carrierId = (String) storageInfo.get("shelf_num");
			String direction = (String) storageInfo.get("direction");
			String shelfLocation = (String) storageInfo.get("shelf_location");
			String command = cmd.getDestinationAddress().equals("CAP101")? "O" : "I";
			if (command == "I")
				direction = "D";
			
			if (command == "O") {
				if (capsInfo.get("shelf_num").equals(storageInfo.get("shelf_num"))) {
					command = "T";
				}		
			}	
			
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
			if (cmd.getSourceAddress().equals("CAP101"))
				strResult = sendRequestToIIs("set_carrier_next_location", List.of(dtf.format(LocalDateTime.now()), shelfLocation, carrierId, direction, command));
			else
				strResult = sendRequestToIIs("set_carrier_next_location", List.of(dtf.format(LocalDateTime.now()), capsInfo.get("shelf_location").toString(), carrierId, direction, command));
			
			System.out.println("外層AGV執行結果:"+strResult);
			if (strResult.contains("000")) {
				if (!command.equals("I"))
					carQueue.add(cmd);
				result.put("status", "OK");
				result.put("message", "");
				//新增action_log
				try {
					actionLogService.save((String)storageInfo.get("storage_num"), cmd.getDestinationAddress(), command);
				}catch (Exception e) {
					result.put("status", "fail");
					result.put("message", "sql寫入錯誤");
					System.out.println("actionLog寫入錯誤:"+e.getLocalizedMessage());
					return result;
				}
			} else {
				carQueue.poll();
				result.put("status", "fail");
				result.put("message", strResult);
			}
		}catch (Exception e) {
			System.out.println("外層AGV　fail cause:"+e.getLocalizedMessage());
			result.put("status", "fail");
			result.put("message", e.getLocalizedMessage());
			return result;
		}
		return result;
	}
	
	public Map<String, String> onSetCmd(SettingAgvCommand sc) {
		String strResult = "";
		String tag = (String) sc.get_tag();
		List<String> soapParameter = new ArrayList<String>();
		Map<String, String> result = new HashMap<String, String>();
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
				Map<String,Object> locationInfo = new HashMap<String,Object>();
				if (!sc.getCarId().isBlank()) {
					locationInfo = locationService.getLocation(sc.getCarId());
				}
				soapParameter = (!sc.getCarId().isBlank()) ? Arrays.asList(locationInfo.get("shelf_num").toString()) : Arrays.asList("1");
				break;
			}
			default: {
				soapParameter = Arrays.asList("1");
				break;
			}
		}
		strResult = sendRequestToIIs(sc.get_tag().toString(), soapParameter);
		if (strResult.contains("000")) {
			result.put("status", "OK");
			result.put("message", strResult);
		} else {
			result.put("status", "fail");
			result.put("message", strResult);
		}
		
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
            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://10.248.82.110:8090/DepotWeb.asmx");
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
        String result= "";
        try{
        	SoapObject request = new SoapObject("http://tempuri.org/", soapRequestData);
            switch (soapRequestData) {
            	case "set_carrier_next_location": {
                    request.addProperty("task_num", arr.get(0));
                    request.addProperty("next_location", arr.get(1));
                    request.addProperty("carrier_id", arr.get(2));
                    request.addProperty("direction", arr.get(3));
                    request.addProperty("command", arr.get(4));
                    System.out.println("task_num:"+arr.get(0));
                    System.out.println("next_location:"+arr.get(1));
                    System.out.println("shelf_id:"+arr.get(2));
                    System.out.println("direction:"+arr.get(3));
                    System.out.println("command:"+arr.get(4));
                    break;
                }
            	default : {
                    request.addProperty("cmd", arr.get(0));
                    break;
                }
            }
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.setOutputSoapObject(request);
            envelope.dotNet = true;
            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://192.168.100.13:8343/WarehouseControlService.asmx");
            androidHttpTransport.call("http://tempuri.org/"+soapRequestData, envelope);
            String resultsRequestSOAP = envelope.getResponse().toString();
            System.out.println("agv執行結果"+resultsRequestSOAP);
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
        	System.out.println("AGV指令錯誤:"+e.getStackTrace());
        	result = e.getLocalizedMessage();
        	return result;
        }
        
        return result;
    }
	
}
