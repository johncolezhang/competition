package com.szu.util;

import java.util.List;

import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;

public class Rule {
	public static final double R = 6378.137;
	public static final int MAX_WEIGHT = 140;

	// 每一个路都有一个，用来记录收货员回到仓库点的时间

	public static boolean isWeightLegal(List<ResultOrder> lists) {//总重量不超过140
		int totalWeight = 0;
		for (ResultOrder resultOrder : lists) {
			totalWeight += resultOrder.Amount;
			if (totalWeight > 140)
				return false;
		}
		return true;
	}

	public static boolean isOverWeight(List<ResultOrder> lists,//加入一个新订单后是否超重
			ResultOrder insertOrder, int index) {
		int totalWeight = 0;
		for (int i = 0; i < index; i++) {
			totalWeight += lists.get(i).Amount;
			if (totalWeight > 140)
				return true;
		}
		totalWeight += insertOrder.Amount;
		if (totalWeight > 140)
			return true;
		for (int i = index; i < lists.size(); i++) {
			totalWeight += lists.get(i).Amount;
			if (totalWeight > 140)
				return true;
		}
		return false;
	}

	public static boolean isLegal(List<? extends ResultOrder> route) {
		return false;
	}

	/**
	 * 修复错误的路径,路径有两种可能出错，1.超载，2.o2o 订单的顺序不对
	 * 
	 * @param route
	 *            要修复的路径信息
	 */
	public static void rectify(List<? extends ResultOrder> route) {
		rectifyOrder(route);
		rectifyWeight(route);
	}

	/**
	 * 修复o2o 路径的顺序
	 * 
	 * @param route
	 */
	private static void rectifyOrder(List<? extends ResultOrder> route) {
		for (int i = 0; i < route.size(); i++) {
			ResultOrder resultOrder = route.get(i);
			// 为电商订单，直接跳过
			if (resultOrder.Order_id.charAt(0) == 'F')
				continue;
			// 处理的是 o2o 订单
			for (int j = i + 1; j < route.size(); j++) {
				ResultOrder resultOrder2 = route.get(j);
				// 为 o2o 订单，并且顺序出错，收大于0，派小于0
				if (resultOrder.Order_id.equals(resultOrder2.Order_id)//订单编号相同时
						&& resultOrder.Amount < 0) {
					// 保存订单1的信息
					ResultOrder tmp = resultOrder.clone();
					// 将订单2赋值给订单1
					resultOrder2.Assignment(resultOrder);
					// 将订单1的信息赋值给订单2
					tmp.Assignment(resultOrder2);
					break;// 跳出循环
				}
			}
		}
	}

	/**
	 * 修复超重问题
	 * 
	 * @param route
	 */
	private static void rectifyWeight(List<? extends ResultOrder> route) {
		int weight = 0;
		// 先计算从站点里拿出来的订单重量
		for (ResultOrder resultOrder : route) {
			// 电商订单
			if (resultOrder.Order_id.charAt(0) == 'F') {
				weight += resultOrder.Amount;
			}
		}// 本来派件的重量为负
		weight = -weight;
		for (int i = 0; i < route.size(); i++) {
			ResultOrder resultOrder = route.get(i);
			// 路径合法，直接跳过
			if (weight + resultOrder.Amount <= Rule.MAX_WEIGHT) {
				weight += resultOrder.Amount;
				continue;
			}
			// 路径超重，进行处理,先将后面的派件移回来
			for (int j = i + 1; j < route.size(); j++) {
				ResultOrder resultOrder2 = route.get(j);
				if (resultOrder2.Order_id.charAt(0) == 'F') {
					// 保存订单1的信息
					ResultOrder tmp = resultOrder.clone();
					// 将订单2赋值给订单1
					resultOrder2.Assignment(resultOrder);
					// 将订单1的信息赋值给订单2
					tmp.Assignment(resultOrder2);
					// 将当前订单数量加上
					weight += resultOrder.Amount;
					break;// 跳出循环
				}
			}
		}
	}

	/**
	 * 用来查看等待时间 kamyang Sep 1, 2016
	 * 
	 * @param lists
	 * @return
	 */
	public static int calWaitingTime(List<ResultOrder> lists) {
		int time = 0;
		for (ResultOrder resultOrder : lists) {
			if (resultOrder.Amount > 0)
				time += (resultOrder.Departure - resultOrder.Arrival_time);
		}
		return time;
	}

	/**
	 * 计算路径适应值
	 * 
	 * @param lists
	 * 			  该出发的的所有订单
	 * @param depot
	 *            表示仓库点，做为环形路进行处理,该节点的时间会被修改，为出发点
	 * @param curTime
	 *            表示当前的时间
	 * @return 返回当前路径的适应值
	 */
	public static int calFitting(List<ResultOrder> lists, ResultOrder depot,
			int curTime) {
		int fitting = 0;
		int time = curTime;
		ResultOrder curResultRoute = depot;//出发点的信息
		for (ResultOrder nextResultRoute : lists) {
			// 计算该仓库送货的所有距离时间
			Node node = ServiceData.localPacageMaps.get(curResultRoute.Addr);
			Node node2 = ServiceData.localPacageMaps.get(nextResultRoute.Addr);
			time += distanceTime(node, node2);
			// 由于第一个是额外增加的仓库，故不用算
			curResultRoute = nextResultRoute;

			curResultRoute.Arrival_time = time;
			fitting += punishTime(curResultRoute);
			time = curResultRoute.Departure;
		}
		// 计算到回到仓库的时间
		Node node = ServiceData.localPacageMaps.get(curResultRoute.Addr);//最后一个送货结点
		Node node2 = ServiceData.localPacageMaps.get(depot.Addr);
		depot.Arrival_time = curResultRoute.Departure
				+ distanceTime(node, node2);
		depot.Departure = depot.Arrival_time;//下一躺的出发时间
		fitting += (time - curTime);// 计算这段时间内的适应值
		return fitting;
	}

	/**
	 * 先计算商户订单，在计算目的地订单 计算罚分的
	 * 用于计算 o2o 点的适应值
	 * 
	 * @param srcOrders
	 *            o2o 点的商户点
	 * @param destOrders
	 *            派送 o2o 点的目的点
	 * @return 返回处罚时间，但是不包括消耗的时间
	 */
	public static int calFitting(List<ResultOrder> srcOrders,
			List<ResultOrder> destOrders) {
		int fitting = 0;
		int time = 0;
		ResultOrder curResultOrder = null;
		for (ResultOrder nextResultOrder : srcOrders) {
			if (curResultOrder == null) {// 第一个节点，故可以获取开始时间，开始时间即收货时间
				curResultOrder = nextResultOrder;
				time = curResultOrder.Arrival_time;// 要在一开始时就设置收货时间
			} else {
				// 计算距离时间，调度中相邻的两个节点
				Node node = ServiceData.localPacageMaps
						.get(curResultOrder.Addr);
				Node node2 = ServiceData.localPacageMaps
						.get(nextResultOrder.Addr);
				time += distanceTime(node, node2);
				curResultOrder = nextResultOrder;//下一单
			}
			// 由于第一个是额外增加的仓库，故不用算
			curResultOrder.Arrival_time = time;
			fitting += punishTime(curResultOrder);//加上惩罚积分
			time = curResultOrder.Departure;
		}
		// 连接起前面的商户点来计算
		for (ResultOrder nextResultOrder : destOrders) {
			// 计算距离时间
			Node node = ServiceData.localPacageMaps.get(curResultOrder.Addr);
			Node node2 = ServiceData.localPacageMaps.get(nextResultOrder.Addr);
			time += distanceTime(node, node2);
			curResultOrder = nextResultOrder;
			curResultOrder.Arrival_time = time;
			fitting += punishTime(curResultOrder);
			time = curResultOrder.Departure;
		}
		return fitting;
	}

	/**
	 * 修改结果订单的离开时间，并且返回多余的处理时间加上处罚时间
	 * 
	 * @param resultOrder
	 *            要修改离开时间的结果订单
	 * @return
	 */
	public static int punishTime(ResultOrder resultOrder) {
		int punish = 0;
		int time = resultOrder.Arrival_time;// 到达时间
		Order order = ServiceData.OrderPackageMaps.get(resultOrder.Order_id);//获取订单
		String string = "";
		// 计算处理时间
		switch (resultOrder.Addr.charAt(0)) {
		case 'A':// 网点 按照测评增加的部分
			punish += Math.max(0, time - order.delivery_time) * 5;//结果调度的到达时间减去订单的最迟送达时间
			break;
		case 'B':// 目的地，要计算处理时间
			punish += Math.max(0, time - order.delivery_time) * 5;
			time += dealTime(resultOrder);//包裹处理时间
			break;
		case 'S':// O2O 商户
			punish += Math.max(0, time - order.pickup_time) * 5;
			time = Math.max(order.pickup_time, time);
			break;
		default:
			break;
		}
		resultOrder.Departure = time;// 结束时间
		return punish;
	}

	/**
	 * 
	 * 
	 * @param startEndOrder
	 *            当前节点
	 * @param newStartOrder
	 *            要获取的节点的起始时间
	 * @return 返回下一个节点的最优开始时间
	 */
	public static int getNextNodeTime(ResultOrder startEndOrder,
			ResultOrder newStartOrder) {
		Node node = ServiceData.localPacageMaps.get(startEndOrder.Addr);
		Node nod2 = ServiceData.localPacageMaps.get(newStartOrder.Addr);
		int startTime = startEndOrder.Departure;
		startTime += Rule.distanceTime(node, nod2);
		return startTime;
	}

	/**
	 * 计算两点之间花费的时间
	 * 
	 * @param node
	 * @param node2
	 * @return
	 */
	public static int distanceTime(Node node, Node node2) {
		if (node.name.equals(node2.name))
			return 0;
		double deltaLat = (node.Lat - node2.Lat) / 2;
		double deltaLng = (node.Lng - node2.Lng) / 2;
		double sinLat2 = Math.pow(Math.sin(Math.PI * deltaLat / 180), 2);
		double sinLng2 = Math.pow(Math.sin(Math.PI * deltaLng / 180), 2);
		double cosLat1 = Math.cos(Math.PI * node.Lat / 180);
		double cosLat2 = Math.cos(Math.PI * node2.Lat / 180);
		double distance = 2 * R
				* Math.asin(Math.sqrt(sinLat2 + cosLat1 * cosLat2 * sinLng2));
		double costTime = distance / 15 * 60; // 两点间路上花费的时间
		return (int) Math.round(costTime);
	}

	/**
	 * 包裹的处理时间
	 * 
	 * @param num
	 * @return
	 */
	public static int dealTime(ResultOrder resultOrder) {
		double costTime = 0;
		if (resultOrder.Amount < 0)
			costTime = 3 * Math.sqrt(-resultOrder.Amount) + 5;
		return (int) Math.round(costTime);
	}

	public static int dealTime(Order order) {
		double costTime = 0;
		costTime = 3 * Math.sqrt(order.num) + 5;
		return (int) Math.round(costTime);
	}
}
