package com.wcs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.wcs.autoEx.service.tally.TallyService;
import com.wcs.dao.DbService;
import com.wcs.mq.MqSender;

/**
 * 
 */
@SpringBootApplication
public class Application implements CommandLineRunner{
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
	
	@Autowired
	DbService dbService;

	@Autowired
	MqSender mqSender;
	
	@Autowired
	TallyService tallyService;
	
	@Override
	public void run(String... args) throws Exception {
		dbService.getCustomer(0);
		System.out.println("database is ok");

		mqSender.sendMessage("channel1", "mq start ok (come from spring boot CommandLineRunner)");
		new Thread(tallyService).start();;
	}
}
