package com.szu.util;

import java.util.ArrayList;
import java.util.List;

import com.szu.algorithm.BrandAndBoundForTSP;
import com.szu.algorithm.BrandAndBoundForTSP.City;
import com.szu.algorithm.Distance;
import com.szu.algorithm.DynamicBrandAndBoundForTSP;
import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;

public class Utils {

	public static int totalIllegal = 0;

	/**
	 * o2o订单的tsp
	 * @param depotOrders 静态订单集合（距离o2oResultOrders出发点最近的）
	 * @param o2oResultOrders o2o订单的一段
	 * @param startTime 开始时间
	 * @return
	 */
	public static List<ResultOrder> tsp(List<Order> depotOrders,
			List<ResultOrder> o2oResultOrders, int startTime) {
		// if (startTime >= 0)
		// return new ArrayList<>();
		ResultOrder depotOrder = createResultOrder(depotOrders.get(0), true);//起始第一单出发
		DynamicBrandAndBoundForTSP brandAndBoundForTSP = new DynamicBrandAndBoundForTSP();
		brandAndBoundForTSP.init(depotOrders, o2oResultOrders, depotOrder,
				startTime);//生成点到点之间的距离
		List<ResultOrder> resultOrders = brandAndBoundForTSP.getResultOrders();//使用tsp
		if (resultOrders.size() == 0) {
			if (DynamicBrandAndBoundForTSP.error.equals("exceedWeigth")) {
				Order order1 = depotOrders.remove(depotOrders.size() - 1);
				MergeOrderUtils.returnDepot(order1);
				brandAndBoundForTSP.init(depotOrders, o2oResultOrders,
						depotOrder, startTime);
				resultOrders = brandAndBoundForTSP.getResultOrders();
				if (resultOrders.size() == 0) {// 没有 仓库点了
					System.out.println("没有使用分支界限法" + startTime);
					System.out.println(o2oResultOrders.get(0).toString());
					for (Order order : depotOrders) {
						MergeOrderUtils.returnDepot(order);
					}
					return o2oResultOrders;
				}
			} else {
				if (startTime == 0)
					totalIllegal++;
				System.out.println("没有使用分支界限法,非超时" + startTime);
				System.out.println(o2oResultOrders.get(0).toString());
				for (Order order : depotOrders) {
					MergeOrderUtils.returnDepot(order);
				}
				return o2oResultOrders;
			}
		}
		ResultOrder endOrder = (ResultOrder) MergeOrderUtils
				.getLast(resultOrders);
		while (endOrder.Arrival_time > 720) {// 超时了
			int index = CreateSortLib.containOrder(depotOrders,
					endOrder.Order_id);
			if (index == -1) {// 只剩下o2o 了
				break;
			}
			Order order = depotOrders.remove(index);
			MergeOrderUtils.returnDepot(order);
			// 移除最后一个，不影响结果
			resultOrders.remove(resultOrders.size() - 1);
			for (ResultOrder resultOrder : resultOrders) {
				if (resultOrder.Order_id.equals(endOrder.Order_id)) {
					endOrder = resultOrder;
					break;
				}
			}
			resultOrders.remove(endOrder);
			endOrder = (ResultOrder) MergeOrderUtils.getLast(resultOrders);
		}
		// while (endOrder.Arrival_time > 720) {// 超时了
		// int index = CreateSortLib.containOrder(depotOrders,
		// endOrder.Order_id);
		// if (index == -1) {// 只剩下o2o 了
		// break;
		// }
		// Order order = depotOrders.remove(index);
		// MergeOrderUtils.returnDepot(order);
		// brandAndBoundForTSP.init(depotOrders, o2oResultOrders, depotOrder,
		// startTime);
		// resultOrders = brandAndBoundForTSP.getResultOrders();
		// endOrder = (ResultOrder) MergeOrderUtils.getLast(resultOrders);
		// }
		if (depotOrders.size() * 2 + o2oResultOrders.size() != resultOrders
				.size())
			System.out.println("订单不对");
		return resultOrders;
	}

	/**
	 * 静态订单的tsp
	 * 通过订单,使用分支界限的方法进行求解最优派件路径
	 * 得到的路径为一个派件员要派件的一小段路径，将这些小片段组合起来，则形成一个派件员日常派件
	 * 获取的小片段起始时间都是为0,后面可以根据需要进行时间平移
	 * 
	 * @param orders
	 * @return
	 */
	public static List<ResultOrder> tsp(List<Order> orders) {
		BrandAndBoundForTSP tsp = new BrandAndBoundForTSP();
		List<ResultOrder> list = new ArrayList<>();
		List<ResultOrder> resultOrders = new ArrayList<>();
		if (orders.size() == 0)
			return resultOrders;
		ResultOrder resultOrder = createResultOrder(orders.get(0), true);//源地点
		list.add(resultOrder);
		for (Order order : orders) {//一个送，一个收
			resultOrder = createResultOrder(order, false);//目的地
			list.add(resultOrder);
			resultOrder = createResultOrder(order, true);//源地点
			resultOrders.add(resultOrder);
		}
		tsp.init(list, new Distance<ResultOrder>() {
			@Override
			public double dist(ResultOrder resultOrder, ResultOrder resultOrder2) {
				Node node = ServiceData.localPacageMaps.get(resultOrder.Addr);
				Node node2 = ServiceData.localPacageMaps.get(resultOrder2.Addr);
				// 形成环状
				int tmp = Rule.distanceTime(node, node2);
				return tmp;
			}//获取各点间距离，再赋给BBFT中的dist二维数组
		});
		
		City city = tsp.getBest();//获取最优的分支界限排序，最优点开始的一条路径
		for (int index : city.route) {
			index -= 1;//route第一点不算
			if (index > 0) {
				resultOrders.add(list.get(index));
			}
		}
		Rule.calFitting(resultOrders, resultOrders.get(0).clone(), 0);
		return resultOrders;
	}

	/**
	 * 通过订单创建结果对象
	 * 
	 * @param order
	 *            要创建结果对象的订单对象
	 * @param isSrc
	 *            进行判断是创建源地点，还是目的地的。
	 *            如果是源地点，则为 true，如果为目的地点，则为 false
	 * @return 返回创建好的结果对象，对象不包括派件员
	 */
	public static ResultOrder createResultOrder(Order order, boolean isSrc) {
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
