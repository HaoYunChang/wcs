package com.wcs.dao;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wcs.autoEx.PackageBox;

@Service
public class FinishedPackageService {
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	final String INSERT_QUERY = "insert into finished_package (order_id, consign_number, product_name, receiver, receiver_addr, sender, sender_addr, customer_id, memo, receive_date)"
			+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	public Map<String,Object> getLocation(String locationId) {
		String sql="SELECT * FROM location where storage_num = ?";
		Object[] params = new Object[] {locationId};
		Map<String,Object> rs=jdbcTemplate.queryForMap(sql, params);
		return rs;
	}
	
	public int save(PackageBox pb) {
		return jdbcTemplate.update(INSERT_QUERY, pb.getOrderId(), pb.getConsignNumber(), pb.getProductName(), pb.receiver,
				pb.getReceiverAddr(), pb.getSender(), pb.getSenderAddr(), pb.customerId, pb.memo, pb.getReceiveDate());
	}
}
