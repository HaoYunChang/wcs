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

import com.wcs.autoEx.service.tally.TallyService;
import com.wcs.frame.WcsApplicationFrame;

@Service
public class ArmDeviceService implements Runnable {
	@Autowired
	TallyService tallyService;
	
	@Autowired
	WcsApplicationFrame wcsAppFrame;
	
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
					//s = new Socket("10.10.0.56", 5566);
					s = new Socket("127.0.0.1", 5566);
					if (s != null)
						break;
				}catch(ConnectException e) {
					e.getLocalizedMessage();
					tallyService.armStatus = false;
					wcsAppFrame.armConnection = false;
					System.out.println("連不到手臂");
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
						Thread.sleep(5000);
						tallyService.armStatus = true;
						wcsAppFrame.tallyArmStatus = true;
						wcsAppFrame.setError("arm", res, "fix");
					} else {
						wcsAppFrame.tallyArmStatus = false;
						wcsAppFrame.setError("arm", res, "set");
					}
				}
			} catch (SocketException e) {
				e.printStackTrace();
				armConnection();
			} catch (Exception ex) {
				ex.printStackTrace();
				armConnection();
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
}
