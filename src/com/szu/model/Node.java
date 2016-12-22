package com.szu.model;

import java.util.ArrayList;
import java.util.List;
//存储网点，配送点以及商户点
public class Node {
	/**
	 * 网点，主要提供包裹服务
	 */
	public static int Site = 1;
	/**
	 * 目的点，收取包裹
	 */
	public static int Spot = 2;
	/**
	 * O2O商户，提供限时服务
	 */
	public static int Shop = 3;
	/**
	 * 对应服务点名
	 */
	public String name;//服务点类型
	public double Lng;//服务点经度
	public double Lat;//服务点纬度
	public int type;//服务点类型：网点，配送点，商户
	/**
	 * 记录 O2O 订单的名称
	 */
	public List<String> o2oPickupOrderNameList = new ArrayList<>();
	/**
	 * 记录 O2O 订单的名称
	 */
	public List<String> o2oDeliveryOrderNameList = new ArrayList<>();
	/**
	 * 记录电商订单名称
	 */
	public List<String> staticOderNameList = new ArrayList<>();

	@Override
	public String toString() {
		return "Node [name=" + name + ", Lng=" + Lng + ", Lat=" + Lat + "]";
	}

}
