package com.wcs.autoEx.service.tally;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.tools.UnitConv;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.MarshalBase64;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import com.hivemq.client.mqtt.mqtt5.exceptions.Mqtt5ConnAckException;
import com.wcs.autoEx.PackageBox;
import com.wcs.autoEx.service.armConnection.ArmDeviceService;
import com.wcs.dao.FinishedPackageService;
import com.wcs.frame.WcsApplicationFrame;
import com.wcs.mq.MqSender;

@Service
@Scope("singleton")
public class TallyService implements Runnable {
	
	@Autowired
	MqSender mqSender;
	
	@Autowired
	ArmDeviceService armDeviceService;
	
	@Autowired
	WcsApplicationFrame wcsAppFrame;
	
	@Autowired
	FinishedPackageService finishedPackageService;
	
	private Boolean test = true;
	private PackageBox packageBox;
	public Boolean irStatus = false;
	public Boolean armStatus = false;
	public Boolean transferStatus = false;
	public Queue<PackageBox> queue1 = new LinkedList<PackageBox>();
	public Queue<PackageBox> queue2 = new LinkedList<PackageBox>();
	public Queue<PackageBox> queue3 = new LinkedList<PackageBox>();
	private int cabCount = 0;
	
	@Override
	public void run() {
		tallyOperation();
	}
	
	public void setPackageBox(PackageBox pb) {
		queue1.add(pb);
	}
	
	public void setVolumeDetect(String vd) {
		JSONObject data = new JSONObject(vd);
		PackageBox pb = queue1.poll();
		if (data.getBoolean("Succ")) {
			JSONArray jsonArr = data.getJSONArray("Data");
			for (int i=0;i<jsonArr.length();i++ ) {
				pb.h_length = jsonArr.getJSONObject(i).getFloat("h_length");	//height
				pb.l_length = jsonArr.getJSONObject(i).getFloat("l_length");  //width
				pb.w_length = jsonArr.getJSONObject(i).getFloat("w_length");	//depth
				pb.weight_g = jsonArr.getJSONObject(i).getFloat("weight_g");
				pb.weight_kg = jsonArr.getJSONObject(i).getFloat("weight_kg");
				pb.cmb_g = jsonArr.getJSONObject(i).getFloat("cmb_g");
				pb.cmb_unit = jsonArr.getJSONObject(i).getFloat("cmb_unit");
				pb.packageStatus = "完成材積量測";
			}
		}
		queue2.add(pb);
	}
	
	public void addBarcodeAndText (File document, String barcodeContent) {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	
    	Code128Bean bean = new Code128Bean();
    	
    	int dpi = 72;
    	
    	bean.setModuleWidth(UnitConv.in2mm(1.0f/dpi));
    	
    	bean.doQuietZone(false);
    	
    	String format = "image/png";
    	
    	BitmapCanvasProvider canvas = new BitmapCanvasProvider(baos, format, dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);
    	
    	bean.generateBarcode(canvas, barcodeContent);
    	
    	try{
    		canvas.finish();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	} finally {
    		try {
    			if (baos != null)
    				baos.close();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    	
    	byte[] barCode = baos.toByteArray();
    	
    	PDDocument doc;
		try {
			doc = PDDocument.load(document);
			PDPage page = doc.getPage(0);
//			doc = new PDDocument();
//    		PDPage page = new PDPage();
//    		doc.addPage(page);
			PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, barCode, barcodeContent);
			PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.APPEND, true, true);
			float scale = 1f;
			//上面barcode
			contentStream.drawImage(pdImage, 155, 435, pdImage.getWidth() * 1f, pdImage.getHeight() * scale);
			//下面barcode
			contentStream.drawImage(pdImage, 240, 220, pdImage.getWidth() * 1f, pdImage.getHeight() * scale);
			//收件人
			contentStream.beginText();
			contentStream.newLineAtOffset(160, 400);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 12 );
			contentStream.showText("工業技術研究院");
			contentStream.endText();
			//收件人地址
			String test = "新竹縣竹東鎮中興路四段195號52館222";
			if (test.length()> 15) {
				contentStream.beginText();
				contentStream.newLineAtOffset(30, 160);
				contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 10 );
				contentStream.showText(test.substring(0, 15));
				contentStream.newLine();
				contentStream.newLineAtOffset(0, -10);
				contentStream.showText(test.substring(15));
				contentStream.endText();
			} else {
				contentStream.beginText();
				contentStream.newLineAtOffset(30, 160);
				contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 10 );
				contentStream.showText(test);
				contentStream.endText();
			}
			//收件人電話
			contentStream.beginText();
			contentStream.newLineAtOffset(160, 365);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 10 );
			contentStream.showText("09876543210");
			contentStream.endText();
			//寄件人
			contentStream.beginText();
			contentStream.newLineAtOffset(160, 335);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 11 );
			contentStream.showText("工業技術研究院");
			contentStream.endText();
			//寄件人地址
			contentStream.beginText();
			contentStream.newLineAtOffset(160, 326);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("新竹縣竹東鎮中興路四段");
			contentStream.endText();
			//寄件人電話
			contentStream.beginText();
			contentStream.newLineAtOffset(160, 318);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("09876543210");
			contentStream.endText();
			//客戶代號
			contentStream.beginText();
			contentStream.newLineAtOffset(177, 303);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("NNNN111111");
			contentStream.endText();
			//訂單編號
			contentStream.beginText();
			contentStream.newLineAtOffset(178, 290);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("n1m1nm3n13");
			contentStream.endText();
			//備註
			contentStream.beginText();
			contentStream.newLineAtOffset(157, 275);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("測試");
			contentStream.endText();
			//收貨日
			contentStream.beginText();
			contentStream.newLineAtOffset(337, 418);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("2022-04-15");
			contentStream.endText();
			//預定配達日
			contentStream.beginText();
			contentStream.newLineAtOffset(337, 383);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("2022-04-15");
			contentStream.endText();
			//配達時段
			contentStream.beginText();
			contentStream.newLineAtOffset(348, 355);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("15-17");
			contentStream.endText();
			//發貨所
			contentStream.beginText();
			contentStream.newLineAtOffset(346, 326);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText("新竹所");
			contentStream.endText();
			//尺寸
			contentStream.beginText();
			contentStream.newLineAtOffset(344, 295);
			contentStream.setFont(PDType0Font.load(doc, new File("D:\\wcsService\\wcsService\\src\\main\\resources\\fonts\\wt003.ttf")), 8 );
			contentStream.showText(queue2.element().h_length+"cm");
			contentStream.endText();
			
			contentStream.close();
			doc.save("C:\\Users\\ITRI2021\\Downloads\\testAddText.pdf");
			doc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	;
    }
	
	public void sendMqtt(String topic, String content) {
		try {
			final Mqtt5BlockingClient client = Mqtt5Client.builder()
	        .identifier(UUID.randomUUID().toString())
	        .serverHost("broker.hivemq.com")
	        .buildBlocking();
	
			String preStr = "itri/074afa246ac14bff9218d213ff1a75ae/aex/Jusda/";
			client.connect();
			client.publishWith().topic(preStr+topic).qos(MqttQos.AT_LEAST_ONCE).payload(content.getBytes()).send();
			client.disconnect();
		} catch (Exception e) {
			e.getStackTrace();
		}
	}
	
	public String soapTransmit(byte[] data) {
		String result= null;
        try{
        	SoapObject request = new SoapObject("http://tempuri.org/", "add_label_file_to_queue");
        	request.addProperty("location", "000");
            request.addProperty("id", cabCount);
        	request.addProperty("file_array", data);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            new MarshalBase64().register(envelope);
            envelope.setOutputSoapObject(request);
            envelope.dotNet = true;
            //測試網址
            //HttpTransportSE androidHttpTransport = new HttpTransportSE("http://59.124.226.7:52000/ServiceMain.asmx");
            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://10.10.0.105:52000/ServiceMain.asmx");
            androidHttpTransport.call("http://tempuri.org/add_label_file_to_queue", envelope);
            String resultsRequestSOAP = envelope.getResponse().toString();
            result =  (resultsRequestSOAP.contains("000"))? "000,OK" : "回應失敗！錯誤碼："+resultsRequestSOAP; 
            System.out.println("cabResult="+result);
            transferStatus = true;
            cabCount++;
            queue3.add(queue2.poll());
            packageBox = queue3.element();
            wcsAppFrame.setLabel();
        }catch(Exception e) {
        	System.out.println(e.getLocalizedMessage());
        	wcsAppFrame.setError("labelConnection", e.getLocalizedMessage(), "set");
        }    
        return result;
	}
	
    public byte[] fileToBase64(String path) {
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }
	
	public void tallyOperation() {
		while (true) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		    
			if (queue1.size()>0)
				System.out.println("Queue1:"+queue1.element().toString());
			
			if (queue2.size()>0)
				System.out.println("Queue2:"+queue2.element().toString());
			
			if (queue3.size()>0)
				System.out.println("Queue3:"+queue3.element().toString());
		 
			//if (packageBox != null && !transferStatus) {
			if (queue2.size() > 0) {
				if (queue2.element() != null) {
					// && !transferStatus
					if (!test) {
						File file = new File("C:\\Users\\ITRI2021\\Downloads\\標籤設計(底).pdf");
						try {
							addBarcodeAndText(file, queue2.element().consignNumber);
							soapTransmit(fileToBase64("C:\\Users\\ITRI2021\\Downloads\\testAddText.pdf"));
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						//測試用
						transferStatus = true;
			            cabCount++;
			            queue3.add(queue2.poll());
			            packageBox = queue3.element();
			            wcsAppFrame.setLabel();
					}
				}
			}

			if (queue3.size() > 0 ) {
				//測試用
				if (test) {
					if(queue3.element() != null && armStatus) {
						irStatus = true;
					}
				}
				if (queue3.element() != null && armStatus && !irStatus) {
					sendMqtt("plc/relay/30f0731bb6c043e79f3b4200f61529e1", "1"); 
					transferStatus = false;
				}
			}
			
			if (!irStatus)
				wcsAppFrame.setError("label", "", "fix");
			
			if (irStatus) {
				sendMqtt("plc/relay/30f0731bb6c043e79f3b4200f61529e1", "2");
//				if (!test) {
				if (doJudge(packageBox)) {
					JSONObject json = new JSONObject();
					json.put("length", packageBox.l_length);
					json.put("width", packageBox.w_length);
					json.put("height", packageBox.h_length);
					json.put("weight", packageBox.weight_g);
					
					armDeviceService.armCmdDo(json);
					WcsApplicationFrame.tallyArmStatus = false;
					wcsAppFrame.setArm();
					PackageBox pb = queue3.poll();
					finishedPackageService.save(pb);
					if (queue3.size() > 0)
						packageBox = queue3.element();
					else
						packageBox = null;
					JSONObject json1 = new JSONObject();
					json1.put("orderId", pb.orderId);
					mqSender.sendJsonTopic("armFinish", json1);
					irStatus = false;
					armStatus = false;	
				}
				
//				} else {
//					JSONObject json = new JSONObject();
//					armDeviceService.armCmdDo(json);
//					WcsApplicationFrame.tallyArmStatus = false;
//					wcsAppFrame.setArm();
//					try {
//						Thread.sleep(2000);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					PackageBox pb = queue3.poll();
//					if (queue3.size() > 0)
//						packageBox = queue3.element();
//					else
//						packageBox = null;
//					mqSender.sendTopic("armFinish", pb.orderId);
//					irStatus = false;
//					armStatus = false;	
//				}
			}
		}
	}
	
	public Boolean doJudge(PackageBox pb) {
		if (pb.weight_g == 0) {
			System.out.println("材積沒重量");
			sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
			wcsAppFrame.setError("label", "CV4包裹材積沒重量", "set");
			queue3.poll();
			irStatus = false;
			return false;
		} else if (pb.h_length == 0) {
			System.out.println("材積沒高度");
			sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
			wcsAppFrame.setError("label", "CV4包裹材積沒高度", "set");
			queue3.poll();
			irStatus = false;
			return false;
		} else if (pb.l_length == 0) {
			System.out.println("材積沒長度");
			sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
			wcsAppFrame.setError("label", "CV4包裹材積沒長度", "set");
			queue3.poll();
			irStatus = false;
			return false;
		} else if (pb.w_length == 0) {
			System.out.println("材積沒寬度");
			sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
			wcsAppFrame.setError("label", "CV4包裹材積沒寬度", "set");
			queue3.poll();
			irStatus = false;
			return false;
		} 
		
		if (pb.l_length > 20 && pb.l_length < 29) { //type 1,4,5,8,9,10
			System.out.println("type 1,4,5,8,9,10");
				if (pb.h_length > 10.5 && pb.h_length < 12.5) { //type 4,10
					if (pb.l_length > 20 && pb.l_length < 22)  
						if (pb.w_length > 15.5 && pb.w_length < 17.5) 
							pb.type = 4;
					
					if (pb.w_length > 20.5 && pb.w_length < 22.5)
						if (pb.l_length > 27 && pb.l_length < 29)
							pb.type = 10;
				}
				
				if(pb.h_length > 6.5 && pb.h_length < 8.5) //type 9
					if (pb.w_length > 14.5 && pb.w_length < 16.5) 
						if (pb.l_length > 22 && pb.l_length < 24)
							pb.type = 9;
				
				if (pb.h_length > 15.5 && pb.h_length < 17.5) //type 8
					if (pb.w_length > 19.5 && pb.w_length < 21.5) 
						if (pb.l_length > 23 && pb.l_length < 25) 
							pb.type = 8;
	
				if (pb.h_length > 17.6 && pb.h_length < 19.6) //type 1
					if (pb.w_length > 14.5 && pb.w_length < 16.5)
						if (pb.l_length > 25 && pb.l_length < 27)
							pb.type = 1;
				
				if (pb.h_length > 12.5 && pb.h_length < 14.5) //type 5
					if (pb.w_length > 20.5 && pb.w_length < 22.5)
						if (pb.l_length > 24.5 && pb.l_length < 26.5)
							pb.type = 5;
				
		} else if (pb.l_length > 33 && pb.l_length < 47) {
			System.out.println("type 3,2,7,12");
			if (pb.h_length > 14 && pb.h_length < 16) //type 3
				if (pb.w_length > 24 && pb.w_length < 26)
					if (pb.l_length > 33.5 && pb.l_length < 35.5)
						pb.type = 3;
			
			if (pb.h_length > 19 && pb.h_length < 21.5) //type 2
				if (pb.w_length > 29.5 && pb.w_length < 31.5)
					if (pb.l_length > 38.5 && pb.l_length < 41)
						pb.type = 2;
			
			if (pb.h_length > 24.5 && pb.h_length < 26.5) //type 7
				if (pb.w_length > 29 && pb.w_length < 31)
					if (pb.l_length > 44 && pb.l_length < 46)
						pb.type = 7;
			
			if (pb.h_length > 29.5 && pb.h_length < 31.5) //type 12
				if (pb.w_length > 29.5 && pb.w_length < 31.5)
					if (pb.l_length > 44 && pb.l_length < 46)
						pb.type = 12;
		}
		
		System.out.println("pbType="+pb.type);
		
		switch (pb.type) {
			case 1,4,5,8,9,10: {
				if (pb.weight_kg > 7) {
					sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
					wcsAppFrame.setError("label", "CV4包裹過重", "set");
					System.out.println("太重了");
					queue3.poll();
					irStatus = false;
					return false;
				}
				break;
			}
			case 2,3: {
				if (pb.weight_kg > 5) {
					sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
					System.out.println("太重了");
					wcsAppFrame.setError("label", "CV4包裹過重", "set");
					queue3.poll();
					irStatus = false;
					return false;
				}
				break;
			}
			case 7,12: {
				if (pb.weight_kg > 4) {
					sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
					System.out.println("太重了");
					wcsAppFrame.setError("label", "CV4包裹過重", "set");
					queue3.poll();
					irStatus = false;
					return false;
				}
				break;
			}
			default : {
				sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
				System.out.println("找不到箱子類別");
				wcsAppFrame.setError("label", "CV4包裹無法找到箱子類別", "set");
				queue3.poll();
				irStatus = false;
				return false;
			}
		}
		return true;
	}
}
