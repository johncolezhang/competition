package com.szu.model;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.szu.algorithm.Vehicle;
import com.szu.util.Classify;

public class Schedule {
	private int fitting = 0;// 总适应值
	private int time = 0;// 总时间，除开处罚
	private Vehicle vehicle = null;
	private final static int MAX_WEIGHT = 140;
	private final static int POP_SIZE = 100;
	private final static int STEP = 15;
	private final static int STOP_TIME = 620;
	
/**
 * 遗传算法
 */
	 public void run() {
		ServiceData.loadData();
		int i = 0;
		int totalDepot = 0, totalO2o = 0, useDepot = 0, useO2o = 0, o2oclass = 0, dui = 0;
		 //先处理静态点
		for (Entry<String, List<Order>> entry : ServiceData.depotMaps
				.entrySet()) {
			String key = entry.getKey();//仓库名
			List<Order> depotOrders = ServiceData.depotMaps.get(key);//该仓库的所有订单
			totalDepot += depotOrders.size();//总网点订单数
			totalO2o += ServiceData.o2oOrderMaps.get(key).size();//该仓库的总o2o订单数
			if (ServiceData.o2oOrderMaps.get(key).size() > 0)
				dui++;
			Map<String, List<Order>> o2oClassMap = Classify.classifyO2oByTime(
					depotOrders, ServiceData.o2oOrderMaps.get(key));//根据时间将o2o排序,仓库对o2o订单列表
			for (List list : o2oClassMap.values()) {
				o2oclass += list.size();//累加o2o订单数
			}
			List<List<Order>> depotLists = Classify.classifyDepot(depotOrders,
					o2oClassMap);//个数不大于140的所有订单列表的集合
			List<ChunkOrder> chunkOrders = Classify.divideDepotbyO2oOrders(
					depotLists, o2oClassMap);
			vehicle = null;
			int minStartTime = getMinStartTime(chunkOrders);
			while (chunkOrders.size() != 0) {
				// 超时
				if (vehicle == null || vehicle.getTime() >= Schedule.STOP_TIME) {
					if (vehicle != null) {// 完成一次演化
						List<ResultOrder> list = vehicle.getServicedLists();//获取结果列表
						ServiceData.ResultMaps.put(vehicle.getCourier_id(),
								list);//加入调度列表中
						fitting += vehicle.getFitting();//累加适应值
						time += list.get(list.size() - 1).Departure;//最后的到达时间
					}
					vehicle = new Vehicle(ServiceData.courierLists.get(i),
							POP_SIZE);//pop_size100
					System.out.println(vehicle.getCourier_id());
					i++;
				}
				ChunkOrder chunkOrder = null;
				// ???/
				if (vehicle.getTime() >= minStartTime - Schedule.STEP) {//到达了订单的起始时间
					for (ChunkOrder chunkOrder2 : chunkOrders) {
						if ((chunkOrder2.startTime - Schedule.STEP - vehicle
								.getTime()) > 0) {//o2o开始时间>货车当前时间+15分钟
							chunkOrder = chunkOrder2;
							break;
						}
					}
					chunkOrders.remove(chunkOrder);
				}
				if (chunkOrder == null) {
					chunkOrder = chunkOrders.get(0);
					chunkOrders.remove(0);
				}
				vehicle.addOrder(chunkOrder.depotOrders, chunkOrder.o2oOrders);//添加订单给机车
				// vehicle.addO2oOrders(chunkOrder.o2oOrders);
				useDepot += chunkOrder.depotOrders.size();
				useO2o += chunkOrder.o2oOrders.size();
				vehicle.run();//机车开始送货
			}
			minStartTime = getMinStartTime(chunkOrders);
			List<ResultOrder> list = vehicle.getServicedLists();
			ServiceData.ResultMaps.put(vehicle.getCourier_id(), list);
			fitting += vehicle.getFitting();
			time += list.get(list.size() - 1).Departure;
		}
		System.out.println(totalDepot + " " + totalO2o);
		System.out.println(useDepot + " " + useO2o + "  " + o2oclass + " "
				+ dui);
		System.out.println("fitting" + fitting);
	}
	
	/*
	 * 订单中最小的起始时间
	 */
	private int getMinStartTime(List<ChunkOrder> chunkOrders) {
		int minStart = Integer.MAX_VALUE;
		for (ChunkOrder chunkOrder : chunkOrders) {
			if (chunkOrder.startTime != 0) {
				minStart = Math.min(minStart, chunkOrder.startTime);
			}
		}
		return minStart;
	}

	public int getTime() {
		return time;
	}

	public int getFitting() {
		return fitting;
	}
}
