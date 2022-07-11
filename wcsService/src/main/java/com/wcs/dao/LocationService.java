package com.wcs.dao;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
	@Autowired
	JdbcTemplate jdbcTemplate;
	final String UPDATE_QUERY = "update location set shelf_num=?, direction=? where storage_num = 'CAP101'";
	
	
	public Map<String,Object> getLocation(String locationId) {
		String sql="SELECT * FROM location where storage_num = ?";
		Object[] params = new Object[] {locationId};
		Map<String,Object> rs=jdbcTemplate.queryForMap(sql, params);
		return rs;
	}
	
	public Map<String,Object> getTestWMSLocation(String locationId) {
		String sql="SELECT * FROM location where test_location = ?";
		Object[] params = new Object[] {locationId};
		Map<String,Object> rs=jdbcTemplate.queryForMap(sql, params);
		return rs;
	}
	
	public Map<String,Object> getWMSLocation(String locationId) {
		String sql="SELECT * FROM location where shelf_location = ?";
		Object[] params = new Object[] {locationId};
		Map<String,Object> rs=jdbcTemplate.queryForMap(sql, params);
		return rs;
	}
	
	public int update(String storageNum, String direction) {
		return jdbcTemplate.update(UPDATE_QUERY, storageNum, direction);
	}
}
