package com.szu.model;

import java.util.ArrayList;
import java.util.List;
//仓库订单
public class DepotOrder {
	public int rank; // 用于排序
	public int centerTime;
	public Order depotOrder;
	public List<Order> o2oOrders = new ArrayList<>();
}
