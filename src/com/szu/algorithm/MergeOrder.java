package com.szu.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;
import com.szu.model.SortNode;
import com.szu.util.Rule;

public class MergeOrder {

	private static int TIME_RANGE = 10;
	private static int O2O_DEPOT_TIME = 90;
	private static int O2OSEP_TIME = 90;
	private static int MAX_WEIGHT = 140;
	private static int MERGE_TIME_RANGE = 60;
	public Map<String, Integer> o2oNumberMap = new HashMap<>();

	/**
	 * 合并所有的固定订单调度以及o2o订单调度
	 * @param depotLists
	 * @param o2oLists
	 * @return
	 */
	public List<List<ResultOrder>> mergeResultOrders(
			List<List<ResultOrder>> depotLists, List<List<ResultOrder>> o2oLists) {
		List<List<ResultOrder>> results = new ArrayList<>();
		ResultOrder depOrder = null;
		if (depotLists.size() != 0)
			depOrder = depotLists.get(0).get(0).clone();//获取仓库订单
		else if (o2oLists.size() != 0) {
			depOrder = o2oLists.get(0).get(0).clone();//获取o2o订单
		}
		// 任意一个序列大于0都要进一步合并,有可能会 o2o 第一
		while (depotLists.size() > 0 || o2oLists.size() > 0) {
			List<ResultOrder> resultOrders = new ArrayList<>();//该趟的总调度
			int startTime = 0;
			if (o2oLists.size() > 0) {
				resultOrders.addAll(o2oLists.get(0));
				Rule.calFitting(resultOrders, depOrder, 0);//修改结点时间
				o2oLists.remove(0);
			} else {
				// if (depotLists.size() > 0) {
				resultOrders.addAll(depotLists.get(0));//添加该仓库调度
				depotLists.remove(0);//删
				Rule.calFitting(resultOrders, depOrder, startTime);//修改结点时间
			} // else {
				// resultOrders.addAll(o2oLists.get(0));
				// startTime = resultOrders.get(0).Arrival_time;
				// o2oLists.remove(0);
				// Rule.calFitting(resultOrders, depOrder, startTime);
				// }
			while (true) {
				List<ResultOrder> list;
				// 优先合并o2o 的队列
				// int index = mergeO2oLists(resultOrders, o2oLists);
				// if (index != -1) {
				// list = o2oLists.remove(index);
				// resultOrders.addAll(list);
				// Rule.calFitting(resultOrders, depOrder, startTime);
				// } else {
				// index = mergeDepotLists(resultOrders, depotLists);
				// if (index != -1) {
				// list = depotLists.remove(index);
				// resultOrders.addAll(list);
				// Rule.calFitting(resultOrders, depOrder, startTime);
				// } else
				// break;
				// }
				int index = mergeDepotLists(resultOrders, depotLists);
				if (index != -1) {
					list = depotLists.remove(index);//时间720的删除，该趟时间超过
					resultOrders.addAll(list);
					Rule.calFitting(resultOrders, depOrder, startTime);//重新计算时间
				} else {
					break;
				}
			}
			results.add(resultOrders);
		}
		return results;//返回所有躺的调度
	}
	
	
	private int mergeDepotLists(List<ResultOrder> resultOrders,
			List<List<ResultOrder>> depotLists) {
		ResultOrder beginOrder, tailOrder = resultOrders.get(resultOrders
				.size() - 1);//最近加入的调度
		Node node, node2 = ServiceData.localPacageMaps.get(tailOrder.Addr);//该单的结束点
		int endTime = tailOrder.Departure;//插入单的结束时间，
		for (int i = 0; i < depotLists.size(); i++) {
			List<ResultOrder> list = depotLists.get(i);//遍历每一趟送货
			beginOrder = list.get(0);//列表第一单
			tailOrder = list.get(list.size() - 1);//列表最后一单
			node = ServiceData.localPacageMaps.get(beginOrder.Addr);//这一趟开始地址
			int time = endTime + Rule.distanceTime(node, node2)//这一趟起始地点到最近加入调度点结束的时间
					+ tailOrder.Departure;//这一趟的结束时间
			if (time <= 720)
				return i;//时间小于720的订单列表
		}
		return -1;
	}

	private int mergeO2oLists(List<ResultOrder> resultOrders,
			List<List<ResultOrder>> o2oLists) {
		ResultOrder tailOrder = resultOrders.get(resultOrders.size() - 1);
		ResultOrder beginOrder;
		int index = 0;
		int minTime = Integer.MAX_VALUE;
		Node node, node2 = ServiceData.localPacageMaps.get(tailOrder.Addr);
		for (int i = 0; i < o2oLists.size(); i++) {
			List<ResultOrder> list = o2oLists.get(i);
			beginOrder = list.get(0);
			node = ServiceData.localPacageMaps.get(beginOrder.Addr);
			int time = Rule.distanceTime(node, node2);
			time += tailOrder.Departure;// 距离加上最后处理时间
			int tmp = beginOrder.Arrival_time - time;
			if (tmp >= 0 && minTime > tmp) {
				minTime = tmp;
				index = i;
			}
		}
		if (minTime < MERGE_TIME_RANGE)
			return index;
		return -1;
	}

	public Map<String, List<List<ResultOrder>>> mergePrioO2oOrdersWithDepot(
			Map<String, List<List<ResultOrder>>> o2oListsMap) {
		Map<String, List<List<ResultOrder>>> results = new HashMap<>();
		List<ResultOrder> resultOrders = null;
		List<List<ResultOrder>> resultLists = new ArrayList<>();
		int i = 1;
		for (Entry<String, List<List<ResultOrder>>> entry : o2oListsMap
				.entrySet()) {
			String depotId = entry.getKey();
			System.out.println(i + "  begin prioMerge:" + depotId);
			i++;
			List<List<ResultOrder>> o2oLists = entry.getValue();
			while (o2oLists.size() > 0) {
				resultOrders = o2oLists.remove(0);
				ResultOrder beginO2oOrder = resultOrders.get(0);
				addO2oOrders(depotId, resultOrders, beginO2oOrder, 0);// 起始位置不添加
				resultLists.add(resultOrders);
			}
			results.put(depotId, resultLists);
			o2oNumberMap.put(depotId, 0);// 表明处理完成
		}
		return results;
	}

	/**
	 * 将 o2o 路径进行合并，只合并中间段
	 * 
	 * @param o2oLists
	 * @param depotOrders
	 * @return
	 */
	public List<List<ResultOrder>> mergeMiddleO2oOrdersWithDepot(
			List<List<ResultOrder>> o2oLists, String depotId) {
		List<List<ResultOrder>> results = new ArrayList<>();
		List<ResultOrder> resultOrders = null;
		List<Order> depotOrders = ServiceData.depotMaps.get(depotId);
		ResultOrder depotOrder = createResultOrder(depotOrders.get(0), true);
		Node node = ServiceData.localPacageMaps.get(depotOrder.Addr);
		while (o2oLists.size() > 0 && depotOrders.size() > 0) {
			resultOrders = o2oLists.remove(0);
			ResultOrder beginO2oOrder = null;
			// addO2oOrders(depotOrders, resultOrders, beginO2oOrder, 0);起始位置不添加
			for (int i = 0; i < o2oLists.size(); i++) {
				ResultOrder endOrder = resultOrders
						.get(resultOrders.size() - 1);
				Node node2 = ServiceData.localPacageMaps.get(endOrder.Addr);
				int startTime = endOrder.Departure;
				startTime += Rule.distanceTime(node, node2);
				List<ResultOrder> o2oList = o2oLists.get(i);
				beginO2oOrder = o2oList.get(0);
				if (startTime > beginO2oOrder.Arrival_time) {// 超时了
					continue;
				} else if (startTime + O2O_DEPOT_TIME < beginO2oOrder.Arrival_time) {// 进行合并
					o2oLists.remove(i);
					i--;
					addO2oOrders(depotId, o2oList, beginO2oOrder, startTime);
					resultOrders.addAll(o2oList);// 合并
					Rule.calFitting(resultOrders, depotOrder,
							resultOrders.get(0).Arrival_time);// 从第一个点到达时间算起，若为仓库，则默认为0了开始算起
				} else {// 直接合并，减少数量,先不让合并
					// o2oLists.remove(i);
					// i--;
					// addO2oOrders(depotOrders, o2oList, beginO2oOrder,
					// startTime);
					// resultOrders.addAll(o2oList);// 合并
					// Rule.calFitting(resultOrders, depotOrder, 0);// 从0开始算起
				}
			}
			results.add(resultOrders);
		}
		results.addAll(o2oLists);
		return results;
	}

	/**
	 * 
	 * kamyang 2016年8月5日
	 * 
	 * @param depotId
	 * @param o2oOrders
	 * @param beginO2oOrder
	 *            o2oOrder 第一个元素
	 * @param startTime
	 */
	private void addO2oOrders(String depotId, List<ResultOrder> o2oOrders,
			ResultOrder beginO2oOrder, final int startTime) {
		List<Order> depotOrders = ServiceData.depotMaps.get(depotId);
		if (depotOrders.size() == 0) {
			depotOrders = getDepotOrder(depotId);
		}
		if (depotOrders == null)
			System.out.println("NULL");
		List<Order> orders = arrayRouteInTime(depotOrders, beginO2oOrder,
				startTime);
		List<ResultOrder> prioOrders = evaluate(orders);
		ResultOrder endDepotOrder, depotOrder;
		// 一不够，还要继续添加，二、在范围内，可以合并，三，超了，要移除
		endDepotOrder = prioOrders.get(prioOrders.size() - 1);
		int time = endDepotOrder.Departure + startTime;// 该序列从0开始，故要加上起始时间
		Node node = ServiceData.localPacageMaps.get(endDepotOrder.Addr);
		Node node2 = ServiceData.localPacageMaps.get(beginO2oOrder.Addr);
		time += Rule.distanceTime(node, node2);// 加上距离
		if (time > beginO2oOrder.Arrival_time) {
			while (prioOrders.size() > 0 && time > beginO2oOrder.Arrival_time) {// 将大于部分移除
				// if (prioOrders.size() == 2)
				// break;// 如果只有两个则加进来（全部都不符合）
				ResultOrder resultOrder = prioOrders
						.remove(prioOrders.size() - 1);
				for (ResultOrder order : prioOrders) {
					if (resultOrder.Order_id.equals(order.Order_id)) {
						resultOrder = order;
						break;
					}
				}
				prioOrders.remove(resultOrder);
				if (prioOrders.size() == 0)
					break;
				resultOrder = prioOrders.get(prioOrders.size() - 1);
				time = resultOrder.Departure + startTime;
				node = ServiceData.localPacageMaps.get(resultOrder.Addr);
				time += Rule.distanceTime(node, node2);
			}
			o2oOrders.addAll(0, prioOrders);
			depotOrder = o2oOrders.get(0).clone();
			Rule.calFitting(o2oOrders, depotOrder, startTime);
			removeDepotOrders(depotOrders, prioOrders);
		} else if (time + O2O_DEPOT_TIME < beginO2oOrder.Arrival_time) {// 还差很多，要继续
			int disparity = beginO2oOrder.Arrival_time - time;
			beginO2oOrder = prioOrders.get(0);
			beginO2oOrder.Arrival_time = startTime + disparity;// 将第一个节点的时间往后移一下，与
																// o2o
																// 点连接，并将给序列的点当做
																// o2o 点
			o2oOrders.addAll(0, prioOrders);
			depotOrder = o2oOrders.get(0).clone();
			Rule.calFitting(o2oOrders, depotOrder, depotOrder.Arrival_time);// o2o
																			// 点的起始时间
			removeDepotOrders(depotOrders, prioOrders);
			addO2oOrders(depotId, o2oOrders, beginO2oOrder, startTime);// 减少仓库数据输出
		} else {// 在范围内，可以了
			o2oOrders.addAll(0, prioOrders);
			depotOrder = o2oOrders.get(0).clone();// 这个可以，因为没用连接，故直接从开始的位置开始
			Rule.calFitting(o2oOrders, depotOrder, startTime);
			removeDepotOrders(depotOrders, prioOrders);
		}
	}

	private List<Order> getDepotOrder(String depotId) {
		List<Order> orders = null;
		Order curO2oOrder = ServiceData.o2oOrderMaps.get(depotId).get(0);
		Node node = ServiceData.localPacageMaps.get(curO2oOrder.src_id);
		int minDist = Integer.MAX_VALUE;
		for (Entry<String, List<Order>> entry : ServiceData.depotMaps
				.entrySet()) {
			// 只有处理完o2o 点并且还有仓库点没用处理的情况后才可以进行
			if (o2oNumberMap.get(entry.getKey()) == 0
					&& entry.getValue().size() > 0) {
				Order depotOrder = entry.getValue().get(0);
				Node node2 = ServiceData.localPacageMaps.get(depotOrder.src_id);
				int tmp = Rule.distanceTime(node, node2);
				if (tmp < minDist) {
					minDist = tmp;
					orders = entry.getValue();
				}
			}
		}

		return orders;
	}

	private void removeDepotOrders(List<Order> depotOrders,
			List<ResultOrder> prioOrders) {
		for (int i = 0; i < prioOrders.size(); i++) {
			ResultOrder resultOrder = prioOrders.get(i);
			if (resultOrder.Amount < 0)// 对称性，处理一半即可
				break;
			int index = containOrder(depotOrders, resultOrder.Order_id);
			depotOrders.remove(index);
		}
	}

	/**
	 * 采用遗传算法进行计算 kamyang 2016年8月5日
	 * 
	 * @param orders
	 * @param startTime
	 * @return
	 */
	private List<ResultOrder> evaluate(List<Order> orders) {
		List<ResultOrder> resultOrders;// = new ArrayList<>();
		// if (orders.size() == 0)
		// return resultOrders;
		// if (orders.size() <= 20) {
		// resultOrders = new ArrayList<>();
		// BrandAndBoundForTSP tsp = new BrandAndBoundForTSP();
		// List<ResultOrder> resultOrders2 = new ArrayList<>();
		// ResultOrder resultOrder = createResultOrder(orders.get(0), true);
		// resultOrders2.add(resultOrder);
		// for (Order order : orders) {
		// resultOrder = createResultOrder(orders.get(0), false);
		// resultOrders2.add(resultOrder);
		// resultOrder = createResultOrder(orders.get(0), true);
		// resultOrders.add(resultOrder);
		// }
		// tsp.init(resultOrders2, new Distance<ResultOrder>() {
		// @Override
		// public double dist(ResultOrder resultOrder,
		// ResultOrder resultOrder2) {
		// Node node = ServiceData.localPacageMaps
		// .get(resultOrder.Addr);
		// Node node2 = ServiceData.localPacageMaps
		// .get(resultOrder2.Addr);
		// // 形成环状
		// int tmp = Rule.distanceTime(node, node2);
		// return tmp;
		// }
		// });
		// City city = tsp.getBest();
		// for (int index : city.route) {
		// index -= 1;
		// if (index > 0) {
		// resultOrders.add(resultOrders2.get(index));
		// }
		// }
		// Rule.calFitting(resultOrders, resultOrders2.get(0), 0);
		// } else {
		Vehicle vehicle = new Vehicle("", 100);
		vehicle.addOrder(orders, new ArrayList<Order>());
		vehicle.run();
		resultOrders = vehicle.getServicedLists();
		// }
		return resultOrders;
	}

	/**
	 * 获取离第一个 o2o 订单最近的不超过140重量的点
	 * 
	 * @param deOrders
	 *            仓库的订单
	 * @param endOrder
	 *            o2o 已经规划好的路径第一个节点
	 * @param startTime
	 *            规划路径开始时间
	 */
	private List<Order> arrayRouteInTime(List<Order> deOrders,
			final ResultOrder endOrder, final int startTime) {
		System.out.println("depotSize:" + deOrders.size() + " " + "startTime:"
				+ startTime);
		List<Order> results = new ArrayList<>();
		int time = endOrder.Arrival_time - startTime;// 总共时间
		List<SortNode> sortNodes = new ArrayList<>();
		Node node = ServiceData.localPacageMaps.get(endOrder.Addr);
		for (Order order : deOrders) {
			Node node2 = ServiceData.localPacageMaps.get(order.dest_id);
			SortNode sortNode = new SortNode();
			sortNode.dist = Rule.distanceTime(node, node2);
			sortNode.staticOderNameList.add(order.order_id);
			sortNodes.add(sortNode);
		}
		Collections.sort(sortNodes);
		int weight = 0;
		int dealingTime = 0;
		for (SortNode sortNode : sortNodes) {
			Order order = ServiceData.OrderPackageMaps
					.get(sortNode.staticOderNameList.get(0));
			int temp = Rule.dealTime(order);
			// 将总重量减少一点
			if (weight + order.num > MAX_WEIGHT
					|| dealingTime + temp + sortNode.dist > time)
				continue;
			weight += order.num;
			dealingTime += temp;
			results.add(order);
		}
		if (results.size() == 0)// 如果没用符合要求的点，先随便加一个进去
			results.add(deOrders.get(0));
		return results;
	}

	/**
	 * 按照仓库点来划分，处理的是 o2o 点,并进行初步合并
	 * 根据订单处理成调度列表
	 * @param list
	 * @return
	 */
	public List<List<ResultOrder>> dealingO2oOrder(List<Order> list) {
		List<List<ResultOrder>> results = new ArrayList<>();
		List<ResultOrder> resultOrders = null;
		List<ResultOrder> pickupResultOrders;
		List<ResultOrder> deliveryResultOrders;
		int weight;
		sort(list);// 按照时间进行排序
		while (list.size() > 0) {
			// 总是从第一个开始，处理完就将订单从列表中删除
			pickupResultOrders = new ArrayList<>();
			deliveryResultOrders = new ArrayList<>();
			resultOrders = new ArrayList<>();
			Order order = list.get(0);
			weight = order.num;
			ResultOrder srcResultOrder, destResultOrder;
			srcResultOrder = createResultOrder(order, true);//取货
			srcResultOrder.Arrival_time = order.pickup_time;//取货时间赋给到达时间
			destResultOrder = createResultOrder(order, false);
			pickupResultOrders.add(srcResultOrder);
			deliveryResultOrders.add(destResultOrder);
			int punish = Rule.calFitting(pickupResultOrders,
					deliveryResultOrders);
			list.remove(0);
			if (punish > 0) {// 出现了处罚情况
				resultOrders.addAll(pickupResultOrders);
				resultOrders.addAll(deliveryResultOrders);
				results.add(resultOrders);
				continue;
			}
			while (true) {
				int index = minO2oOrderIndex(list,
						pickupResultOrders.get(pickupResultOrders.size() - 1),//刚刚加入的取货调度，订单列表中的第一单
						weight);
				if (index == -1)// 没有可以加入的节点了（因为要按照时间排序过来）
					break;
				order = list.get(index);
				srcResultOrder = createResultOrder(order, true);
				destResultOrder = createResultOrder(order, false);
				pickupResultOrders.add(srcResultOrder);
				insertDeliveryOrder(deliveryResultOrders, destResultOrder);
				punish = Rule.calFitting(pickupResultOrders,
						deliveryResultOrders);
				if (punish > 0) {// 派送超时
					// System.out.println("超时");
					pickupResultOrders.remove(srcResultOrder);
					deliveryResultOrders.remove(destResultOrder);
					break;
				} else {// 符合情况
					weight += order.num;
					list.remove(index);
				}
			}
			resultOrders.addAll(pickupResultOrders);
			resultOrders.addAll(deliveryResultOrders);
			results.add(resultOrders);
		}
		return results;
	}

	/**
	 * list订单中， 返回距离resultOrder后时间最近的一单的index
	 * @param list o2o订单
	 * @param resultOrder 该订单的取货调度，订单列表中的第一单的调度
	 * @param weight 该订单的货物个数
	 */
	private int minO2oOrderIndex(List<Order> list, ResultOrder resultOrder,
			int weight) {
		int index = 0;
		int minDist = Integer.MAX_VALUE;
		Node node = ServiceData.localPacageMaps.get(resultOrder.Addr);//出发地点
		// 进行遍历，寻找最小的那个
		for (int i = 0; i < list.size(); i++) {
			Order order = list.get(i);
			if (weight + order.num > MAX_WEIGHT)// 超重
				continue;
			Node node2 = ServiceData.localPacageMaps.get(order.src_id);//该o2o订单的出发及节点
			int dist = Rule.distanceTime(node, node2);//当前节点与第一单节点的距离
			int tmp = order.pickup_time - dist - resultOrder.Departure;//该最迟取货时间-第一单离开时间-距离
			if (tmp >= 0 && minDist > tmp) {
				minDist = tmp;
				index = i;
			}
		}
		if (minDist < TIME_RANGE)
			return index;
		return -1;
	}

	/**
	 * 之前的方法
	 * 
	 * @param list
	 * @return
	 */
	private List<List<ResultOrder>> o2oOrder(List<Order> list) {
		List<List<ResultOrder>> results = new ArrayList<>();
		List<ResultOrder> resultOrders = null;
		List<ResultOrder> pickupResultOrders;
		List<ResultOrder> deliveryResultOrders;
		int weight = 0;
		ResultOrder srcResultOrder;
		ResultOrder destResultOrder;
		Node node, node2;
		sort(list);
		while (list.size() > 0) {
			Order order = list.get(0);
			weight += order.num;
			node = ServiceData.localPacageMaps.get(order.src_id);
			node2 = ServiceData.localPacageMaps.get(order.dest_id);
			srcResultOrder = createResultOrder(order, true);
			destResultOrder = createResultOrder(order, false);
			srcResultOrder.Arrival_time = order.pickup_time;// 初始化第一个节点的开始时间
			resultOrders = new ArrayList<>();
			pickupResultOrders = new ArrayList<>();
			deliveryResultOrders = new ArrayList<>();
			pickupResultOrders.add(srcResultOrder);
			deliveryResultOrders.add(destResultOrder);
			int punish = Rule.calFitting(pickupResultOrders,
					deliveryResultOrders);
			list.remove(0);
			// 从拣货到派件的距离间就超时了
			if (punish != 0) {
				resultOrders.addAll(pickupResultOrders);
				resultOrders.addAll(deliveryResultOrders);
				results.add(resultOrders);
				continue;
			}
			while (true) {
				ResultOrder resultOrder = pickupResultOrders
						.get(pickupResultOrders.size() - 1);
				List<SortNode> sortNodes = ServiceData.sortLibMap
						.get(resultOrder.Addr);
				for (SortNode sortNode : sortNodes) {
					List<String> removeOrderIds = new ArrayList<>();
					if (sortNode.o2oPickupOrderNameList.size() == 0)
						continue;// 没有 o2o 商户点
					for (String orderString : sortNode.o2oPickupOrderNameList) {
						int index = containOrder(list, orderString);
						if (index == -1)// 订单已经被处理（获取第一个的时候，没有在 map 里移除）
							continue;
						order = ServiceData.OrderPackageMaps.get(orderString);
						// 超重
						if (weight + order.num > MAX_WEIGHT)
							continue;
						// 将这个时间窗口的当做
						if (resultOrder.Departure <= order.pickup_time
								&& order.pickup_time <= (resultOrder.Departure + TIME_RANGE)) {
							srcResultOrder = createResultOrder(order, true);
							destResultOrder = createResultOrder(order, false);
							pickupResultOrders.add(srcResultOrder);
							// 插入到派送队列
							insertDeliveryOrder(deliveryResultOrders,
									destResultOrder);
							punish = Rule.calFitting(pickupResultOrders,
									deliveryResultOrders);
							if (punish != 0) {// 出现超时的情况
								pickupResultOrders.remove(srcResultOrder);
								deliveryResultOrders.remove(destResultOrder);
							} else {// 满足条件，可以插入，将列表移除
								list.remove(index);
								removeOrderIds.add(orderString);
							}
						}
					}
					// 将添加到路径上的订单号都移除
					sortNode.o2oPickupOrderNameList.removeAll(removeOrderIds);
				}
				if (pickupResultOrders.get(pickupResultOrders.size() - 1)
						.equals(resultOrder)) {
					// 没有元素增加，退出循环
					resultOrders.addAll(pickupResultOrders);
					resultOrders.addAll(deliveryResultOrders);
					results.add(resultOrders);
					break;
				}
			}
		}
		return results;
	}

	public List<List<ResultOrder>> dealDepot(List<Order> list) {
		List<List<ResultOrder>> results = new ArrayList<>();
		List<ResultOrder> resultOrders;
		List<String> orderIDs = new ArrayList<>();
		ArrayList<ResultOrder> pickupResultOrders;
		ArrayList<ResultOrder> deliveryResultOrders;
		int weight = 0;
		if (list.size() == 0)
			return results;
		ResultOrder depotOrder = createResultOrder(list.get(0), true);// 获取第一个仓库
		while (list.size() > 0) {
			// 只要不超过140即可
			resultOrders = new ArrayList<>();
			pickupResultOrders = new ArrayList<>();
			deliveryResultOrders = new ArrayList<>();
			// 将第一个点加到路径上
			Order order = list.get(0);
			weight = order.num;
			ResultOrder srcResultOrder = createResultOrder(order, true);
			ResultOrder destResultOrder = createResultOrder(order, false);
			pickupResultOrders.add(srcResultOrder);
			insertDeliveryOrder(deliveryResultOrders, destResultOrder);
			list.remove(0);
			String destId;
			while ((destId = getMinDistOrder(list, deliveryResultOrders, weight)) != null) {
				// 可以进行处理
				// System.out.println(destId);
				List<SortNode> sortNodes = ServiceData.sortLibMap.get(destId);
				orderIDs.clear();
				for (SortNode sortNode : sortNodes) {
					if (sortNode.staticOderNameList.size() == 0)
						continue;
					for (String string : sortNode.staticOderNameList) {
						order = ServiceData.OrderPackageMaps.get(string);
						if (order.num + weight <= MAX_WEIGHT) {
							weight += order.num;
							if (weight > 140)
								System.out.println(string + "  " + weight);
							srcResultOrder = createResultOrder(order, true);
							destResultOrder = createResultOrder(order, false);
							pickupResultOrders.add(srcResultOrder);
							insertDeliveryOrder(deliveryResultOrders,
									destResultOrder);
							// System.out.println(deliveryResultOrders.size());
							orderIDs.add(string);
						}
					}
					if (orderIDs.size() > 0) {
						sortNode.staticOderNameList.removeAll(orderIDs);
						for (String string : orderIDs) {
							int index = containOrder(list, string);
							list.remove(index);
						}
						// 后面的不用再进一步处理
						break;
					}
				}
			}
			// System.out.println("end a classify:" + list.size());
			resultOrders.addAll(pickupResultOrders);
			resultOrders.addAll(deliveryResultOrders);
			Rule.calFitting(resultOrders, depotOrder, 0);
			results.add(resultOrders);
		}
		return results;
	}

	/**
	 * 获取最近邻的目的地 ID
	 * 
	 * @param list
	 * @param resultOrders
	 * @param weight
	 * @return
	 */
	private String getMinDistOrder(List<Order> list,
			List<ResultOrder> resultOrders, int weight) {
		List<String> orderIds = new ArrayList<>();
		String string = null;
		// if (resultOrders.size() == 0) {// 还没有元素加进来
		// for (Order order : list) {// 存在即可
		// List<SortNode> sortNodes = ServiceData.sortLibMap
		// .get(order.dest_id);
		// for (SortNode sortNode : sortNodes) {
		// if (sortNode.staticOderNameList.size() > 0) {
		// orderIds.clear();
		// for (String orderId : sortNode.staticOderNameList) {
		// int index = containOrder(list, orderId);
		// if (index != -1) {// 已经不存在于队列中，故要移除
		// string = order.dest_id;
		// break;
		// } else {
		// orderIds.add(orderId);
		// }
		// }
		// sortNode.staticOderNameList.removeAll(orderIds);
		// if (string != null) {
		// return string;
		// }
		// }
		// }
		// }
		// }

		int minDist = Integer.MAX_VALUE;
		for (ResultOrder resultOrder : resultOrders) {
			List<SortNode> sortNodes = ServiceData.sortLibMap
					.get(resultOrder.Addr);
			for (SortNode sortNode : sortNodes) {
				if (sortNode.staticOderNameList.size() == 0)
					continue;// 没有货物要派送的
				if (minDist > sortNode.dist) {// 当前的最小距离比全局距离小
					orderIds.clear();
					for (String orderId : sortNode.staticOderNameList) {
						int index = containOrder(list, orderId);
						if (index == -1) {// 已经不存在于队列中，故要移除
							orderIds.add(orderId);
							continue;
						}
						Order order = ServiceData.OrderPackageMaps.get(orderId);
						if (weight + order.num <= MAX_WEIGHT) {// 符合要求，可以进行更新全局距离和订单的位置
							// System.out.println(weight + order.num +
							// "orderId "
							// + orderId);
							string = resultOrder.Addr;
							minDist = sortNode.dist;
							break;
						}
					}
					// 将重复的部分移除
					sortNode.staticOderNameList.removeAll(orderIds);
				}
			}
		}
		return string;
	}

	/**
	 * 获取列表里订单的位置
	 * 
	 * @param list
	 * @param orderId
	 * @return
	 */
	private int containOrder(List<Order> list, String orderId) {//获取订单位置
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
	 * 将派送订单插入到路径里
	 * 
	 * @param deliveryOrders
	 * @param insertOrder
	 */
	private void insertDeliveryOrder(List<ResultOrder> deliveryOrders,
			ResultOrder insertOrder) {
		// 只有一个
		if (deliveryOrders.size() < 2) {
			deliveryOrders.add(insertOrder);
			return;
		}
		int index = 0;
		ResultOrder nextOrder;
		nextOrder = deliveryOrders.get(0);
		Node node;
		Node node2 = ServiceData.localPacageMaps.get(insertOrder.Addr);
		Node node3 = ServiceData.localPacageMaps.get(nextOrder.Addr);
		int minDist = Rule.distanceTime(node3, node2);
		for (int i = 1; i < deliveryOrders.size(); i++) {
			node = node3;
			nextOrder = deliveryOrders.get(i);
			node3 = ServiceData.localPacageMaps.get(nextOrder.Addr);
			int tmp = Rule.distanceTime(node, node2)
					+ Rule.distanceTime(node2, node3);
			if (tmp < minDist) {
				minDist = tmp;
				index = i;
			}
		}
		node3 = ServiceData.localPacageMaps.get(nextOrder.Addr);
		int tmp = Rule.distanceTime(node3, node2);
		if (tmp < minDist) {
			minDist = tmp;
			index = deliveryOrders.size();
		}
		deliveryOrders.add(index, insertOrder);
	}

	private void sort(List<Order> list) {
		Collections.sort(list, new Comparator<Order>() {
			// 接收时间从小到大排序
			@Override
			public int compare(Order o1, Order o2) {
				if (o1.pickup_time < o2.pickup_time)
					return -1;
				if (o1.pickup_time > o2.pickup_time)
					return 1;
				return 0;
			}
		});
	}

	private ResultOrder createResultOrder(Order order, boolean isSrc) {
		ResultOrder resultOrder = new ResultOrder();
		resultOrder.Courier_id = "";
		if (isSrc) {
			resultOrder.Addr = order.src_id;
			resultOrder.Amount = order.num;
		} else {
			resultOrder.Addr = order.dest_id;
			resultOrder.Amount = -order.num;
		}
		resultOrder.Order_id = order.order_id;
		return resultOrder;
	}

}
