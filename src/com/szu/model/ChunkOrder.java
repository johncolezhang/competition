package com.szu.model;

import java.util.ArrayList;
import java.util.List;
//货物订单
public class ChunkOrder {
	public List<Order> depotOrders = new ArrayList<>();
	public List<Order> o2oOrders = new ArrayList<>();
	public int startTime = 0;//o2o订单的起始时间
}
