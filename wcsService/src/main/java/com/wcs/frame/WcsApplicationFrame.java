package com.wcs.frame;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import com.wcs.autoEx.PackageBox;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.awt.*;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.border.LineBorder;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.ListSelectionModel;
import javax.swing.JTextField;

@Component
public class WcsApplicationFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final OkHttpClient httpClient = new OkHttpClient();
	private JPanel contentPane;
	private JPanel workStationPanel;
	private JPanel volumeDetectPanel;
	private JPanel labelPanel;
	private JPanel armPanel;
	private JLabel armLabel;
	private JLabel labelLabel;
	private JLabel volumeDetectLabel;
	private JLabel workStationLabel;
	private JTable workingTable;
	private static ArrayList<PackageBox> pendingWorkList = new ArrayList<PackageBox>(); 
	private static ArrayList<PackageBox> workingList = new ArrayList<PackageBox>(); 
	public static int cv1 = 0;
	public static int cv2 = 0;
	public static int cv3 = 0;
	public static int cv4 = 0;
	private static int doneCount = 0;
	private static Boolean workStatus = false;
	private static Boolean volumeStatus = false;
	private static Boolean labelStatus = false;
	private static Boolean armStatus = false;
	public static Boolean tallyArmStatus = false;
	public static Boolean armConnection = false;
	private JScrollPane workingScrollPane;
	private JTable pendingWorkTable;
	private JLabel pendingWorkLabel;
	private JPanel pendingWorkPanel;
	private JScrollPane pendingWorkScrollPane;
	private JTextField armErrorText;
	private static Map<String, String> errorMap = new HashMap<String, String>();
	private JTextField labelErrorText;
	/**
	 * Create the frame.
	 */
	public WcsApplicationFrame() {
		setForeground(Color.LIGHT_GRAY);
		setTitle("流道狀態介面");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(0, 0, 1551, 815);
		contentPane = new JPanel();
		contentPane.setBackground(Color.LIGHT_GRAY);
		contentPane.setForeground(new Color(192, 192, 192));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		try {
			BufferedImage background = ImageIO.read(this.getClass().getClassLoader().getResource("background.png"));
			armPanel = new JPanel();
			armPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
			armPanel.setBounds(75, 452, 120, 35);
			armPanel.setBackground(Color.GREEN);
			contentPane.add(armPanel);
			armLabel = new JLabel("機械手臂");
			armLabel.setBounds(0, 0, 102, 30);
			armPanel.add(armLabel);
			armLabel.setFont(new Font("標楷體", Font.PLAIN, 24));
			
			labelPanel = new JPanel();
			labelPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
			labelPanel.setBounds(360, 452, 150, 35);
			labelPanel.setBackground(Color.GREEN);
			contentPane.add(labelPanel);
			labelLabel = new JLabel("自動貼標機");
			labelLabel.setBounds(0, 0, 120, 30);
			labelPanel.add(labelLabel);
			labelLabel.setFont(new Font("標楷體", Font.PLAIN, 24));
			
			volumeDetectPanel = new JPanel();
			volumeDetectPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
			volumeDetectPanel.setBounds(803, 452, 160, 35);
			volumeDetectPanel.setBackground(Color.GREEN);
			contentPane.add(volumeDetectPanel);
			volumeDetectPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			volumeDetectLabel = new JLabel("自動材積辨識");
			volumeDetectPanel.add(volumeDetectLabel);
			volumeDetectLabel.setFont(new Font("標楷體", Font.PLAIN, 24));
			
			workStationPanel = new JPanel();
			workStationPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
			workStationPanel.setBounds(1156, 452, 130, 35);
			workStationPanel.setBackground(Color.GREEN);
			contentPane.add(workStationPanel);
			workStationPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			workStationLabel = new JLabel("理貨工作站");
			workStationPanel.add(workStationLabel);
			workStationLabel.setFont(new Font("標楷體", Font.PLAIN, 24));
			JLabel backgroundLabel = new JLabel(new ImageIcon(background));
			backgroundLabel.setBackground(Color.WHITE);
			backgroundLabel.setBounds(10, 10, 1329, 483);
			contentPane.add(backgroundLabel);
			pendingWorkScrollPane = new JScrollPane();
			pendingWorkScrollPane.setBounds(1062, 544, 277, 224);
			contentPane.add(pendingWorkScrollPane);
			pendingWorkTable = new JTable();
			pendingWorkTable.setBackground(Color.WHITE);
			pendingWorkScrollPane.setViewportView(pendingWorkTable);
			pendingWorkTable.setModel(new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
					"", "\u8A02\u55AE\u7DE8\u865F"
				}
			));
			pendingWorkTable.getColumnModel().getColumn(0).setPreferredWidth(50);
			pendingWorkTable.getColumnModel().getColumn(1).setPreferredWidth(312);
			pendingWorkTable.getTableHeader().setPreferredSize(new Dimension(pendingWorkTable.getTableHeader().getWidth(), 30));
			pendingWorkTable.getTableHeader().setFont(new Font("標楷體", Font.PLAIN, 18));
			pendingWorkTable.setRowHeight(25);
			pendingWorkTable.setFont(new Font("標楷體", Font.PLAIN, 18));

			workingScrollPane = new JScrollPane();
			workingScrollPane.setBounds(10, 503, 1042, 265);
			contentPane.add(workingScrollPane);
			workingTable = new JTable();
			workingTable.setEnabled(false);
			workingTable.setBackground(Color.WHITE);
			workingScrollPane.setViewportView(workingTable);
			workingTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			workingTable.setModel(new DefaultTableModel(
				new Object[][] {
				},
				new String[] {
					"", "\u8A02\u55AE\u7DE8\u865F", "\u9577(cm)", "\u5BEC(cm)", "\u9AD8(cm)", "\u91CD(kg)", "\u8A17\u904B\u55AE\u865F", "\u5305\u88F9\u72C0\u614B"
				}
			));
			workingTable.getColumnModel().getColumn(0).setPreferredWidth(50);
			workingTable.getColumnModel().getColumn(1).setPreferredWidth(210);
			workingTable.getColumnModel().getColumn(2).setPreferredWidth(120);
			workingTable.getColumnModel().getColumn(3).setPreferredWidth(120);
			workingTable.getColumnModel().getColumn(4).setPreferredWidth(120);
			workingTable.getColumnModel().getColumn(5).setPreferredWidth(120);
			workingTable.getColumnModel().getColumn(6).setPreferredWidth(210);
			workingTable.getColumnModel().getColumn(7).setPreferredWidth(150);
			workingTable.getTableHeader().setFont(new Font("標楷體", Font.PLAIN, 18));
			workingTable.getTableHeader().setPreferredSize(new Dimension(workingTable.getTableHeader().getWidth(), 30));
			workingTable.setFont(new Font("標楷體", Font.PLAIN, 18));
			workingTable.setRowHeight(25);
			
			BufferedImage addPackage = ImageIO.read(this.getClass().getClassLoader().getResource("newpackage.png"));
			JButton addPackageBtn = new JButton("", new ImageIcon(addPackage));
			addPackageBtn.setFont(new Font("標楷體", Font.PLAIN, 22));
			addPackageBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					AddPackageFrame addPackageFrame = new AddPackageFrame();
					addPackageFrame.setVisible(true);
				}
			});
			addPackageBtn.setBounds(1349, 135, 178, 106);
			contentPane.add(addPackageBtn);

			BufferedImage clear = ImageIO.read(this.getClass().getClassLoader().getResource("clear.png"));
			JButton clearButton = new JButton("", new ImageIcon(clear));
			clearButton.setFont(new Font("標楷體", Font.PLAIN, 22));
			clearButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int result = JOptionPane.showConfirmDialog(null,
							"若「確定清除」將清空作業中(流道上)包裹資料，\r\n"
							+ "請將流道上包裹移除，\r\n"
							+"並自WMS[裝箱檢查]重新確認理貨。",
                            "是否確定要清除資料？", JOptionPane.WARNING_MESSAGE);
					if (result==JOptionPane.YES_OPTION) {
						setError("label", "", "fix");
						setError("labelConnection", "", "fix");
						workingList.removeAll(workingList);
						cv2 = 0;
						cv3 = 0;
						cv4 = 0;
						doneCount = 0;
						((DefaultTableModel) workingTable.getModel()).setRowCount(0);
						((DefaultTableModel) workingTable.getModel()).fireTableDataChanged();
						Request request = new Request.Builder()
								.url("http://localhost:9102/wcs_api/autoDevice/outbound/queueClear/")
								.build();
						try {
							Response response = httpClient.newCall(request).execute();
							if (response.code() == 200)
								JOptionPane.showMessageDialog(null, "清除成功");
							else
								JOptionPane.showMessageDialog(null, "錯誤提示:"+response.body().string());
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			});
			clearButton.setBounds(1349, 295, 178, 106);
			contentPane.add(clearButton);
			
			pendingWorkPanel = new JPanel();
			pendingWorkPanel.setBackground(Color.WHITE);
			pendingWorkPanel.setBorder(new LineBorder(new Color(0, 0, 0)));
			pendingWorkPanel.setBounds(1062, 503, 277, 41);
			contentPane.add(pendingWorkPanel);
			pendingWorkPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
			
			pendingWorkLabel = new JLabel("待作業清單");
			pendingWorkPanel.add(pendingWorkLabel);
			pendingWorkLabel.setBackground(Color.WHITE);
			pendingWorkLabel.setFont(new Font("標楷體", Font.BOLD, 28));
			
			armErrorText = new JTextField();
			armErrorText.setForeground(Color.RED);
			armErrorText.setFont(new Font("標楷體", Font.PLAIN, 22));
			armErrorText.setBounds(28, 24, 213, 94);
			contentPane.add(armErrorText);
			armErrorText.setColumns(30);
			armErrorText.setEditable(false);
			
			labelErrorText = new JTextField();
			labelErrorText.setEditable(false);
			labelErrorText.setFont(new Font("標楷體", Font.PLAIN, 22));
			labelErrorText.setForeground(Color.RED);
			labelErrorText.setBounds(309, 24, 218, 94);
			contentPane.add(labelErrorText);
			labelErrorText.setColumns(30);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void doShow() {
		checkError();
		if (tallyArmStatus &&  workingList.size() > 0 && workingList.size() > doneCount) {
			if (workingList.get(doneCount).packageStatus.equals("手臂作業中")) {
				workingList.get(doneCount).packageStatus = "完成出貨";
				refreshDataModel(workingTable, "working");
				doneCount++;
			}
		}
		if (armStatus) {
			refreshDataModel(workingTable, "working");
			armStatus = false;
		}
		if (labelStatus) {
			refreshDataModel(workingTable, "working");
			labelStatus = false;
		}
		if (volumeStatus) {
			refreshDataModel(pendingWorkTable, "pending");
			refreshDataModel(workingTable, "working");
			volumeStatus = false;
		}
		if (workStatus) {
			refreshDataModel(pendingWorkTable, "pending");
			workStatus = false;	
		}
	}
	
	private void refreshDataModel(JTable table, String name) {
		((DefaultTableModel) table.getModel()).setRowCount(0);
		if (name.equals("working")) {
			for (int i=workingList.size()-1; i>-1; i--) {
				PackageBox pb = workingList.get(i);
				((DefaultTableModel) table.getModel()).addRow(new String[] {String.format("%03d", workingList.size()-i), pb.orderId, String.valueOf(pb.l_length),
						String.valueOf(pb.w_length), String.valueOf(pb.h_length), String.valueOf(pb.weight_kg), pb.consignNumber, pb.packageStatus});
			}
		}
		else {
			((DefaultTableModel) pendingWorkTable.getModel()).setRowCount(0);
			for (int i=0; i<pendingWorkList.size(); i++) {
				((DefaultTableModel) table.getModel()).addRow(new String[] {String.format("%03d", i+1), pendingWorkList.get(i).orderId});
			}
		}
	}
	
	public void addPackage(PackageBox pb){
		cv1++;
		pendingWorkList.add(pb);
		workStatus = true;
	}
	
	public void setPackageVolumn(String vd) {
		JSONObject data = new JSONObject(vd);
		PackageBox pb = pendingWorkList.get(0);
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
			}
			pb.packageStatus = "完成材積量測";
		}
		else {
			pb.h_length = (float) 0;	//height
			pb.l_length = (float) 0;  //width
			pb.w_length = (float) 0;	//depth
			pb.weight_g = (float) 0;
			pb.weight_kg = (float) 0;
			pb.cmb_g = (float) 0;
			pb.cmb_unit = (float) 0;
			pb.packageStatus = "材積設備回傳失敗";
		}
		workingList.add(pb);
		pendingWorkList.remove(0);
		cv2++;
		volumeStatus = true;
	}

	public void setLabel() {
		if (workingList.get(cv3).packageStatus.equals("完成材積量測"))
			workingList.get(cv3).packageStatus = "貼標作業中";
		cv3++;
		labelStatus = true;
	}
	
	public void setArm() {
		if (workingList.get(cv4).packageStatus.equals("貼標作業中"))
			workingList.get(cv4).packageStatus = "手臂作業中";
		cv4++;
		armStatus = true;
	}
	
	private void checkError() {
		armPanel.setBackground(Color.GREEN);
		armErrorText.setText("");
		labelPanel.setBackground(Color.GREEN);
		labelErrorText.setText("");
		if (!armConnection) {
			armPanel.setBackground(Color.ORANGE);
			armErrorText.setText("手臂未連線");
		} else {
			armPanel.setBackground(Color.GREEN);
			armErrorText.setText("");
		}
		if (!errorMap.isEmpty()) {
			for (Object error : errorMap.keySet()) {
				if (error.equals("arm")) {
					armPanel.setBackground(Color.ORANGE);
					armErrorText.setText(errorMap.get(error));
				} else {
					labelPanel.setBackground(Color.ORANGE);
					if (labelErrorText.getText().isBlank())
						labelErrorText.setText(errorMap.get(error));
					else
						labelErrorText.setText(labelErrorText.getText()+"\r\n"+errorMap.get(error));
				}
			}
		}
	}
	
	public void setError(String device, String message, String fix) {
		if (fix.equals("set"))
			errorMap.put(device, message);
		else
			errorMap.remove(device);
	}
}
