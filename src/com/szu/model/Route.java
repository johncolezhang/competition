package com.szu.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Route {
	private List<ResultOrder> lists;//调度计划列表
	// 此处记录的是每段路径的适应值
	private int fitting;
	private String Courier_id;//快递员id
	private ResultOrder depotOrder;//取货点的调度信息

	// 每条路都有一个时间，主要是演化过程中，每条路径的耗时都不一样，但是同一批的路径，起始时间相同
	public Route(String Courier_id) {
		fitting = 0;
		lists = new ArrayList<>();
		this.Courier_id = Courier_id;
	}

	public void setFitting(int fitting) {
		this.fitting = fitting;
	}

	public int getFitting() {
		return fitting;
	}

	/**
	 * 通过订单，获取目的地点
	 */
	public void addResultOrders(List<Order> lists2) {
		Collections.shuffle(lists2);// 打乱顺序，创建不同的路径
		for (Order order : lists2) {
			ResultOrder destOrder = createResultOrder(order, false);//添加送订单，利用订单创建调度计划，Num为负
			lists.add(destOrder);
		}
		this.depotOrder = createResultOrder(lists2.get(0), true);//取货点订单
	}

	public ResultOrder getDepotOrder() {
		return depotOrder;
	}

	/*
	 * public void addO2oResultOrder(List<Order> o2oList) { for (Order order :
	 * o2oList) { ResultOrder resultOrder = createResultOrder(order, true); int
	 * randIndex = (int) (Math.random() * lists.size()); lists.add(randIndex,
	 * resultOrder);
	 * 
	 * resultOrder = createResultOrder(order, false); randIndex = (int)
	 * (Math.random() * lists.size()); lists.add(randIndex, resultOrder); } }
	 */
	private ResultOrder createResultOrder(Order order, boolean isSrc) {
		ResultOrder resultOrder = new ResultOrder();
		resultOrder.Courier_id = Courier_id;
		if (isSrc) {//取货量
			resultOrder.Addr = order.src_id;//起点id
			resultOrder.Amount = order.num;
		} else {//送货量
			resultOrder.Addr = order.dest_id;//目的地id
			resultOrder.Amount = -order.num;
		}
		resultOrder.Order_id = order.order_id;
		return resultOrder;
	}

	public List<ResultOrder> getLists() {
		return lists;
	}

	public Route clone() {
		Route route = new Route(this.Courier_id);
		route.fitting = this.fitting;
		for (int i = 0; i < this.lists.size(); i++) {
			route.lists.add(lists.get(i).clone());
		}
		route.depotOrder = this.depotOrder.clone();
		return route;
	}//深度拷贝路线，以获取新的实例

	@Override
	public String toString() {
		return "Route [fitting=" + fitting + ", Courier_id=" + Courier_id + "]";
	}

}
