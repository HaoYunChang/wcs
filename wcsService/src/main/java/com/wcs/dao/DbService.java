package com.wcs.dao;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DbService {
		
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	public List<Map<String,Object>> getCustomer(long customerId) {
		String sql="SELECT * FROM customer where rowId = ?";
		Object[] params = new Object[] {customerId};
		List<Map<String,Object>> rs=jdbcTemplate.queryForList(sql, params);
		return rs;
	}

	@Transactional
	public void updateCustomer(long customerId) {
		System.out.println("update customerId:" +customerId);
	}
}
