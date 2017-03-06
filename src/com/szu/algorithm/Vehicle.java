package com.szu.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ResultOrder;
import com.szu.model.Route;
import com.szu.model.ServiceData;
import com.szu.util.Chromosome;
import com.szu.util.LocalSearch;
import com.szu.util.Rule;
/**
 * 传入一组routes，然后使用遗传算法完成配送
 * @author johncole
 * @ClassName Vehicle
 * @date 2017年1月16日
 *
 */
public class Vehicle {
	private List<ResultOrder> servicedLists;//结果订单列表，以完成服务的订单
	private List<Route> routes;
	private int time;
	private int fitting;// 一辆车的适应值
	private int weight; // 车载容量，最大为140
	private final static int DELAY_TIME = 80;// 3 点，不超过50min
	private final static int MAX_WEIGHT = 140;
	private final static int FIN_SER = 850;
	private List<Order> o2oOrders;//o2o订单列表
	/**
	 * 记录当前的仓库
	 */
	private List<ResultOrder> depots;//仓库订单
	String Courier_id;//快递员id
	private int popSize;//订单数量

	public Vehicle(String courier_id, int popSize) {
		this.servicedLists = new ArrayList<>();
		this.depots = new ArrayList<>();
		this.Courier_id = courier_id;
		this.routes = new ArrayList<>();
		this.popSize = popSize;
		this.time = 0;// 起始时间为0
		this.weight = 0;
	}

	public void run() {
		for (int j = 0; j < popSize; j++) {
			int size = routes.size();//路径个数
			Route route, route2;
			for (int i = 0; i < size / 2; i++) {
				//抽取两个调度表进行变异，之后在插入队尾
				route = routes.get(i).clone();// 正向深度拷贝，获取独立实例
				route2 = routes.get(size - i - 1).clone();//逆向拷贝
				// Chromosome.crossOperator(route.getLists(),
				// route2.getLists());
				Chromosome.mutateOperator(route.getLists());//lists中为所有要送的订单
				Chromosome.mutateOperator(route2.getLists());//变异操作
				// 计算分数，时间会随之改变
				int fitting = Rule.calFitting(route.getLists(),
						route.getDepotOrder(), time);//获取所有要送的订单，以及出发点的信息
				route.setFitting(fitting);
				fitting = Rule.calFitting(route2.getLists(),
						route.getDepotOrder(), time);
				route2.setFitting(fitting);
				routes.add(route);
				routes.add(route2);
			}
			// 进行局部优化
			for (int i = 0; i < routes.size(); i++) {
				Route route3 = routes.get(i);
				LocalSearch.localStaticSearch(route3.getLists(),
						route3.getDepotOrder());//调整各订单调度之间的顺序，使其路径距离最短
				// 计算适应值
				int fitting = Rule.calFitting(route3.getLists(),
						route3.getDepotOrder(), time);
				route3.setFitting(fitting);
			}
			selectRoute();//选择routes中最适合的路径
		
		}
		// 增加 o2o 订单
		List<ResultOrder> curServiceLists = new ArrayList<>();
		//curServiceLists.addAll(depots);
		curServiceLists.addAll(routes.get(0).getLists());
		Rule.calFitting(curServiceLists, routes.get(0).getDepotOrder(), time);
		//insertO2OOrder(curServiceLists, this.o2oOrders);
		// 一次结束后，将其加入到服务队列，由于进行了选择，故，最好的为第一条路
		servicedLists.addAll(curServiceLists);
		fitting = Rule.calFitting(servicedLists, routes.get(0).getDepotOrder(),
				0);// 计算时间
		// 回到仓库的时间
		time = routes.get(0).getDepotOrder().Arrival_time;
		weight = 0;// 车容量清0，一次任务完成
	}

	/**
	 * 插入o2o列表
	 * @param curServiceLists 当前调度
	 * @param o2oOrders
	 */
	private void insertO2OOrder(List<ResultOrder> curServiceLists,
			List<Order> o2oOrders) {
		List<Order> orders = new ArrayList<>();
		int pickupTime = this.time - DELAY_TIME;//
		int deliveryTime = curServiceLists.get(curServiceLists.size() - 1).Departure;//出发时间
		ResultOrder insertResultOrder;
		for (Order order : o2oOrders) {
			insertResultOrder = createResultOrder(order, true);
			int index = 1;
			index = insertO2OOrderIndex(curServiceLists, insertResultOrder,
					index, true);
			if (index < 0)
				continue;
			curServiceLists.add(index, insertResultOrder);
			if (!Rule.isWeightLegal(curServiceLists)) {
				curServiceLists.remove(insertResultOrder);
				continue;
			}
			Rule.calFitting(curServiceLists, depots.get(0), this.time);
			insertResultOrder = createResultOrder(order, false);
			index++;// 多了 一个元素，保证在该元素右边，故移动一位
			index = insertO2OOrderIndex(curServiceLists, insertResultOrder,
					index, false);
			curServiceLists.add(index, insertResultOrder);//加入
			Rule.calFitting(curServiceLists, depots.get(0), this.time);
			orders.add(order);
		}
		// 将处理过的 O2O 点移除
		o2oOrders.removeAll(orders);
		if (o2oOrders.size() > orders.size()) {
			Order order = ServiceData.OrderPackageMaps
					.get(depots.get(0).Order_id);
			System.out.println(order.src_id + " 还剩  "
					+ (o2oOrders.size() - orders.size()) + " 没有处理");
		}
	}

	/**
	 * 获取插入点的最佳位置
	 * 
	 * @param curServiceLists
	 * @param insertResultOrder
	 * @param index
	 *            插入点的初始位置，为1开,始即 index>=1
	 * @return 插入点位置
	 */
	private int insertO2OOrderIndex(List<ResultOrder> curServiceLists,
			ResultOrder insertResultOrder, int index, boolean isSrc) {
		int i;// 主要针对商户点，因为是收件，为正，派件为负
		int tmp, minTime = Integer.MAX_VALUE;
		for (i = index; i < curServiceLists.size(); i++) {
			// 插入后超重了
			if (Rule.isOverWeight(curServiceLists, insertResultOrder, i)) {
				continue;
			}
			tmp = additionTime(curServiceLists, insertResultOrder, i);
			if (minTime > tmp) {
				index = i;
				minTime = tmp;
			}
		}
		return index;
	}
/**
 * 计算加入调度后的总时间
 * @param list
 * @param insertOrder
 * @param index
 * @return
 */
	private int additionTime(List<ResultOrder> list, ResultOrder insertOrder,
			int index) {
		list.add(index, insertOrder);
		ResultOrder depotOrder = list.get(0).clone();
		int fitting = Rule.calFitting(list, depotOrder, this.time);
		list.remove(index);
		return fitting;
	}

	/**
	 * 会修改类的属性，故要深度克隆进来
	 * 
	 * @param prioOrder
	 * @param middOrder
	 * @param tailOrder
	 * @return
	 */
	private int additionTime(ResultOrder prioOrder, ResultOrder middOrder,
			ResultOrder tailOrder) {
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
/**
 * 生成调度
 * @param order
 * @param isSrc
 * @return
 */
	private ResultOrder createResultOrder(Order order, boolean isSrc) {
		ResultOrder resultOrder = new ResultOrder();
		resultOrder.Courier_id = Courier_id;
		if (isSrc) {
			resultOrder.Addr = order.src_id;
			resultOrder.Amount = order.num;
			// System.out.println("src:" + order.src_id);
		} else {
			resultOrder.Addr = order.dest_id;
			resultOrder.Amount = -order.num;
			// System.out.println("dest:" + order.dest_id);
		}
		resultOrder.Order_id = order.order_id;
		return resultOrder;
	}

	public int getTime() {
		return time;
	}

	public int getFitting() {
		return fitting;
	}

	public List<ResultOrder> getServicedLists() {
		return servicedLists;
	}

	public String getCourier_id() {
		return Courier_id;
	}

	public int getWeight() {
		return weight;
	}

	/**
	 * 根据fitting进行降序排序，挑选0到popsize的route
	 * 选择优秀的路径
	 */
	protected void selectRoute() {
		Collections.sort(routes, new Comparator<Route>() {

			@Override
			public int compare(Route o1, Route o2) {
				int d1 = o1.getFitting();
				int d2 = o2.getFitting();
				if (d1 < d2)
					return -1;
				if (d1 > d2)
					return 1;
				return 0;// 升序排序
			}

		});
		routes = routes.subList(0, popSize);// 精英选择，选择最佳的路径
	}

	public void addO2oOrders(List<Order> o2oOrders) {
		this.o2oOrders = new ArrayList<>();
		this.o2oOrders.addAll(o2oOrders);
	}

	/**
	 * 添加仓库货物到depots中
	 * 
	 * @param lists
	 */
	public void addOrder(List<Order> lists, List<Order> o2oOrders) {
		depots.clear();
		this.weight = 0;
		for (Order order : lists) {
			ResultOrder srcOrder = new ResultOrder();
			srcOrder.Courier_id = Courier_id;
			srcOrder.Addr = order.src_id;
			srcOrder.Amount = order.num;
			srcOrder.Order_id = order.order_id;
			this.depots.add(srcOrder);
			this.weight += order.num;
		}
		this.o2oOrders = o2oOrders;
		initRoute(lists, o2oOrders);//初始化路径
	}
/**
 * 初始化路径，将调度列表插入route中，然后生成总routes
 * @param lists
 * @param o2oList
 */
	private void initRoute(List<Order> lists, List<Order> o2oList) {
		routes.clear();
		// 初始化路径
		for (int i = 0; i < popSize; i++) {
			Route route = new Route(Courier_id);
			route.addResultOrders(lists);//在这里打乱每个list中订单的顺序
			// route.addO2oResultOrder(o2oList);
			int fitting = Rule.calFitting(route.getLists(),
					route.getDepotOrder(), time);
			route.setFitting(fitting);
			routes.add(route);
		}//把list复制popsize次插到routes中
	}

}
