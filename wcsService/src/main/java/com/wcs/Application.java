package com.wcs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import com.wcs.autoEx.service.armConnection.ArmDeviceService;
import com.wcs.autoEx.service.tally.TallyService;
import com.wcs.frame.WcsApplicationFrame;

/**
 * 
 */
@SpringBootApplication
@EnableAsync
public class Application extends JFrame implements CommandLineRunner{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2839641139874868326L;
	
	@Autowired
	TallyService tallyService;
	
	@Autowired
	ArmDeviceService armDeviceService;
	
	@Autowired
	WcsApplicationFrame frame;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Override
	public void run(String... args) throws Exception {
		new Thread(tallyService).start();
		new Thread(armDeviceService).start();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					frame = new WcsApplicationFrame();
					frame.setVisible(true);		
					Timer t = new Timer(1000, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							frame.doShow();
							frame.repaint();
							frame.revalidate();
						}
					});
					t.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
