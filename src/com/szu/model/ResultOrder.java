package com.szu.model;
//调度计划
public class ResultOrder {
	/**
	 * 1 列 快递员编号
	 */
	public String Courier_id;
	/**
	 * 2 列配送地址
	 */
	public String Addr;
	/**
	 * 3 列到达时间
	 */
	public int Arrival_time;
	/**
	 * 4 列 离开时间
	 */
	public int Departure;
	/**
	 * 5 列配送数量
	 */
	public int Amount;
	/**
	 * 6 列订单编号
	 */
	public String Order_id;

	/**
	 * 深度拷贝
	 */
	public ResultOrder clone() {
		ResultOrder order = new ResultOrder();
		order.Courier_id = this.Courier_id;
		order.Addr = this.Addr;
		order.Amount = this.Amount;
		order.Order_id = this.Order_id;
		order.Arrival_time = this.Arrival_time;
		order.Departure = this.Departure;
		return order;

	}

	/**
	 * 当前对象的属性赋值给传入的对象
	 * 
	 * @param order
	 *            被赋值的对象
	 */
	public void Assignment(ResultOrder order) {
		order.Courier_id = this.Courier_id;
		order.Addr = this.Addr;
		order.Amount = this.Amount;
		order.Order_id = this.Order_id;
		order.Arrival_time = this.Arrival_time;
		order.Departure = this.Departure;
	}

	@Override
	public String toString() {
		return "ResultOrder [Courier_id=" + Courier_id + ", Addr=" + Addr
				+ ", Arrival_time=" + Arrival_time + ", Departure=" + Departure
				+ ", Amount=" + Amount + ", Order_id=" + Order_id + "]";
	}

}
