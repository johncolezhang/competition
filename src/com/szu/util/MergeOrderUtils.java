package com.szu.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;
import com.szu.model.SortNode;

/**
 * 合并订单，包括动态点订单和静态点订单
 * 有的在o2o之前插静态，有的在o2o段之间插静态
 * 
 */
public class MergeOrderUtils {
	public static int calTime = 0;
	public static int totalPrio = 0;

	public static List<List<ResultOrder>> mergeResultList(
			List<List<ResultOrder>> depotLists, List<List<ResultOrder>> o2oLists) {
		List<List<ResultOrder>> resultLists = new ArrayList<>();

		return resultLists;
	}

	/**
	 * 在o2o之前插部分静态订单
	 * @param sortLib 排序表
	 * @param o2oLists 该仓库附近的o2o
	 * @param depotId 仓库id
	 * @return
	 */
	public static List<List<ResultOrder>> mergeO2oOrder(
			Map<String, List<SortNode>> sortLib,
			List<List<ResultOrder>> o2oLists, String depotId) {
		List<List<ResultOrder>> resultLists = new ArrayList<>();
		List<Order> depotLists = ServiceData.depotMaps.get(depotId);//该仓库订单
		while (o2oLists.size() > 0) {
			List<ResultOrder> o2oOrders = o2oLists.remove(0);//获取一段
			ResultOrder newStartOrder = o2oOrders.get(0).clone();//克隆o2o第一单
			List<ResultOrder> resultOrders = addOneMorePrioDepot(sortLib,
					depotLists, newStartOrder);
			resultOrders.addAll(o2oOrders);// 将 o2o 订单加上去
			Rule.calFitting(resultOrders, resultOrders.get(0).clone(), 0);// 从新计算
			resultLists.add(resultOrders);
		}
		return resultLists;
	}

	/**
	 * 插静态，newstartorder近，已经用tsp排序过
	 * @param sortLib
	 * @param depotLists 静态订单
	 * @param newStartOrder 该段o2o的第一单
	 * @return 
	 */
	private static List<ResultOrder> addOneMorePrioDepot(
			Map<String, List<SortNode>> sortLib, List<Order> depotLists,
			ResultOrder newStartOrder) {
		List<ResultOrder> resultList = new ArrayList<>();
		List<ResultOrder> resultOrders = new ArrayList<>();
		List<Order> orders = null;
		ResultOrder preNewStartResultOrder = null;// 用于保存上一个片段起始位置
		while (0 < newStartOrder.Arrival_time) {
			// 可能还可以增加，如果不符合，则抛弃并将这些电商包裹归还
			resultList.addAll(0, resultOrders);// 符合的就加进来，第一个为空，作为缓冲
			orders = getAPartDepot(sortLib, depotLists, newStartOrder);//获取距离newstartorder近的静态订单
			resultOrders = Utils.tsp(orders);//根据tsp生成订单调度
			ResultOrder depotOrder = resultOrders.get(0).clone();//获取第一单
			ResultOrder endOrder = (ResultOrder) getLast(resultOrders);//获取最后一单
			// 这一段路径需要花费的时间
			int spendTime = endOrder.Departure
					+ Rule.getNextNodeTime(endOrder, newStartOrder);//静态最后一单出发时间加最后一单到新一段o2o的时间
			int startTime = newStartOrder.Arrival_time - spendTime;
			Rule.calFitting(resultOrders, depotOrder, startTime);
			preNewStartResultOrder = newStartOrder;
			newStartOrder = resultOrders.get(0).clone();
		}//开始时添加resultlist，保证arrivaltime小于0的时候
		// 将不符合的部分进行剪裁
		if (orders != null) {
			resultOrders = cutPrioDepotOrder(orders, preNewStartResultOrder);
			resultList.addAll(0, resultOrders);
		}
		return resultList;
	}

	/**
	 * 此时传进去的 newStartOrder 起始时间小于0
	 * 
	 * @param orders
	 * @param newStartOrder
	 * @return
	 */
	private static List<ResultOrder> cutPrioDepotOrder(List<Order> orders,
			ResultOrder newStartOrder) {
		List<ResultOrder> resultOrders = Utils.tsp(orders);//分支界限处理，resultorder已排序好，没考虑时间
		ResultOrder endOrder = (ResultOrder) getLast(resultOrders);//获取最后一单
		int newStartTime = Rule.getNextNodeTime(endOrder, newStartOrder);
		while (newStartTime > newStartOrder.Arrival_time) {// 超时了，要进行截取
			Order order = orders.remove(orders.size() - 1);//减少一单
			returnDepot(order);// 将其加到合适的仓库里
			if (orders.size() == 0) {// 加不进去，退出
				resultOrders = new ArrayList<>();
				break;
			}
			resultOrders = Utils.tsp(orders);
			endOrder = (ResultOrder) getLast(resultOrders);
			newStartTime = Rule.getNextNodeTime(endOrder, newStartOrder);
		}
		return resultOrders;
	}

	public static void returnDepot(Order order) {
		calTime++;
		List<Order> depotList = ServiceData.depotMaps.get(order.src_id);
		depotList.add(order);
	}

	/**
	 * 获取距离newstartorder近的该仓库的静态订单，已排序
	 * @param sortLib
	 * @param depotLists
	 * @param newStartOrder
	 * @return
	 */
	private static List<Order> getAPartDepot(
			Map<String, List<SortNode>> sortLib, List<Order> depotLists,
			ResultOrder newStartOrder) {
		if (depotLists.size() == 0)
			depotLists = getMinDepotOrderList(newStartOrder);
		// if (depotLists.size() > 0) {// 后面所有的 depot 点都使用上了
		// 获取距离 newStartOrder 最近的一个起始点
		int minDist = Integer.MAX_VALUE;
		Order order = null;//距离第一单出发地点最近的订单
		for (Order o : depotLists) {
			Node node = ServiceData.localPacageMaps.get(o.dest_id);//o目的地
			Node node2 = ServiceData.localPacageMaps.get(newStartOrder.Addr);//第一单出发地点
			int dist = Rule.distanceTime(node, node2);
			if (dist < minDist) {
				order = o;
				minDist = dist;
			}
		}
		List<Order> orders = CreateSortLib.getSortOrders(sortLib, depotLists,
				order);//该仓库订单排序
		return orders;
	}
/**
 * 
 * @param newStartOrder
 * @return 返回距离该调度点最近的仓库的所有订单
 */
	private static List<Order> getMinDepotOrderList(ResultOrder newStartOrder) {
		List<Order> depotList = new ArrayList<>();
		int minDist = Integer.MAX_VALUE;
		// 获取距离最近的 depot
		for (Entry<String, List<Order>> entry : ServiceData.depotMaps//静态订单映射
				.entrySet()) {
			String key = entry.getKey();
			if (entry.getValue().size() > 0) {
				Node node = ServiceData.localPacageMaps.get(key);
				Node node2 = ServiceData.localPacageMaps
						.get(newStartOrder.Addr);
				int dict = Rule.distanceTime(node, node2);
				if (dict < minDist) {
					minDist = dict;
					depotList = entry.getValue();
				}
			}
		}
		return depotList;
	}

	/**
	 * 
	 * 将o2o 订单从中间开始进行合并
	 * 
	 * @param o2oList
	 *            初步合并的o2o 订单，o2o订单段的集合
	 * @param depotId
	 *            该序列的 o2o 最近的 Id，该仓库的id
	 * @return 返回 o2o 与电商订单合并的路径，但前段没有合并，还是 o2o 起头
	 */
	public static List<List<ResultOrder>> mergeO2oOrderWithDepotOrders(
			List<List<ResultOrder>> o2oLists, String depotId) {
		List<List<ResultOrder>> resultLists = new ArrayList<>();
		List<Order> depotLists = ServiceData.depotMaps.get(depotId);//获取该仓库所有静态订单
		Map<String, List<SortNode>> sortLib = ServiceData.sortLibMap;
		while (o2oLists.size() > 0) {
			totalPrio++;
			List<ResultOrder> o2oOrders = o2oLists.remove(0);//获取一段
			List<ResultOrder> resultOrders = new ArrayList<>();
			ResultOrder startEndOrder, depotOrder, o2oStartOrder;
			o2oStartOrder = o2oOrders.get(0);//该段的第一单
			List<Order> orders1 = addAPartOrders(sortLib, depotLists,
					o2oStartOrder);//距离o2oStartOrder近的静态订单集合，用来填充的
			resultOrders = Utils.tsp(orders1, o2oOrders, 0);//使用分支界限调整总段，进行填充
			if (resultOrders.size() == 0) {
				// System.out.println("\n 没有完成");
				resultOrders.addAll(o2oOrders);
			}
			for (int i = 0; i < o2oLists.size(); i++) {
				o2oOrders = o2oLists.get(i);
				startEndOrder = (ResultOrder) getLast(resultOrders);//获取上一次最后的调度
				o2oStartOrder = o2oOrders.get(0);
				int toO2oOrderTime = Rule.getNextNodeTime(startEndOrder,
						o2oStartOrder);
				if (toO2oOrderTime > o2oStartOrder.Arrival_time) {
					continue;// 前一段和后一段 o2o 订单连接不上，如果连接上，则超时了
				} else {// 没有超时，进行填充电商订单
					if (toO2oOrderTime <= o2oStartOrder.Arrival_time
							&& o2oStartOrder.Arrival_time <= toO2oOrderTime + 5) {//等不超过5分钟
						resultOrders.addAll(o2oOrders);//加入该段
						o2oLists.remove(i);
						i--;//？？？
						// 为保证节点的时间正确，使用 o2o 节点代替depot
						ResultOrder depot = resultOrders.get(0).clone();
						Rule.calFitting(resultOrders, depot, depot.Arrival_time);
					} else {
						List<Order> orders = addAPartOrders(sortLib,
								depotLists, o2oStartOrder);//距离o2ostartorder点最近的静态订单集合，多加一段
						depotOrder = Utils.createResultOrder(orders.get(0),
								true);
						int startTime = Rule.getNextNodeTime(startEndOrder,
								depotOrder);
						depotOrder.Departure = startTime;//出发时间
						toO2oOrderTime = Rule.getNextNodeTime(depotOrder,
								o2oStartOrder);
						if (toO2oOrderTime > o2oStartOrder.Arrival_time) {//不超时
							for (Order order : orders) {
								returnDepot(order);//加入静态订单
							}
						} else {
							List<ResultOrder> resultOrders2 = addOneMoreResultOrders(
									sortLib, depotLists, o2oStartOrder,
									startTime);
							if (resultOrders2.size() > 0) {
								startEndOrder = (ResultOrder) getLast(resultOrders2);
								startTime = Rule.getNextNodeTime(startEndOrder,
										depotOrder);
								resultOrders.addAll(resultOrders2);// 添加中间片段
							}
							resultOrders2 = Utils.tsp(orders, o2oOrders,
									startTime);//路径调整
							if (resultOrders2.size() == 0) {
								System.out.println("中间也有为空的");
							}
							resultOrders.addAll(resultOrders2);// 添加中间片段
							o2oLists.remove(i);
							i--;
							// 为保证节点的时间正确，使用 o2o 节点代替deopt
							ResultOrder depot = resultOrders.get(0).clone();
							Rule.calFitting(resultOrders, depot,
									depot.Arrival_time);
						}
					}
				}
			}
			resultLists.add(resultOrders);
		}
		return resultLists;
	}

	/*private static List<ResultOrder> addOnePrioOrder(List<Order> orders,
			List<ResultOrder> o2oOrders) {
		// System.out.println("开始的仓库点大小" + orders.size());
		List<ResultOrder> resultOrders = Utils.tsp(orders, o2oOrders, 0);
		return resultOrders;
	}*/



	/**
	 * 将该段的订单进行截取，以适应规划好的路径
	 * 
	 * @param orders
	 *            要截取的订单
	 * @param newStartOrder
	 *            下一个 o2o开始的节点
	 * @param depotLists
	 *            初始化的订单
	 * @param newStartTime
	 *            此时仓库里的时间
	 * @return
	 */
	public static List<ResultOrder> cutResultOrders(List<Order> orders,
			ResultOrder newStartOrder, List<Order> depotLists, int startTime) {
		List<ResultOrder> resultOrders = Utils.tsp(orders);
		ResultOrder startOrder = resultOrders.get(0).clone();
		Rule.calFitting(resultOrders, startOrder, startTime);
		ResultOrder endOrder = (ResultOrder) getLast(resultOrders);
		int newStartTime = Rule.getNextNodeTime(endOrder, newStartOrder);
		while (newStartTime > newStartOrder.Arrival_time) {// 超时了，要进行截取
			Order order = orders.remove(orders.size() - 1);
			depotLists.add(order);
			if (orders.size() == 0) {// 加不进去，退出
				resultOrders = new ArrayList<>();
				break;
			}
			resultOrders = Utils.tsp(orders);
			Rule.calFitting(resultOrders, startOrder, startTime);
			endOrder = (ResultOrder) getLast(resultOrders);
			newStartTime = Rule.getNextNodeTime(endOrder, newStartOrder);
		}
		return resultOrders;
	}

	public static List<ResultOrder> addOneMoreResultOrders(
			Map<String, List<SortNode>> sortLib, List<Order> depotOrders,
			ResultOrder newStartOrder, int startTime) {
		List<ResultOrder> resultList = new ArrayList<>();
		List<Order> orders = addAPartOrders(sortLib, depotOrders, newStartOrder);
		List<ResultOrder> resultOrders = Utils.tsp(orders);
		ResultOrder depotOrder = resultOrders.get(0).clone();
		Rule.calFitting(resultOrders, depotOrder, startTime);
		startTime = depotOrder.Arrival_time; // 更新新的到达仓库时间
		ResultOrder endOrder = (ResultOrder) getLast(resultOrders);
		int newStartTime = Rule.getNextNodeTime(endOrder, newStartOrder);
		while (newStartTime < newStartOrder.Arrival_time) {
			// 可能还可以增加，如果不符合，则抛弃并将这些电商包裹归还
			resultList.addAll(resultOrders);

			orders = addAPartOrders(sortLib, depotOrders, newStartOrder);
			if (orders.size() == 0) {
				System.out.println("超支");
				break;
			}
			resultOrders = Utils.tsp(orders);
			depotOrder = resultOrders.get(0).clone();
			Rule.calFitting(resultOrders, depotOrder, startTime);
			startTime = depotOrder.Arrival_time; // 更新新的到达仓库时间
			endOrder = (ResultOrder) getLast(resultOrders);
			newStartTime = Rule.getNextNodeTime(endOrder, newStartOrder);
		}
		// 将不符合的部分归还
		depotOrders.addAll(orders);
		return resultList;
	}

	/**
	 * 加多一段仓库里的订单在空闲时间进行填充
	 * 
	 * @param sortLib所有点的最近20点排序
	 * @param depotOrders 该仓库所有静态订单
	 * @param newStartOrder 第一个调度
	 * @return 与第一个调度距离最近的静态订单集合，即要填充的订单
	 */
	public static List<Order> addAPartOrders(
			Map<String, List<SortNode>> sortLib, List<Order> depotOrders,
			ResultOrder newStartOrder) {
		List<Order> orders;
		if (depotOrders.size() == 0)//为0则用距其最近点的所有订单
			depotOrders = getMinDepotOrderList(newStartOrder);//最近点的所有订单
		int minDist = Integer.MAX_VALUE;
		Order order = null;//记录距离（调度单地点）最近的（到达地点的订单）
		for (Order o : depotOrders) {
			Node node = ServiceData.localPacageMaps.get(o.dest_id);//o单到达地点
			Node node2 = ServiceData.localPacageMaps.get(newStartOrder.Addr);//调度单地点
			int dist = Rule.distanceTime(node, node2);
			if (dist < minDist) {
				order = o;
				minDist = dist;
			}
		}
		orders = CreateSortLib.getSortOrders(sortLib, depotOrders, order);
		return orders;
	}

	/**
	 * 获取距离第一个订单片段最近的电商订单<BR>
	 * 
	 * @param depotLists
	 *            第一个订单
	 * @param addr
	 *            要寻找的 Id 号
	 * @return
	 */
	private static Order getStartOrder(List<Order> depotLists, String addr) {
		Node node = ServiceData.localPacageMaps.get(addr);
		int minDist = Integer.MAX_VALUE;
		Order minOrder = null;
		for (Order order : depotLists) {
			Node node2 = ServiceData.localPacageMaps.get(order.dest_id);
			int dist = Rule.distanceTime(node, node2);
			if (dist < minDist) {
				minDist = dist;
				minOrder = order;
			}
		}
		return minOrder;
	}

	/**
	 * 获取列表里最后一个元素，并返回该元素
	 * 
	 * @param list
	 * @return
	 */
	public static Object getLast(List list) {
		return list.get(list.size() - 1);
	}
}
