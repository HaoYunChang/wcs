package com.wcs.dao;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationService {
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	public Map<String,Object> getLocation(String locationId) {
		String sql="SELECT * FROM location where storage_num = ?";
		Object[] params = new Object[] {locationId};
		Map<String,Object> rs=jdbcTemplate.queryForMap(sql, params);
		return rs;
	}

	@Transactional
	public void updateCustomer(long customerId) {
		System.out.println("update customerId:" +customerId);
	}
}
