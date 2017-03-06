package com.szu.model;

import java.util.ArrayList;
import java.util.List;

import com.szu.util.Rule;
//货物订单
public class ChunkOrder {
	public List<Order> depotOrders = new ArrayList<>();//该次的所有静态订单
	public List<Order> o2oOrders = new ArrayList<>();//该次该点的所有目的地附近的o2o订单(不超过30分钟的)
	public int startTime = 0;//静态订单集合的所有目的点附近的最早的o2o起始时间
	
	public List<Order> getDepotOrders() {
		return depotOrders;
	}
	public void setDepotOrders(List<Order> depotOrders) {
		this.depotOrders = depotOrders;
	}
	public List<Order> getO2oOrders() {
		return o2oOrders;
	}
	public void setO2oOrders(List<Order> o2oOrders) {
		this.o2oOrders = o2oOrders;
	}
	public int getStartTime() {
		return startTime;
	}
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}
	
	public int getDepotOderTime() {
		int time = 0;
		Order preOrder = depotOrders.get(0);
		time += Rule.distanceTime(ServiceData.localPacageMaps.get(preOrder.src_id),
				ServiceData.localPacageMaps.get(preOrder.dest_id));//第一单的时间
		if(depotOrders.size() >= 1) {
			for(int i = 1; i< depotOrders.size(); i++) {
				time += Rule.distanceTime(ServiceData.localPacageMaps.get(preOrder.dest_id),
						ServiceData.localPacageMaps.get(depotOrders.get(i).src_id));//间距
				time += Rule.distanceTime(ServiceData.localPacageMaps.get(depotOrders.get(i).src_id),
						ServiceData.localPacageMaps.get(depotOrders.get(i).dest_id));
				preOrder = depotOrders.get(i);
			}
		}
		return time;
	}
}
