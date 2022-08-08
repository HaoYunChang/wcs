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
import com.wcs.autoEx.ErrorMessage;
import com.wcs.autoEx.PackageBox;
import com.wcs.autoEx.service.armConnection.ArmDeviceService;
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

	public static Boolean irStatus = false; //cv4是否有包裹
	public static Boolean armStatus = false;	//手臂是否可運作
	public static Boolean transferStatus = false;	//包裹是否傳送貼標訊息給貼標機
	public static Boolean cv4PackPoll = false;	//cv4包裹異常以及是否取下
	public Queue<PackageBox> queue1 = new LinkedList<PackageBox>(); //包裹新增進流道
	public Queue<PackageBox> queue2 = new LinkedList<PackageBox>();	//包裹經過材積辨識
	public Queue<PackageBox> queue3 = new LinkedList<PackageBox>();	//包裹以傳送貼標訊息給貼標機
	public static PackageBox packageBox;	//包裹等待手臂執行結果
	private int cabCount = 0;
	private Boolean test = false;
	private String path = "D:\\PDF\\font\\wt003.ttf";
	
	@Override
	public void run() {
		tallyOperation();
	}
	
	public void reset() {
		queue1.clear();
		queue2.clear();
		queue3.clear();
		irStatus = false;
		armStatus = false;
		transferStatus = false;
		cv4PackPoll = false;
		packageBox = null;
		cabCount = 0;
	}
	
	public void setPackageBox(PackageBox pb) {
		queue1.add(pb);
	}
	
	public void setVolumeDetect(String vd) {
		try {
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
		} catch(Exception e) {
			e.toString();
		}
	}
	
	public void addBarcodeAndText (File document, String barcodeContent, String consignNumBarcode) {
		try {
			//產製barcode
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    	Code128Bean bean = new Code128Bean();
	    	int dpi = 72;
	    	bean.setModuleWidth(UnitConv.in2mm(1.0f/dpi));
	    	bean.doQuietZone(false);
	    	String format = "image/png";
	    	BitmapCanvasProvider canvas = new BitmapCanvasProvider(baos, format, dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);
	    	bean.generateBarcode(canvas, consignNumBarcode);
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
	    	//將barcode印在pdf
	    	PDDocument doc;
			PackageBox pb = queue2.element();
			doc = PDDocument.load(document);
			PDPage page = doc.getPage(0);
			PDImageXObject pdImageConsignNum = PDImageXObject.createFromByteArray(doc, barCode, consignNumBarcode);
			PDPageContentStream contentStream = new PDPageContentStream(doc, page, AppendMode.APPEND, true, true);
			float scale = 1f;
			//上面barcode
			contentStream.drawImage(pdImageConsignNum, 10, 200, pdImageConsignNum.getWidth() * 1.1f, pdImageConsignNum.getHeight() * scale);

			//收件人
			contentStream.beginText();
			contentStream.newLineAtOffset(30, 170);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 12 );
			contentStream.showText(pb.receiver);
			contentStream.endText();
			//收件人地址
			if (pb.receiverAddr.length()> 15) {
				contentStream.beginText();
				contentStream.newLineAtOffset(30, 160);
				contentStream.setFont(PDType0Font.load(doc, new File(path)), 10 );
				contentStream.showText(pb.receiverAddr.substring(0, 15));
				contentStream.newLine();
				contentStream.newLineAtOffset(0, -10);
				contentStream.showText(pb.receiverAddr.substring(15));
				contentStream.endText();
			} else {
				contentStream.beginText();
				contentStream.newLineAtOffset(30, 160);
				contentStream.setFont(PDType0Font.load(doc, new File(path)), 10 );
				contentStream.showText(pb.receiverAddr);
				contentStream.endText();
			}
			//收件人電話
			if (pb.receiverAddr.length()>15) {
				contentStream.beginText();
				contentStream.newLineAtOffset(30, 140);
				contentStream.setFont(PDType0Font.load(doc, new File(path)), 10 );
				contentStream.showText(pb.receiverTel);
				contentStream.endText();
			} else {
				contentStream.beginText();
				contentStream.newLineAtOffset(30, 150);
				contentStream.setFont(PDType0Font.load(doc, new File(path)), 10 );
				contentStream.showText(pb.receiverTel);
				contentStream.endText();
			}
			//寄件人
			contentStream.beginText();
			contentStream.newLineAtOffset(30, 115);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 11 );
			contentStream.showText(pb.sender);
			contentStream.endText();
			//寄件人地址
			contentStream.beginText();
			contentStream.newLineAtOffset(30, 105);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText(pb.senderAddr);
			contentStream.endText();
			//寄件人電話
			contentStream.beginText();
			contentStream.newLineAtOffset(30, 95);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText(pb.senderTel);
			contentStream.endText();
			//客戶代號
			contentStream.beginText();
			contentStream.newLineAtOffset(48, 75);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText(pb.customerId);
			contentStream.endText();
			//訂單編號
			contentStream.beginText();
			contentStream.newLineAtOffset(48, 63);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText(pb.orderId);
			contentStream.endText();
			//備註
			contentStream.beginText();
			contentStream.newLineAtOffset(42, 50);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText(pb.memo);
			contentStream.endText();
			//收貨日
			contentStream.beginText();
			contentStream.newLineAtOffset(185, 175);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText(pb.receiveDate);
			contentStream.endText();
			//預定配達日
			contentStream.beginText();
			contentStream.newLineAtOffset(185, 145);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText(pb.estimatedRecvDate);
			contentStream.endText();
			//配達時段
			contentStream.beginText();
			contentStream.newLineAtOffset(190, 120);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText("");
			contentStream.endText();
			//發貨所
			contentStream.beginText();
			contentStream.newLineAtOffset(188, 95);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText("");
			contentStream.endText();
			//尺寸
			contentStream.beginText();
			contentStream.newLineAtOffset(185, 70);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 8 );
			contentStream.showText(Math.round(pb.l_length)+"*"+Math.round(pb.w_length)+"*"+Math.round(pb.h_length)+" cm");
			contentStream.newLine();
			contentStream.newLineAtOffset(10, -10);
			contentStream.showText(pb.weight_kg+" kg");
			contentStream.endText();
			//物流業者
			contentStream.beginText();
			contentStream.newLineAtOffset(211, 230);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 13 );
			contentStream.showText(pb.transporter);
			contentStream.endText();
			//託運單號
			contentStream.beginText();
			contentStream.newLineAtOffset(138, 10);
			contentStream.setFont(PDType0Font.load(doc, new File(path)), 18 );
			contentStream.showText(pb.orderId);
			contentStream.endText();
			
			contentStream.close();
			doc.save("D:\\PDF\\testAddText.pdf");
			doc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	;
    }
	
	public void sendMqtt(String topic, String content) {
		try {
			final Mqtt5BlockingClient client = Mqtt5Client.builder()
	        .identifier(UUID.randomUUID().toString())
	        .serverHost("127.0.0.1")
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
            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://127.0.0.1:52000/ServiceMain.asmx");
            androidHttpTransport.call("http://tempuri.org/add_label_file_to_queue", envelope);
            String resultsRequestSOAP = envelope.getResponse().toString();
            result =  (resultsRequestSOAP.contains("000"))? "000,OK" : "回應失敗！錯誤碼："+resultsRequestSOAP; 
            System.out.println("cabResult="+result);
            transferStatus = true;
            cabCount++;
            queue3.add(queue2.poll());
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
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		    try {
				if (queue1.size()>0)
					System.out.println("Queue1:"+queue1.element().toString());
				
				if (queue2.size()>0)
					System.out.println("Queue2:"+queue2.element().toString());
				
				if (queue3.size()>0)
					System.out.println("Queue3:"+queue3.element().toString());
			 
				if (queue2.size() > 0) {
					if (queue2.element() != null) {
						if (test){
							transferStatus = true;
				            cabCount++;
				            queue3.add(queue2.poll());
				            wcsAppFrame.setLabel();
						} else{
						File file = new File("D:\\PDF\\source\\標籤設計(底).pdf");
						try {
							addBarcodeAndText(file, queue2.element().orderId, queue2.element().consignNumber);
							soapTransmit(fileToBase64("D:\\PDF\\testAddText.pdf"));
						} catch (Exception e) {
							e.printStackTrace();
						}
						}
					}
				}
	
				if (queue3.size() > 0 ) {
					if (queue3.element() != null && armStatus && !irStatus) {
						sendMqtt("plc/relay/30f0731bb6c043e79f3b4200f61529e1", "2"); 
						transferStatus = false;
					}
					//測試用
					if (test) {
						if(queue3.element() != null && armStatus) {
							irStatus = true;
						}
					}
				}
				
				if (irStatus && armStatus && !cv4PackPoll) {
					sendMqtt("plc/relay/30f0731bb6c043e79f3b4200f61529e1", "1");
					if (!test) {
						if (doJudge(queue3.element())) {
							JSONObject json = new JSONObject();
							json.put("length", queue3.element().l_length);
							json.put("width", queue3.element().w_length);
							json.put("height", queue3.element().h_length);
							json.put("weight", queue3.element().weight_g);
							wcsAppFrame.setArm();
							armDeviceService.armCmdDo(json);
							WcsApplicationFrame.tallyArmStatus = false;
							packageBox = queue3.poll();
							armStatus = false;	
						}
					} else {
						if (doJudge(queue3.element())) {
							JSONObject json = new JSONObject();
							json.put("length", queue3.element().l_length);
							json.put("width", queue3.element().w_length);
							json.put("height", queue3.element().h_length);
							json.put("weight", queue3.element().weight_g);
							wcsAppFrame.setArm();
							armDeviceService.armCmdDo(json);
							WcsApplicationFrame.tallyArmStatus = false;
							packageBox = queue3.poll();
							irStatus = false;
							armStatus = false;	
						} else {
							irStatus = false;
						}
					}
				}
				if (!irStatus && cv4PackPoll) {
					queue3.poll();
					wcsAppFrame.setError("label", "", "fix");
					cv4PackPoll = false;
				}
		    }catch(Exception e) {
		    	System.out.println("tallyError:"+e.getLocalizedMessage());
		    }
		}
	}
	
	public Boolean doJudge(PackageBox pb) {
		if (pb.weight_g == 0) {
			System.out.println("材積沒重量");
			sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
			wcsAppFrame.setError("label", ErrorMessage.LABEL_WEIGHT_ZERO.value(), "set");
			cv4PackPoll = true;
		} else if (pb.h_length == 0) {
			System.out.println("材積沒高度");
			sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
			wcsAppFrame.setError("label", ErrorMessage.LABEL_HEIGHT_ZERO.value(), "set");
			cv4PackPoll = true;
		} else if (pb.l_length == 0) {
			System.out.println("材積沒長度");
			sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
			wcsAppFrame.setError("label", ErrorMessage.LABEL_LENGTH_ZERO.value(), "set");
			cv4PackPoll = true;
		} else if (pb.w_length == 0) {
			System.out.println("材積沒寬度");
			sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
			wcsAppFrame.setError("label", ErrorMessage.LABEL_WIDTH_ZERO.value(), "set");
			cv4PackPoll = true;
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
					wcsAppFrame.setError("label", ErrorMessage.LABEL_OVER_WEIGHT.value(), "set");
					System.out.println("太重了");
					cv4PackPoll = true;
				}
				break;
			}
			case 2,3: {
				if (pb.weight_kg > 5) {
					sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
					System.out.println("太重了");
					wcsAppFrame.setError("label", ErrorMessage.LABEL_OVER_WEIGHT.value(), "set");
					cv4PackPoll = true;
				}
				break;
			}
			case 7,12: {
				if (pb.weight_kg > 4) {
					sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
					System.out.println("太重了");
					wcsAppFrame.setError("label", ErrorMessage.LABEL_OVER_WEIGHT.value(), "set");
					cv4PackPoll = true;
				}
				break;
			}
			default : {
				sendMqtt("alert/light/20f0731bb6c043e79f3b4200f61529e0", "0");
				System.out.println("找不到箱子類別");
				wcsAppFrame.setError("label", ErrorMessage.LABEL_NO_TYPE.value(), "set");
				cv4PackPoll = true;
				break;
			}
		}

		if (cv4PackPoll) {
			JSONObject json = new JSONObject();
			json.put("orderId", pb.orderId);
			json.put("consign_number", pb.consignNumber);
			json.put("status", "fail");
			json.put("message", "包裹異常");
			mqSender.sendTopic("ArmFinish", json.toString());
			wcsAppFrame.setPackageError("label");
			return false;
		} else
			return true;
	}
}
