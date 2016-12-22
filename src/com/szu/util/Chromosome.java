package com.szu.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.szu.model.Order;
import com.szu.model.ResultOrder;

public class Chromosome {
	public final static double MUTATE_REVERSE = 0.5;
	public final static double MUTATE_SWAP_NODE = 0.5;

	/**
	 * 交叉操作
	 * 
	 * @param route1
	 *            进行交叉的路径1
	 * @param route2
	 *            进行交叉的路径2
	 */

	public static void crossOperator(List<ResultOrder> route1,
			List<ResultOrder> route2) {
		int size1, size2, min;
		double rd;
		int offset = 0, length = 0;
		size1 = route1.size();
		size2 = route2.size();
		min = Math.min(size1, size2);
		if (min <= 1)
			return;
		// 选择变异点
		while (0 == length) {
			rd = Math.random();
			offset = (int) (min * rd);
			length = (int) ((min - offset) * Math.random());
		}
		List<ResultOrder> routeSeg1, routeSeg2;
		routeSeg1 = new ArrayList<>();
		routeSeg2 = new ArrayList<>();
		for (int i = offset; i < offset + length; i++) {
			routeSeg1.add(route1.get(i).clone());
			routeSeg2.add(route2.get(i).clone());
		}

		crossover(route1, routeSeg2, offset);
		crossover(route2, routeSeg1, offset);
		Rule.rectify(route1);
		Rule.rectify(route2);
	}

	/**
	 * 变异操作
	 * 
	 * @param route
	 * 路线变异
	 */
	public static void mutateOperator(List<? extends ResultOrder> route) {
		if (MUTATE_REVERSE > Math.random())//超过0.5则进行变异
			mutateReverse(route);
		if (MUTATE_SWAP_NODE > Math.random())
			mutateSwapNode(route);
		Rule.rectify(route);
	}

	/**
	 * 进行交叉变换
	 * 
	 * @param route
	 *            要交叉的路径
	 * @param routeSeg
	 *            要交叉的路径片段
	 * @param offset
	 *            交叉起始位置
	 */
	private static void crossover(List<ResultOrder> route,
			List<ResultOrder> routeSeg, int offset) {
		List<ResultOrder> prior, tail, mid, unique;
		unique = new ArrayList<>();
		int length = routeSeg.size();
		mid = new ArrayList<>();
		for (int i = offset; i < offset + length; i++) {
			mid.add(route.get(i).clone());
		}
		// 获取两个路径不重复部分的节点
		for (ResultOrder resultOrderSeg : routeSeg) {
			boolean isUnique = true;
			for (ResultOrder resultOrder : mid) {
				if (resultOrder.Order_id.equals(resultOrderSeg.Order_id)) {
					isUnique = false;
					break;// 此处要注意，route和routeSeg是里的ResultOrder两个不同的实例
				}
			}
			if (isUnique)
				unique.add(resultOrderSeg.clone());
		}
		System.out.println(unique.size());
		prior = new ArrayList<>();
		tail = new ArrayList<>();
		for (int i = 0; i < offset; i++) {
			prior.add(route.get(i).clone());
		}
		for (int i = offset + length; i < route.size(); i++) {
			tail.add(route.get(i).clone());
		}
		for (ResultOrder resultOrder : routeSeg) {
			for (ResultOrder order : prior) {
				if (resultOrder.Order_id.equals(order.Order_id)) {
					System.out.println("prio" + order.Order_id);
					// 相等，则需要深拷贝
					unique.get(0).Assignment(order);
					unique.remove(0);
				}
			}
			for (ResultOrder order : tail) {
				if (resultOrder.Order_id.equals(order.Order_id)) {
					System.out.println("tail" + order.Order_id);
					// 相等，则需要深拷贝
					unique.get(0).Assignment(order);
					unique.remove(0);
				}
			}
		}
		route.clear();
		route.addAll(prior);
		for (ResultOrder resultOrder : routeSeg) {
			route.add(resultOrder.clone());
		}
		route.addAll(tail);
	}

	/**
	 * 将调度单中的一部分进行反转操作
	 * 
	 * @param route
	 */
	private static void mutateReverse(List<? extends ResultOrder> route) {
		int index1, index2, size;

		size = route.size();
		if (size <= 1)
			return;
		index1 = (int) (Math.random() * size);
		do {
			index2 = (int) (Math.random() * size);//index2与index1不能一样，不然重新赋index2
		} while (index1 == index2);
		if (index1 < index2) {
			Collections.reverse(route.subList(index1, index2));//将index1到index2之间的list反转
		} else {
			Collections.reverse(route.subList(index2, index1));
		}

	}

	/**
	 * 选择随机两单进行路径交换
	 * 
	 * @param route
	 */
	private static void mutateSwapNode(List<? extends ResultOrder> route) {//将随意两个进行交换
		ResultOrder tmp, tmp1, tmp2;
		int index1, index2, size;

		size = route.size();
		if (size <= 1)
			return;
		index1 = (int) (Math.random() * size);
		do {
			index2 = (int) (Math.random() * size);
		} while (index1 == index2);
		tmp1 = route.get(index1);
		tmp = tmp1.clone();
		tmp2 = route.get(index2);
		tmp2.Assignment(tmp1);// 将tmp2的值赋值给tmp1
		tmp.Assignment(tmp2);// tmp的值赋给tmp2
	}

	public static List<ResultOrder> insertO2OOrder(
			List<ResultOrder> resultOrders, List<Order> o2oOrders) {
		return null;
	}

	public static void mutateAdd(List<? extends ResultOrder> route) {

	}

	public static void mutateDel(List<? extends ResultOrder> route) {

	}
}
