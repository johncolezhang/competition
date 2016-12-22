package com.szu.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.szu.model.Node;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;
import com.szu.model.SortNode;

public class LocalSearch {
	private static int M_MIN_NODE = 20;// 每个点挂20个其他最近邻点

	/**
	 * 创建排序列表，key 表示一个订单的名称，List 表示该点到其他点的距离和信息
	 * 
	 * @param list
	 *            要创建排序库的列表
	 * @return 返回创建好的 map
	 */
	public static Map<String, List<SortNode>> createSortLib() {
		Map<String, List<SortNode>> libMap = new HashMap<>();
		Collection<Node> nodes = ServiceData.localPacageMaps.values();//所有地址的集合
		for (Node node : nodes) {
			List<SortNode> sortNodeList = new ArrayList<>();
			for (Node node2 : nodes) {
				// 同一个对象，故大胆用 equals
				if (node.equals(node2))
					continue;
				SortNode sortNode = new SortNode();
				sortNode.dist = Rule.distanceTime(node, node2);//两点花费的时间
				sortNode.staticOderNameList.addAll(node2.staticOderNameList);//将node2的所有静态订单加入
				sortNode.o2oDeliveryOrderNameList
						.addAll(node2.o2oDeliveryOrderNameList);//将node2的所有o2o配送订单送入
				sortNode.o2oPickupOrderNameList
						.addAll(node2.o2oPickupOrderNameList);//将node2的所有o2o取货订单送入
				sortNodeList.add(sortNode);//形成队列
			}
			SortNode[] sortNodes = MaxHeap.topK(sortNodeList, M_MIN_NODE);//距离最短的20个
			sortNodeList.clear();
			for (SortNode sortNode : sortNodes) {
				sortNodeList.add(sortNode);
			}
			Collections.sort(sortNodeList);//升序排列
			libMap.put(node.name, sortNodeList);
		}
		return libMap;

	}

	/**
	 * 只考虑静态点的优化
	 * 拿到该路径的调度列表以及取货点的调度
	 * @param resultOrders
	 * @param depotOrder
	 * @param curTime
	 */
	public static void localStaticSearch(List<ResultOrder> resultOrders,
			ResultOrder depotOrder) {
		int index = bestRemoveIndex(resultOrders, depotOrder);//时间最长的订单移除
		ResultOrder order = resultOrders.remove(index);//将要移除的调度传给order
		index = bestInsertIndex(resultOrders, depotOrder, order);
		resultOrders.add(index, order);
	}

	/**
	 * 将三点距离最大的中间结点返回
	 * @param resultOrders
	 * @param depotOrder
	 * @return index
	 */
	private static int bestRemoveIndex(List<ResultOrder> resultOrders,
			ResultOrder depotOrder) {
		int index = 0, size = resultOrders.size();
		int maxFitting = Integer.MIN_VALUE;
		ResultOrder preOrder, curOrder, nextOrder;
		preOrder = depotOrder;
		for (int i = 0; i < size - 1; i++) {
			curOrder = resultOrders.get(i);
			nextOrder = resultOrders.get(i + 1);
			Node node = ServiceData.localPacageMaps.get(preOrder.Addr);//发货点的地址
			Node node2 = ServiceData.localPacageMaps.get(nextOrder.Addr);
			int newDist = Rule.distanceTime(node, node2);//上一单结点和下一单结点之间的距离
			int oldDist = (curOrder.Arrival_time - preOrder.Departure)//当前到达时间减去前一调度的离开时间
					+ (nextOrder.Arrival_time - curOrder.Departure);//下一单到达时间减当前出发时间
			int tmp = oldDist - newDist;
			if (maxFitting < tmp) {
				maxFitting = tmp;
				index = i;//三点距离最大的
			}
			preOrder = curOrder;
		}
		curOrder = resultOrders.get(size - 1);//处理最后一个订单
		nextOrder = depotOrder;
		Node node = ServiceData.localPacageMaps.get(preOrder.Addr);
		Node node2 = ServiceData.localPacageMaps.get(nextOrder.Addr);
		int newDist = Rule.distanceTime(node, node2);
		int oldDist = (curOrder.Arrival_time - preOrder.Departure)
				+ (nextOrder.Arrival_time - curOrder.Departure);
		int tmp = oldDist - newDist;
		if (maxFitting < tmp) {
			maxFitting = tmp;
			index = size - 1;//设为当前订单的index
		}
		return index;
	}

	/**
	 * 找到该insertOrder订单最适合的顺序
	 * @param resultOrders
	 * @param depotOrder
	 * @param insertOrder
	 * @return
	 */
	private static int bestInsertIndex(List<ResultOrder> resultOrders,
			ResultOrder depotOrder, ResultOrder insertOrder) {
		int index = 0, size = resultOrders.size();
		int minFitting = Integer.MAX_VALUE;
		ResultOrder preOrder, curOrder, nextOrder;
		curOrder = insertOrder;
		preOrder = depotOrder;
		Node node2 = ServiceData.localPacageMaps.get(curOrder.Addr);
		for (int i = 0; i < size; i++) {
			nextOrder = resultOrders.get(i);
			Node node = ServiceData.localPacageMaps.get(preOrder.Addr);
			Node node3 = ServiceData.localPacageMaps.get(nextOrder.Addr);
			int newDist1 = Rule.distanceTime(node, node2);
			int newDist2 = Rule.distanceTime(node3, node2);
			int tmp = newDist1 + newDist2;//两点加node2的距离
			if (minFitting > tmp) {
				minFitting = tmp;
				index = i;
			}
			preOrder = nextOrder;
		}
		nextOrder = depotOrder;
		Node node = ServiceData.localPacageMaps.get(preOrder.Addr);
		Node node3 = ServiceData.localPacageMaps.get(nextOrder.Addr);
		int newDist = Rule.distanceTime(node, node2);
		int newDist1 = Rule.distanceTime(node3, node2);
		int tmp = newDist1 + newDist;//三点之间的最短路径
		if (minFitting < tmp) {
			minFitting = tmp;
			index = size;
		}
		return index;
	}

	/**
	 * 考虑动态点的局部优化
	 * 
	 * @param resultOrders
	 * @param depotOrder
	 * @param curTime
	 */
	public static void localSearch(List<ResultOrder> resultOrders,
			ResultOrder depotOrder, int curTime) {
		int index = bestRemoveIndex(resultOrders, depotOrder, curTime);
		ResultOrder order = resultOrders.remove(index);
		index = bestInsertIndex(resultOrders, depotOrder, order, curTime);
		resultOrders.add(index, order);
		Rule.rectify(resultOrders);// 进行修复
	}

	private static int bestRemoveIndex(List<ResultOrder> resultOrders,
			ResultOrder depotOrder, int curTime) {
		int index = 0, size = resultOrders.size();
		int maxFitting = Integer.MIN_VALUE;
		for (int i = 0; i < size; i++) {
			ResultOrder order = resultOrders.remove(i);
			int fitting = Rule.calFitting(resultOrders, depotOrder, curTime);
			if (maxFitting > fitting) {
				maxFitting = fitting;
				index = i;
			}
			resultOrders.add(i, order);
		}
		return index;
	}

	private static int bestInsertIndex(List<ResultOrder> resultOrders,
			ResultOrder depotOrder, ResultOrder insertOrder, int curTime) {
		int index = 0, size = resultOrders.size();
		int minFitting = Integer.MAX_VALUE;
		// 可以插入最后一个空位，即共有 size+1 个空位
		for (int i = 0; i <= size; i++) {
			resultOrders.add(i, insertOrder);
			int fitting = Rule.calFitting(resultOrders, depotOrder, curTime);
			if (fitting < minFitting) {
				minFitting = fitting;
				index = i;
			}
			resultOrders.remove(i);
		}
		return index;
	}

	// 会修改传进去的内容
	private static int additionTime(ResultOrder prioOrder,
			ResultOrder middOrder, ResultOrder tailOrder) {
		int addTime = 0;
		Node pioNode = ServiceData.localPacageMaps.get(prioOrder.Addr);
		Node middNode = ServiceData.localPacageMaps.get(middOrder.Addr);
		Node tailNode = ServiceData.localPacageMaps.get(tailOrder.Addr);
		int distance1 = Rule.distanceTime(pioNode, middNode);
		int distance2 = Rule.distanceTime(middNode, tailNode);
		// 局部计算消耗时间
		middOrder.Arrival_time = prioOrder.Departure + distance1;
		addTime += Rule.punishTime(middOrder);
		tailOrder.Arrival_time = middOrder.Departure + distance2;
		addTime += Rule.punishTime(tailOrder);
		// 加上总时间
		addTime += (tailOrder.Departure - prioOrder.Departure);
		return addTime;
	}
}
