package com.wcs.autoEx.service.tally;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.wcs.autoEx.PackageBox;
import com.wcs.mq.MqSender;

@Service
@Scope("singleton")
public class TallyService implements Runnable {
	
	@Autowired
	MqSender mqSender;
	
	private PackageBox packageBox;
	private Socket s = null;
	public Boolean irStatus = false;
	Queue<PackageBox> queue1 = new LinkedList<PackageBox>();
	Queue<PackageBox> queue2 = new LinkedList<PackageBox>();
	
	public void setPackageBox(PackageBox pb) {
		queue1.add(pb);
	}
	
	public void setVolumeDetect(String vd) {
		System.out.println("0");
		PackageBox pb = queue1.poll();
		pb.setHeight(null);
		pb.setLength(null);
		pb.setWeight(null);
		pb.setWidth(null);
		queue2.add(pb);
		packageBox = pb;
		System.out.println(queue2.element());
	}
	
	public void sendFinish() {
		if (queue2.size()>0) {
			PackageBox pb = queue2.poll();
			mqSender.sendTopic("armFinish", pb.orderId);
		}
	}
	
	public void armConnection() {
		try {
			s = new Socket("localhost", 5566);
			// 本地主機
			System.out.println("客戶端。。。。。。。");
			byte[] temp = new byte[1024];
			int bytesRead = 0;
		
			while (true) {
				InputStream in = s.getInputStream();
				bytesRead = in.read(temp); // 行讀取
				String res = new String(temp,0,bytesRead,"ASCII");
				System.out.println("伺服器：" + res);
				if (res.contains("Ready")) {
					armCmdDo();
					mqSender.sendTopic("ArmStatus", res);
				}
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}
	
	public void armCmdDo() {
		PrintWriter pw;
		try {
			pw = new PrintWriter(s.getOutputStream(),true);
//			packageBox = new PackageBox();
//			packageBox.orderId= "123333";
//			packageBox.height=(float)1.00;
//			packageBox.length=(float)1.00;
//			packageBox.weight=(float)1.00;
//			packageBox.width=(float)1.00;
			while (packageBox == null) {
				try {
					if (queue1.size()>0)
						System.out.println("Queue1:"+queue1.element().toString());
					if (queue2.size()>0)
						System.out.println("Queue2:"+queue2.element().toString());
					System.out.println("packageBox:"+packageBox);
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (irStatus) {
				JSONObject jsonObject = new JSONObject(packageBox);
				String jsonStr = "{\"command\": \"start\", \"volume\": "+jsonObject+"}";
				JSONObject info = new JSONObject(jsonStr);
				pw.println(info);
				packageBox = null;
				irStatus = false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		armConnection();
	}
	
}
