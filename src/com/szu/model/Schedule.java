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
	private final static int STOP_TIME = 540;
	
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
			System.out.println("从" + key + "出发的所有订单：");
			List<Order> depotOrders = ServiceData.depotMaps.get(key);//该仓库的所有订单
			totalDepot += depotOrders.size();//总网点订单数
			totalO2o += ServiceData.o2oOrderMaps.get(key).size();//该仓库的总o2o订单数
			if (ServiceData.o2oOrderMaps.get(key).size() > 0)
				dui++;
			Map<String, List<Order>> o2oClassMap = Classify.classifyO2oByTime(
					depotOrders, ServiceData.o2oOrderMaps.get(key));//静态订单目的地周围的o2o订单，key值为静态订单目的地
			for (List list : o2oClassMap.values()) {
				o2oclass += list.size();//累加o2o订单数
			}
			List<List<Order>> depotLists = Classify.classifyDepot(depotOrders,
					o2oClassMap);//按距离排好的订单块，每个个数不大于140且总时不超540分钟的所有订单列表的集合
			List<ChunkOrder> chunkOrders = Classify.divideDepotbyO2oOrders(
					depotLists, o2oClassMap);//chunkOrders按照o2o的开始时间排好序
			vehicle = null;
			int minStartTime = getMinStartTime(chunkOrders);//chunkorder中的最早o2o起始时间
			while (chunkOrders.size() != 0) {
				// 超时
				if (vehicle == null || vehicle.getTime() >= Schedule.STOP_TIME) {//还有180分钟也换个快递员*****因此造成很多罚分
					if (vehicle != null) {// 完成一次演化
						List<ResultOrder> list = vehicle.getServicedLists();//获取结果列表
						ServiceData.ResultMaps.put(vehicle.getCourier_id(),
								list);//加入调度列表中
						System.out.println("单次累加：" + vehicle.getFitting());
						fitting += vehicle.getFitting();//累加适应值
						time += list.get(list.size() - 1).Departure;//最后的到达时间
					}
					vehicle = new Vehicle(ServiceData.courierLists.get(i),//从0开始，逐渐++
							POP_SIZE);//pop_size100
					System.out.println(vehicle.getCourier_id());//输出快递员id
					i++;
				}
				ChunkOrder chunkOrder = null;
				
				if (vehicle.getTime() >= minStartTime - Schedule.STEP) {//添加新的一单
					for (ChunkOrder chunkOrder2 : chunkOrders) {
						if ((chunkOrder2.startTime > Schedule.STEP + vehicle.getTime()) && 
								true) {//o2o开始时间>货车当前时间+15，*****有可能o2o会超时
							chunkOrder = chunkOrder2;
							break;
						}
					}
					chunkOrders.remove(chunkOrder);
				}//****这里尝试用vehicle当前的点来选chunkorder
				
				if (chunkOrder == null) {
					chunkOrder = chunkOrders.get(0);
					chunkOrders.remove(0);
				}
				System.out.println("一个chunkorder的时间：" + chunkOrder.getDepotOderTime());
				vehicle.addOrder(chunkOrder.depotOrders, chunkOrder.o2oOrders);//添加订单给机车和初始化
				
				
				// vehicle.addO2oOrders(chunkOrder.o2oOrders);
				useDepot += chunkOrder.depotOrders.size();
				useO2o += chunkOrder.o2oOrders.size();
				vehicle.run();//机车开始送货
				
				System.out.println("vehicle 跑完一个chunk的时间" + vehicle.getTime());
			}
			minStartTime = getMinStartTime(chunkOrders);
			List<ResultOrder> list = vehicle.getServicedLists();
			ServiceData.ResultMaps.put(vehicle.getCourier_id(), list);
			fitting += vehicle.getFitting();
			time += list.get(list.size() - 1).Departure;
		}
		System.out.println("totalDepot " + totalDepot + " totalO2o " + totalO2o);
		System.out.println(useDepot + " " + useO2o + "  " + o2oclass + " "
				+ "o2o对 " + dui);
		System.out.println("fitting " + fitting);
	}
	
	/**
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
