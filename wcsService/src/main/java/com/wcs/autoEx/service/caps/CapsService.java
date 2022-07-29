package com.wcs.autoEx.service.caps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.springframework.stereotype.Service;
import org.xmlpull.v1.XmlPullParserException;
import com.atop.autoEx.cAPS.*;
import com.atop.autoEx.IExceptionEvent;
import com.atop.autoEx.IGoodEndEvent;
import com.wcs.autoEx.AexServiceBase;


@Service
public class CapsService extends AexServiceBase {
private List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
private JSONArray capsMapper = null;
private JSONArray seedsMapper = null;
//private String path = "http://10.248.82.110:8090/DepotWeb.asmx";
private String path = "http://192.168.100.13:8343/WarehouseControlService.asmx";

	@PostConstruct
	@Override
	public void init() {
		super.init();
		String inputFileName = "aex_setting_jsd.json";
//		String inputFileName = "aex_setting.json";

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream inputStream = classloader.getResourceAsStream(inputFileName);

		try {
			InputStreamReader isr = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(isr);

			StringBuilder resultStringBuilder = new StringBuilder();
			String aexSettingStr;
			while ((aexSettingStr = reader.readLine()) != null) {
				resultStringBuilder.append(aexSettingStr).append("\n");
			}
			
			JSONObject dc_setting_obj = new JSONObject(resultStringBuilder.toString());
			if (dc_setting_obj != null) {
	            JSONObject aexSetting = dc_setting_obj.getJSONObject("AEX");
	            if (aexSetting != null) {
	                JSONObject capsSetting = aexSetting.getJSONObject("CAPS");
	                if (capsSetting != null) {
	                    String iisAddress = capsSetting.getString("iis_url");
	                    if (iisAddress != null) {
	                        JSONArray capsPotsSetting = capsSetting.getJSONArray("caps_pots");
	                        if (capsPotsSetting != null) {
	                            this.capsMapper = capsPotsSetting;
	                            for (int i=0; i<this.capsMapper.length(); i++) {
	                            	JSONObject apiAddress = this.capsMapper.getJSONObject(i);
	                            	JSONObject seeds = this.capsMapper.getJSONObject(i);
	                            	this.seedsMapper = seeds.getJSONArray("seeds");
	                            	if (seeds != null) {
	                                    for (int j=0; j< this.seedsMapper.length(); j++) {
	                                        HashMap<String, Object> capsMap = new HashMap<String, Object>();
	                                        JSONObject contentBiz = this.seedsMapper.getJSONObject(j);
	                                        JSONObject contentHard = this.seedsMapper.getJSONObject(j);
	                                        capsMap.put("firstBiz", apiAddress.getString("bizAddress"));
	                                        capsMap.put("secondBiz", contentBiz.getString("bizAddress"));
	                                        capsMap.put("hard", contentHard.getString("hardAddress"));
	                                        results.add(capsMap);
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	        }

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Subscribe
	public void onButtonClickEvent(ICAPSButtonClickEvent event) throws Exception {
		System.out.println(
				"CAPSService onButtonClickEvent potAddress: " + event.getPotAddress() + " seedAddress: " + event.getSeedAddress());
	}

	@Override
	@Subscribe
	public void onCommandGoodEnd(IGoodEndEvent event) {
		if (event.getCommand() instanceof ILightChangeCmd) {
			this.bizLightChangeCmdGoodEndDo((ILightChangeCmd) event.getCommand());
		} else if (event.getCommand() instanceof INumDisplaySetCmd) {
			this.bizNumDisplaySetCmdGoodEndDo((INumDisplaySetCmd) event.getCommand());
		}
	}

	@Override
	@Subscribe
	public void onCommandException(IExceptionEvent event) {
		if (event.getCommand() instanceof ICAPSCommand) {
			System.out.println("ShuttleService onCommandException commandId: " + event.getCommand().getId() + "ex: "
					+ event.getException().getLocalizedMessage());
		}
	}

	@Subscribe
	private void bizLightChangeCmdGoodEndDo(ILightChangeCmd cmd) {
		// API Team 需要寫邏輯去寫命令完成邏輯
		System.out.println("CAPSService bizLightChangeCmdGoodEndDo color: " + cmd.getLightColor());
	}

	@Subscribe
	private void bizNumDisplaySetCmdGoodEndDo(INumDisplaySetCmd cmd) {
		// API Team 需要寫邏輯去寫命令完成邏輯
	}
	
	public void onSeedCmd(ISeedCommand cmd) {
		reset(cmd.getPotAddress());
		setLight(cmd);
		setNum(cmd);
	}
	
	private void reset(String potId) {
		int count = 0;
		String closeLight ="";
		System.out.println(results.size());
		while(count < results.size()){
			if(potId.equals(results.get(count).get("firstBiz"))){
				if(count == 0 )
					closeLight =  "CAP"+ (results.get(count).get("hard")) +",0";		        
		        else
		        	closeLight += "|||CAP"+ (results.get(count).get("hard")) +",0";        
		    }
		    count++;
		}
		sendColorToIIs(closeLight);
	}
	
	private void setNum(ISeedCommand cmd){
        int count = 0;
        for (int i=0; i< cmd.getSeeds().size(); i++) {
        	String addresses = cmd.getSeeds().get(i).getAddress();
        	int num = cmd.getSeeds().get(i).getNum();
        	count=0;
        	while(count<results.size()){
                if(cmd.getPotAddress().equals(results.get(count).get("firstBiz"))){
                    if(addresses.equals(results.get(count).get("secondBiz"))){
                    	NumDisplaySetCmd numChangeCmd = new NumDisplaySetCmd(List.of(results.get(count).get("hard").toString()), num, "capsSetNum");
                        onNumDisplaySetCmdDo(numChangeCmd);
                    }
                }
                count++;
            }
        }
    }

    private void setLight(ISeedCommand cmd){
        int count = 0;
        for (int i=0; i< cmd.getSeeds().size(); i++) {
        	String addresses = cmd.getSeeds().get(i).getAddress();
        	LightColorEnum color = cmd.getSeeds().get(i).getColor();
        	count = 0;
        	while(count<results.size()){
                if(cmd.getPotAddress().equals(results.get(count).get("firstBiz"))){
                    if(addresses.equals(results.get(count).get("secondBiz"))){
                    	LightChangeCmd lightChangeCmd = new LightChangeCmd(List.of(results.get(count).get("hard").toString()), color, "capsSetLight");
                        onLightChangeCmdDo(lightChangeCmd);
                    }
                }
                count++;
            }
        }
    }
    
    public void onLightChangeCmdDo(ILightChangeCmd cmd) { //AEX Team 需寫Device 底層協定
    	try {
	        String value = "0";
	        String multiLight = "";
	        String light = cmd.getLightColor().toString();
	        String green = "GREEN";
        	String red= "RED";

            if (cmd.getAddresses().size() < 2) {
            	if (light.equals(green)) {
            		value = "CAP" + cmd.getAddresses().get(0) + ",2";
                    multiLight = value;
                } else if (light.equals(red)) {
                    value = "CAP" + cmd.getAddresses().get(0) + ",1";
                    multiLight = value;
                }
            } else {
                int i = 0;
                while (i < cmd.getAddresses().size()) {
                    if (light.equals(green)) {
                        value = "CAP" + cmd.getAddresses().get(i) + ",2";
                    } else if (light.equals(red)) {
                        value = "CAP" + cmd.getAddresses().get(i) + ",1";
                    }
                    if(i>0)
                        multiLight += "|||$value";
                    else 
                        multiLight = value;
                    
                    i++;
                }
            }
            sendColorToIIs(multiLight);
            System.out.println("CPASController onLightChangeCmdDo bizAddress:" +cmd.getAddresses().get(0) +"hardAddress:$hardAddress color:"+cmd.getLightColor());
        } catch (Exception ex) {
            ex.getStackTrace();
        }
    }

    public void onNumDisplaySetCmdDo(INumDisplaySetCmd cmd) {
        try {
            String value = "";
            String multiLight = "";
            String num = Integer.toString(cmd.getNum());

            if (cmd.getAddresses().size() < 2) {
                value = "CAP" + cmd.getAddresses().get(0) + ","+ num;
                multiLight = value;
            } else {
                int i = 0;
                while (i < cmd.getAddresses().size()) {
                    value = "CAP" + cmd.getAddresses().get(i)  + ","+ num;
                    if(i>0)
                        multiLight += "|||$value";
                    else
                        multiLight = value;
                        
                    i++;
                }
            }
            if (multiLight != null) {
                sendTagToIIs(multiLight);
            }
            System.out.println("CAPSDeviceController numChangeCmdDo bizAddress:"+cmd.getAddresses().get(0)+ " hardAddress:$hardAddress num:"+cmd.getNum());
        } catch (Exception ex) {
        	ex.getStackTrace();
        }
    }
	
	private void sendColorToIIs(String value) {
		SoapObject request = new SoapObject("http://tempuri.org/", "SetToTagsColorOnWeb");
        request.addProperty("cmd", value);
//		request.addProperty("command", value);//測試模擬器用的
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        envelope.dotNet = true;
        HttpTransportSE androidHttpTransport = new HttpTransportSE(path);
        try {
			androidHttpTransport.call("http://tempuri.org/SetToTagsColorOnWeb", envelope);
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}

        String resultsRequestSOAP = null;
		try {
			resultsRequestSOAP = envelope.getResponse().toString();
		} catch (SoapFault e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        if (!resultsRequestSOAP.isBlank()) {
            System.out.println("呼叫成功");
        }
        else {
        	System.out.println("呼叫失敗！response_body為空");
            //錯誤轉發至外層去做 try catch 機制
        }
    }
	
	private void sendTagToIIs(String value) {
		SoapObject request = new SoapObject("http://tempuri.org/", "SetToTagsOnWeb");
        request.addProperty("cmd", value);
//		request.addProperty("command", value);//測試模擬器用的
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.setOutputSoapObject(request);
        envelope.dotNet = true;
        HttpTransportSE androidHttpTransport = new HttpTransportSE(path);
        try {
			androidHttpTransport.call("http://tempuri.org/SetToTagsOnWeb", envelope);
		} catch (IOException | XmlPullParserException e) {
			e.printStackTrace();
		}

        String resultsRequestSOAP = null;
		try {
			resultsRequestSOAP = envelope.getResponse().toString();
		} catch (SoapFault e) {
			e.printStackTrace();
		}

        if (!resultsRequestSOAP.isBlank()) {
            System.out.println("呼叫成功");
        }
        else {
        	System.out.println("呼叫失敗！response_body為空");
            //錯誤轉發至外層去做 try catch 機制
        }
    }
}
