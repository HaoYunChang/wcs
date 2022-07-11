package com.wcs.dao;

import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ActionLogService {
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	final String INSERT_QUERY = "insert into action_log (storage_num, destination, command, created_time, status)"
			+ " values (?, ?, ?, ?, ?)";
	
	final String UPDATE_QUERY = "update action_log set status =1 where storage_num = ? and destination = ? and status=0";
	
	public int save(String storageNum, String destination, String command) {
		return jdbcTemplate.update(INSERT_QUERY, storageNum, destination, command, LocalDateTime.now(), 0);
	}
	
	public Map<String,Object> getLocation(String locationId) {
		String sql="SELECT * FROM action_log al left join location l on al.storage_num = l.storage_num where l.shelf_num = ? and al.status=0";
		Object[] params = new Object[] {locationId};
		Map<String,Object> rs=jdbcTemplate.queryForMap(sql, params);
		return rs;
	}
	
	public int update(String storageNum, String destination) {
		return jdbcTemplate.update(UPDATE_QUERY, storageNum, destination);
	}
}
