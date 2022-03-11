package com.wcs.autoEx.config;

import org.greenrobot.eventbus.EventBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventBusConfig {
	@Bean
	public EventBus eventBus() {
		return new EventBus();
	}
}