package com.szu.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.szu.model.ChunkOrder;
import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ServiceData;

public class Classify {
	private static final int RANGE = 30;
	private static final int step = 20; // 以20分钟为一个尺度，进行划分

	/**
	 * 将o2o 的数据划分到仓库里，按照最近原则，起始点离该仓库最近，但没有考虑时间的问题
	 * 
	 * @param o2oOrderMaps
	 *            要划分的o2o 仓库
	 */
	public static void attach2depot(Map<String, List<Order>> o2oOrderMaps) {
		// 初始化 o2o 订单
		for (String string : ServiceData.depotMaps.keySet()) {
			List<Order> list = new ArrayList<>();
			o2oOrderMaps.put(string, list);
		}
		// 将 o2o 订单分配到相关的站点里
		for (Order order : ServiceData.o2oOrders) {
			Node node = ServiceData.localPacageMaps.get(order.src_id);//o2o起始地点
			int distance = Integer.MAX_VALUE;
			String minDepot = null;
			for (String string : ServiceData.depotMaps.keySet()) {
				Node node2 = ServiceData.localPacageMaps.get(string);
				int tmp = Rule.distanceTime(node, node2);
				if (distance > tmp) {
					distance = tmp;
					minDepot = string;
				}
			}
			o2oOrderMaps.get(minDepot).add(order);//划分o2o订单
		}
	}

	/**
	 * 按照时间进行划分 o2o 订单
	 * 返回静态订单目的地周围的o2o订单，用map来映射
	 * 
	 * @param depotList
	 *            该仓库点，商户点
	 * @param o2oOrders
	 *            为对应仓库点的o2o 订单，商户点的o2o订单
	 * @return 返回定点与o2o 的map,其中键为定点名称，值为 o2o 的订单
	 */
	public static Map<String, List<Order>> classifyO2oByTime(
			List<Order> depotList, List<Order> o2oOrders) {
		// System.out.println(depotList.size() + "  " + o2oOrders.size());
		int maxTime = Integer.MIN_VALUE;//最晚的o2o
		int minTime = Integer.MAX_VALUE;//最早的o2o
		List<Order> depotOrders = new ArrayList<>();
		for (Order order : depotList) {
			depotOrders.add(order);
		}
		// 获取o2o 点的时间分布
		for (Order order : o2oOrders) {
			maxTime = Math.max(maxTime, order.pickup_time);
			minTime = Math.min(minTime, order.pickup_time);
		}
		Map<Integer, List<Order>> o2oclassMap = new HashMap<>();//按时间段分的o2o，每段20分钟
		for (Order order : o2oOrders) {
			int tmp = (order.pickup_time - minTime) / step;//暗转step时间进行划分
			if (o2oclassMap.containsKey(tmp)) {//已存在该key值的key-value对，将订单加入到该对中
				o2oclassMap.get(tmp).add(order);
			} else {//不存在则新建key-value对
				List<Order> list = new ArrayList<>();
				list.add(order);
				o2oclassMap.put(tmp, list);//key值为时间划分组
			}
		}
		Map<String, List<Order>> o2oToDepotclassMap = new HashMap<>();
		// System.out.println(o2oclassMap.size());
		// 按时间的分隔，进行划分o2o 到目的点的距离的分类
		for (Entry<Integer, List<Order>> entity : o2oclassMap.entrySet()) {//已经按时间段分好
			// 每个 list 都是时间相近的点
			List<Order> list = null;
			while (entity.getValue().size() > 0) {
				list = new ArrayList<>();//记录静态点周围的o2o订单
				Order centerOrder = null;//距离下一个o2o最近的静态点
				Order o2oOrder = entity.getValue().get(0);//该时间段的第一个订单
				Node node = ServiceData.localPacageMaps.get(o2oOrder.src_id);//获取o2o出发点信息
				// 将 o2o 点依附到仓库的点上
				int minDistance = Integer.MAX_VALUE;
				for (Order depOrder : depotOrders) {
					Node node2 = ServiceData.localPacageMaps
							.get(depOrder.dest_id);//静态到达点信息
					int tmp = Rule.distanceTime(node, node2);//o2o出发点到静态到达点的距离
					if (minDistance > tmp) {
						minDistance = tmp;
						centerOrder = depOrder;//该时间段该订单时间最短，则将该订单设为centerorder
					}
				}
				// 将中心点移除
				depotOrders.remove(centerOrder);
				node = ServiceData.localPacageMaps.get(centerOrder.dest_id);//获取静态达到点信息
				for (Order order : entity.getValue()) {//遍历所有实体中的订单
					Node node2 = ServiceData.localPacageMaps.get(order.src_id);//获取o2o的出发点
					if (Rule.distanceTime(node, node2) <= Classify.RANGE) {//不超过30分钟
						list.add(order);
					}
				}
				entity.getValue().removeAll(list);
				// System.out.println(minOrder == null);
				o2oToDepotclassMap.put(centerOrder.dest_id, list);//静态目的地周围的o2o订单
			}
		}
		return o2oToDepotclassMap;
	}

	/**随机选取一单，并且将其他单按照距离排序好
	 * 
	 * 每个list重量不超过140且总时间不超过540分钟
	 * 
	 * 将仓库点进行划分成小于140的块
	 * 
	 * 该仓库结点的送达点之间距离的排序放在一个lists中返回
	 * 
	 * @param depotList
	 *            该仓库所有订单
	 * @param o2oclassToDepotMap
	 *            动态的与仓库点的关系（还没有加入）
	 *            仓库目的地周围的o2o映射
	 * @return 返回排好序的lists
	 */
	public static List<List<Order>> classifyDepot(List<Order> depotList,
			Map<String, List<Order>> o2oclassToDepotMap) {
		int time = 0;//记录order进行了多久,同时限制list总时长不超过540
		List<Order> depotOrders = new ArrayList<>();
		for (Order order : depotList) {
			depotOrders.add(order);//加入所有订单
		}
		int randomIndex = (int) (Math.random() * depotOrders.size());//随机索引
		List<List<Order>> lists = new ArrayList<>();
		List<Order> list = new ArrayList<>();
		int carry = 0;//记录货物的总重
		Order centerOrder = depotOrders.get(randomIndex);//随机获取一个订单
		depotOrders.remove(randomIndex);
		list.add(centerOrder);//加入中心订单
		time = Rule.distanceTime(ServiceData.localPacageMaps.get(centerOrder.src_id),
				ServiceData.localPacageMaps.get(centerOrder.dest_id));//出发点到目的点的距离
		carry += centerOrder.num;//累积货物个数
		while (depotOrders.size() > 0) {
			Order order = min2CenterOrder(depotOrders, centerOrder);//返回距离中心订单最近的订单
			time = orderTime(list.get(list.size()-1), time, order);//当前list所花时间
			if (carry + order.num > 140 || time > 540) {
				System.out.println("carry:" + carry);
				carry = order.num;
				centerOrder = order; // 更改中心点
				System.out.println("每个list所花时间：" + time + ",list长度：" + list.size() + "(在classify)");
				time = 0;
				lists.add(list);//重量大于140，不能添加订单了，将订单列表加入lists中
				list = new ArrayList<>();
				list.add(order);
			} else {
				carry += order.num;
				list.add(order);
			}
			depotOrders.remove(order);
		}
		lists.add(list);
		return lists;
	}
	
	/**
	 * 
	* @Title: orderTime 
	* @Description: TODO计算当前该list会花多少时间
	* @param @param list
	* @param @param order 
	* @return void
	* @throws 
	* @author johncole
	* @date 2017年2月27日
	 */
	private static int orderTime(Order preOrder, int time, Order order) {
		int orderTime = Rule.distanceTime(ServiceData.localPacageMaps.get(order.src_id),
				ServiceData.localPacageMaps.get(order.dest_id));//订单的时长
		int dist = Rule.distanceTime(ServiceData.localPacageMaps.get(preOrder.dest_id),
				ServiceData.localPacageMaps.get(order.src_id));//前一单到这一单的距离
		return time + orderTime + dist;
	}
	
	/**
	 * lists已经按照距离排好序，且每个list重量不大于140
	 * o2o是静态订单目的地的映射，且按时间排好
	 * 将o2o订单和网点订单存在ChunkOrder列表中
	 * lists中的list地点与chunkorder的地点有对应关系
	 * chunkorders装着每个配送点的所有静态和所有达到点的所有o2o
	 */
	public static List<ChunkOrder> divideDepotbyO2oOrders(
			List<List<Order>> lists, Map<String, List<Order>> o2oclassToDepotMap) {
		List<ChunkOrder> chunkOrders = new ArrayList<>();//包括了网点订单和o2o订单
		for (List<Order> list : lists) {
			ChunkOrder chunkOrder = new ChunkOrder();
			chunkOrder.depotOrders.addAll(list);//加静态
			int startTime = Integer.MAX_VALUE;
			for (Order order : list) {
				if (o2oclassToDepotMap.containsKey(order.dest_id)) {
					List<Order> o2oOrders = o2oclassToDepotMap
							.get(order.dest_id);
					chunkOrder.o2oOrders.addAll(o2oOrders);//加目的点的o2o
					for (Order order2 : o2oOrders) {
						startTime = Math.min(startTime, order2.pickup_time);//求最早起始时间
					}
				}
			}//******这里没有考虑o2o的时长，只是执行完一个静态后将到达点的o2o送完
			if (startTime < Integer.MAX_VALUE) {
				chunkOrder.startTime = startTime;
			}
			//输出每个chunkorder起始时间
			//System.out.println("chunkorder的o2o起始时间： " + chunkOrder.startTime + "(在classify里)");
			
			chunkOrders.add(chunkOrder);
		}
		Collections.sort(chunkOrders, new Comparator<ChunkOrder>() {
			@Override
			public int compare(ChunkOrder o1, ChunkOrder o2) {
				int d1 = o1.startTime;
				int d2 = o2.startTime;
				if (d1 < d2)
					return -1;
				if (d1 > d2)
					return 1;
				return 0;
			}
		});//按起始升序排序chunkorders
		return chunkOrders;
	}

	/**
	 * 将订单放到地点上
	 */
	public static void attachOrderToNode() {
		/**
		 * 静态地点，只放到目的点上
		 */
		for (List<Order> list : ServiceData.depotMaps.values()) {
			for (Order order : list) {
				Node node = ServiceData.localPacageMaps.get(order.dest_id);
				node.staticOderNameList.add(order.order_id);
			}
		}
		// 动态点，放在目的点和原点上
		for (Order order : ServiceData.o2oOrders) {
			Node node = ServiceData.localPacageMaps.get(order.src_id);
			node.o2oPickupOrderNameList.add(order.order_id);
			node = ServiceData.localPacageMaps.get(order.dest_id);
			node.o2oDeliveryOrderNameList.add(order.order_id);
		}
	}
	
	/**
	 * 返回距离中心订单最近的订单
	 */
	private static Order min2CenterOrder(List<Order> depotOrders, Order center) {
		Order minOrder = null;
		int minDist = Integer.MAX_VALUE;
		Node node = ServiceData.localPacageMaps.get(center.dest_id);//中心订单的配送点
		for (Order order : depotOrders) {
			Node node2 = ServiceData.localPacageMaps.get(order.dest_id);//仓库订单的配送点
			int tmp = Rule.distanceTime(node, node2);
			if (minDist > tmp) {
				minDist = tmp;
				minOrder = order;
			}
		}
		return minOrder;//返回距离中心订单最近的订单
	}
}
