package com.szu.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ServiceData;
import com.szu.model.SortNode;

public class CreateSortLib {
	private static final int MAX_WEIGHT = 140;

	private static Set<Node> getNode(List<Order> depotOrders,
			List<Order> o2oOrders) {
		Set<Node> set = new HashSet<>();
		Node node;
		node = ServiceData.localPacageMaps.get(depotOrders.get(0).src_id);//根据仓库地址获取仓库结点
		set.add(node);
		for (Order order : depotOrders) {
			node = ServiceData.localPacageMaps.get(order.dest_id);
			set.add(node);
		}//将送货地址的节点加入其中
		for (Order order : o2oOrders) {
			node = ServiceData.localPacageMaps.get(order.src_id);
			set.add(node);
			node = ServiceData.localPacageMaps.get(order.dest_id);
			set.add(node);
		}//将o2o的收发货地点加入其中
		return set;
	}

	/**
	 * 根据订单地点来创建排序库<br>
	 * kamyang Sep 1, 2016
	 * 
	 * @param orders
	 * @return
	 */
	public static Map<String, List<SortNode>> createSortLib(
			final List<Order> depotOrders, final List<Order> o2oOrders) {
		Map<String, List<SortNode>> libMap = new HashMap<>();
		Set<Node> set = getNode(depotOrders, o2oOrders);//获取所有结点
		for (Node node : set) {
			List<SortNode> sortNodeList = new ArrayList<>();
			for (Node node2 : set) {
				// 同一个对象，故大胆用 equals
				if (node.equals(node2))
					continue;
				SortNode sortNode = new SortNode();
				sortNode.dist = Rule.distanceTime(node, node2);//两点之间的距离
				sortNode.staticOderNameList.addAll(node2.staticOderNameList);//电商订单名称
				sortNode.o2oDeliveryOrderNameList
						.addAll(node2.o2oDeliveryOrderNameList);//o2o目的地址名称
				sortNode.o2oPickupOrderNameList
						.addAll(node2.o2oPickupOrderNameList);//o2o取货地址名称
				sortNodeList.add(sortNode);
			}
			// 排序
			Collections.sort(sortNodeList);
			libMap.put(node.name, sortNodeList);
		}
		return libMap;
	}

	/**
	 * 在这里获取排序库里面的序列，并且将传进去的那个 order 也包含进去<Br>
	 * 
	 * @param sortLib 最近20点排序映射
	 * @param initOrders 静态订单列表
	 * @param startOrder 列表中距离调度点最近的订单（结束点）
	 * @return 将静态订单中离startOrder最近的订单形成列表
	 */
	public static List<Order> getSortOrders(
			Map<String, List<SortNode>> sortLib, List<Order> initOrders,
			Order startOrder) {
		List<Order> resultOrders = new ArrayList<>();
		initOrders.remove(startOrder);// 将订单从初始堆里移除
		resultOrders.add(startOrder);// 将初始订单加入结果队列
		int weight = startOrder.num;// 初始化重量
		Order order;
		while ((order = getMinDistOrder(sortLib, initOrders, resultOrders,
				weight)) != null) {
			weight += order.num;
			initOrders.remove(order);
			resultOrders.add(order);//添加距离最短的订单
		}
		return resultOrders;
	}

	/**
	 * 判断 orderId 是否在 list 里面
	 * 
	 * @param list
	 * @param orderId
	 * @return 如果 orderId 在，则返回 list 里面的位置，如果不存在，则返回-1
	 */
	public static int containOrder(List<Order> list, String orderId) {
		int index = -1;
		Order order;
		for (int i = 0; i < list.size(); i++) {
			order = list.get(i);
			if (order.order_id.equals(orderId)) {
				index = i;
				break;
			}
		}
		return index;
	}

	/**
	 * 获取排序库里最小距离的那个节点
	 * 
	 * @param initOrders
	 *            初始订单，没有被分类
	 * @param resultOrders
	 *            被分类后的订单
	 * @param weight
	 *            当前的重量
	 * @return
	 */
	private static Order getMinDistOrder(Map<String, List<SortNode>> sortLib,
			List<Order> initOrders, List<Order> resultOrders, int weight) {
		Order resultOrder = null;
		int minDist = Integer.MAX_VALUE;
		for (Order order : resultOrders) {
			List<SortNode> sortNodes = sortLib.get(order.dest_id);//用目的地id来取
			for (SortNode sortNode : sortNodes) {
				if (sortNode.staticOderNameList.size() == 0)
					continue;// 没有货物要派送的
				if (minDist > sortNode.dist) {// 当前的最小距离比全局距离小
					for (String orderId : sortNode.staticOderNameList) {
						int index = containOrder(initOrders, orderId);
						// int index2 = containOrder(resultOrders, orderId);
						if (index == -1) {// 已经不存在于队列中，或者已经存在于寻找的堆里|| index2 ==
											// -1
							continue;
						}
						Order order2 = initOrders.get(index);// 获取初始堆里可能符合要求的订单
						if (weight + order2.num <= MAX_WEIGHT) {// 符合要求，可以进行更新全局距离和订单的位置
							minDist = sortNode.dist; // 更新数据
							resultOrder = order2;
							break;
						}
					}
				}
			}
		}
		return resultOrder;
	}
}
