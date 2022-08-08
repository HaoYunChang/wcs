package com.wcs.autoEx;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
public class PackageBox extends PackageSize{
	public String orderId="";
	public String transporter="";
	public String consignNumber="";
	public String productName="";
	public String receiver="";
	public String receiverTel="";
	public String receiverAddr="";
	public String sender="";
	public String senderTel="";
	public String senderAddr="";
	public String customerId="";
	public String memo="";
	public String receiveDate="";
	public String estimatedRecvDate="";
	public String packageStatus="";
}
