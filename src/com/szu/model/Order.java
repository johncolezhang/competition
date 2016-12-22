package com.szu.model;
//订单
public class Order {
	/**
	 * 订单Id
	 */
	public String order_id;
	/**
	 * 目的点Id
	 */
	public String dest_id;
	/**
	 * 源地点Id
	 */
	public String src_id;
	/**
	 * 包裹数量
	 */
	public int num;
	/**
	 * 最早到达取包裹时间
	 */
	public int pickup_time = 0;
	/**
	 * 最迟送达时间
	 */
	public int delivery_time = 720;//12个小时，静态点在12小时内送完就行

	@Override
	public String toString() {
		return "Order [order_id=" + order_id + ", dest_id=" + dest_id
				+ ", src_id=" + src_id + ", num=" + num + ", pickup_time="
				+ pickup_time + ", delivery_time=" + delivery_time + "]";
	}

}
