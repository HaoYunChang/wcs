package com.wcs.autoEx;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.springframework.beans.factory.annotation.Autowired;

import com.atop.autoEx.IExceptionEvent;
import com.atop.autoEx.IGoodEndEvent;
import com.atop.autoEx.INormalEndEvent;
import com.atop.autoEx.IStateChangeEvent;
import com.atop.autoEx.ITimeoutEvent;

public class AexServiceBase {
	
	@Autowired
	protected EventBus eventBus;

	@PostConstruct
	public void init() {
		eventBus.register(this);
	}

	@PreDestroy
	protected void destroy() {
		eventBus.unregister(this);
	}

	// 命令生命週期起於開始執行到命令執行完成或非正常執行完畢(TimeOut or Exception)
	// 生命週期間會有內部幾個狀態變化,如將貨A點拉到B點,她不會瞬間移動,她會A,a1,a2,a3..b3,b2,b1
	// 又如搭高鐵從台北坐到台中,到桃園通知一次,到新竹通一次,當然每個裝置,命令狀態不同,比如坐慢車桃園,新竹還會加苗栗站
	@Subscribe
	public void onCommandStateChange(IStateChangeEvent event) {
//        println("onCommandStateChange commandId:${event.getCommand().getId()}")
	}

	// AEX or 底層原生機器驅動程式等發生不名的錯誤
	@Subscribe
	public void onCommandException(IExceptionEvent event) {
//        println("onCommandException commandId:${event.getCommand().getId()}")
	}

	// 命令逾時,比如貨A拉到B點,正常60秒要到,等了120秒卻沒有通知執行完畢
	@Subscribe
	public void onCommandTimeOut(ITimeoutEvent event) {
//        println("onCommandTimeOut commandId:${event.getCommand().getId()}")
	}

	// 這個命令通常執行完成但結果與預期不同,比如貨品從A點拉到B點,到B點發生異樣,車子演算法把貨拉到C點
	@Subscribe
	public void onCommandNormalEnd(INormalEndEvent event) {
//        println("onCommandNormalEnd commandId:${event.getCommand().getId()}")
	}

	// 這個命令執行完成,比如貨品從A點拉到B點,到B點時通知
	@Subscribe
	public void onCommandGoodEnd(IGoodEndEvent event) {
//        println("onCommandGoodEnd commandId:${event.getCommand().getId()}")
	}
}