package com.wcs.frame;

import javax.swing.JFrame;
import javax.swing.JPanel;
import org.springframework.stereotype.Component;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.Font;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.awt.event.ActionEvent;
import okhttp3.*;

@Component
public class AddPackageFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final OkHttpClient httpClient = new OkHttpClient();
	private JPanel contentPane;
	private JTextField orderIdText;
	private JTextField productNameText;
	private JTextField receiverText;
	private JTextField receiverAddrText;
	private JTextField senderText;
	private JTextField senderAddrText;
	private JLabel customerIdLabel;
	private JLabel memoLabel;
	private JLabel receiveDateLabel;
	private JLabel estimatedRecvDateLabel;
	private JTextField customerIdText;
	private JTextField memoText;
	private JTextField receiveDateText;
	private JTextField estimatedRecvDateText;
	private JButton submitBtn;
	private JButton returnBtn;
	private JTextField consignNumberText;

	/**
	 * Create the frame.
	 */
	public AddPackageFrame() {
		setTitle("新增包裹介面");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(0, 0, 478, 650);
		contentPane = new JPanel();
		contentPane.setLayout(null);
		setContentPane(contentPane);
		
		orderIdText = new JTextField();
		orderIdText.setFont(new Font("新細明體", Font.PLAIN, 14));
		orderIdText.setBounds(114, 13, 125, 21);
		contentPane.add(orderIdText);
		orderIdText.setColumns(10);
		
		productNameText = new JTextField();
		productNameText.setFont(new Font("新細明體", Font.PLAIN, 14));
		productNameText.setBounds(114, 100, 202, 21);
		contentPane.add(productNameText);
		productNameText.setColumns(10);
		
		receiverText = new JTextField();
		receiverText.setFont(new Font("新細明體", Font.PLAIN, 14));
		receiverText.setBounds(114, 151, 125, 21);
		contentPane.add(receiverText);
		receiverText.setColumns(10);
		
		receiverAddrText = new JTextField();
		receiverAddrText.setFont(new Font("新細明體", Font.PLAIN, 14));
		receiverAddrText.setBounds(114, 204, 202, 21);
		contentPane.add(receiverAddrText);
		receiverAddrText.setColumns(10);
		
		senderText = new JTextField();
		senderText.setFont(new Font("新細明體", Font.PLAIN, 14));
		senderText.setBounds(114, 257, 125, 21);
		contentPane.add(senderText);
		senderText.setColumns(10);
		
		senderAddrText = new JTextField();
		senderAddrText.setFont(new Font("新細明體", Font.PLAIN, 14));
		senderAddrText.setBounds(114, 306, 202, 21);
		contentPane.add(senderAddrText);
		senderAddrText.setColumns(10);
		
		JLabel orderIdLabel = new JLabel("訂單編號");
		orderIdLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		orderIdLabel.setBounds(27, 10, 64, 28);
		contentPane.add(orderIdLabel);
		
		JLabel productNameLabel = new JLabel("產品名稱");
		productNameLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		productNameLabel.setBounds(27, 100, 73, 21);
		contentPane.add(productNameLabel);
		
		JLabel receiverLabel = new JLabel("收件人");
		receiverLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		receiverLabel.setBounds(27, 153, 60, 18);
		contentPane.add(receiverLabel);
		
		JLabel receiverAddrLabel = new JLabel("收件地址");
		receiverAddrLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		receiverAddrLabel.setBounds(27, 206, 73, 18);
		contentPane.add(receiverAddrLabel);
		
		JLabel senderLabel = new JLabel("寄件人");
		senderLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		senderLabel.setBounds(27, 257, 64, 19);
		contentPane.add(senderLabel);
		
		JLabel senderAddrLabel = new JLabel("寄件地址");
		senderAddrLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		senderAddrLabel.setBounds(27, 307, 73, 21);
		contentPane.add(senderAddrLabel);
		
		customerIdLabel = new JLabel("客戶代碼");
		customerIdLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		customerIdLabel.setBounds(27, 357, 69, 18);
		contentPane.add(customerIdLabel);
		
		memoLabel = new JLabel("備註");
		memoLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		memoLabel.setBounds(27, 502, 60, 18);
		contentPane.add(memoLabel);
		
		receiveDateLabel = new JLabel("收貨日");
		receiveDateLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		receiveDateLabel.setBounds(27, 406, 60, 18);
		contentPane.add(receiveDateLabel);
		
		estimatedRecvDateLabel = new JLabel("預定配達日");
		estimatedRecvDateLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		estimatedRecvDateLabel.setBounds(27, 454, 89, 21);
		contentPane.add(estimatedRecvDateLabel);
		
		customerIdText = new JTextField();
		customerIdText.setFont(new Font("新細明體", Font.PLAIN, 14));
		customerIdText.setBounds(114, 355, 125, 21);
		contentPane.add(customerIdText);
		customerIdText.setColumns(10);
		
		memoText = new JTextField();
		memoText.setFont(new Font("新細明體", Font.PLAIN, 14));
		memoText.setBounds(114, 500, 202, 21);
		contentPane.add(memoText);
		memoText.setColumns(10);
		
		receiveDateText = new JTextField();
		receiveDateText.setFont(new Font("新細明體", Font.PLAIN, 14));
		receiveDateText.setBounds(114, 404, 125, 21);
		contentPane.add(receiveDateText);
		receiveDateText.setColumns(10);
		
		estimatedRecvDateText = new JTextField();
		estimatedRecvDateText.setFont(new Font("新細明體", Font.PLAIN, 14));
		estimatedRecvDateText.setBounds(115, 453, 124, 21);
		contentPane.add(estimatedRecvDateText);
		estimatedRecvDateText.setColumns(10);
		
		submitBtn = new JButton("送出");
		submitBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {	
				if (orderIdText.getText().isBlank()) {
					JOptionPane.showMessageDialog(null, "錯誤提示: 訂單編號為空");
					return;
				}
				RequestBody formBody = new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("orderId", orderIdText.getText())
						.addFormDataPart("consignNumber", consignNumberText.getText())
						.addFormDataPart("productName", productNameText.getText())
						.addFormDataPart("receiver", receiverText.getText())
						.addFormDataPart("receiverAddr", receiverAddrText.getText())
						.addFormDataPart("sender", senderText.getText())
						.addFormDataPart("senderAddr", senderAddrText.getText())
						.addFormDataPart("customerId", customerIdText.getText())
						.addFormDataPart("receiveDate", receiveDateText.getText())
						.addFormDataPart("estimatedRecvDate", estimatedRecvDateText.getText())
						.addFormDataPart("memo", memoText.getText())
						.build();
				
		        Request request = new Request.Builder()
		                .url("http://localhost:9102/wcs_api/autoDevice/outbound/package/")
		                .addHeader("Content-Type", "multipart/form-data")
		                .post(formBody)
		                .build();
		        
		        try {
					Response response = httpClient.newCall(request).execute();
					if (response.code() == 200)
						JOptionPane.showMessageDialog(null, "新增成功");
					else
						JOptionPane.showMessageDialog(null, "錯誤提示:"+response.body().string());
					
					dispose();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		submitBtn.setFont(new Font("新細明體", Font.PLAIN, 16));
		submitBtn.setBounds(94, 559, 85, 23);
		contentPane.add(submitBtn);
		
		returnBtn = new JButton("返回");
		returnBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		returnBtn.setFont(new Font("新細明體", Font.PLAIN, 16));
		returnBtn.setBounds(292, 559, 85, 23);
		contentPane.add(returnBtn);
		
		JLabel consignNumberLabel = new JLabel("託運單號");
		consignNumberLabel.setFont(new Font("新細明體", Font.PLAIN, 16));
		consignNumberLabel.setBounds(27, 51, 64, 28);
		contentPane.add(consignNumberLabel);
		
		consignNumberText = new JTextField();
		consignNumberText.setFont(new Font("新細明體", Font.PLAIN, 14));
		consignNumberText.setColumns(10);
		consignNumberText.setBounds(114, 54, 125, 21);
		contentPane.add(consignNumberText);
	}
}
