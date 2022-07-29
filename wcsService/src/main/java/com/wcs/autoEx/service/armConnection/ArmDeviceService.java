package com.wcs.autoEx.service.armConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wcs.autoEx.ErrorMessage;
import com.wcs.autoEx.PackageBox;
import com.wcs.autoEx.service.tally.TallyService;
import com.wcs.dao.FinishedPackageService;
import com.wcs.frame.WcsApplicationFrame;
import com.wcs.mq.MqSender;

@Service
public class ArmDeviceService implements Runnable {
	@Autowired
	TallyService tallyService;
	
	@Autowired
	WcsApplicationFrame wcsAppFrame;
	
	@Autowired
	MqSender mqSender;
	
	@Autowired
	FinishedPackageService finishedPackageService;
	
	private Socket s = null;
	
	@Override
	public void run() {
		try {
			armConnection();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void armConnection() throws InterruptedException {
		try {
			while (true) {
				try {
					s = new Socket("127.0.0.1", 5566);
					if (s != null)
						break;
				}catch(ConnectException e) {
					e.getLocalizedMessage();
					TallyService.armStatus = false;
					WcsApplicationFrame.armConnection = false;
					Thread.sleep(1000);
				}
			}
			WcsApplicationFrame.armConnection = true;
			byte[] temp = new byte[1024];
			int bytesRead = 0;
			try {
				while (true) {
					InputStream in = s.getInputStream();
					bytesRead = in.read(temp); // 行讀取
					String res = new String(temp, 0, bytesRead, "ASCII");
					System.out.println("伺服器：" + res);
					if (res.contains("Ready")) {		
						TallyService.armStatus = true;
						WcsApplicationFrame.tallyArmStatus = true;
						wcsAppFrame.setError("arm", "", "fix");
						if (TallyService.packageBox != null) {
							finishedPackageService.save(TallyService.packageBox);
							sendResultToWMS(true);
						}
					} else {
						WcsApplicationFrame.tallyArmStatus = false;
						if (TallyService.packageBox != null) {
							wcsAppFrame.setError("arm", ErrorMessage.ARM_ERROR.value(), "set");
							wcsAppFrame.setPackageError();
							sendResultToWMS(false);
						}
					}
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				System.out.println(ex.getLocalizedMessage());
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void armCmdDo(JSONObject data) {
		try {
			System.out.println(data);
			PrintWriter pw = new PrintWriter(s.getOutputStream(),true);
			String jsonStr = "{\"command\": \"start\", \"volume\": "+data+"}";
			JSONObject info = new JSONObject(jsonStr);
			pw.println(info);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendResultToWMS(Boolean status) {
		JSONObject json = new JSONObject();
		json.put("id", TallyService.packageBox.id);
		json.put("orderId", TallyService.packageBox.orderId);
		json.put("consign_number", TallyService.packageBox.consignNumber);
		if (status) {
			json.put("status", "success");
			json.put("message", "成功");
		} else {
			json.put("status", "fail");
			json.put("message", "手臂異常");
		}
		mqSender.sendTopic("ArmFinish", json.toString());
		json.clear();
		TallyService.packageBox = null;	
	}
}
