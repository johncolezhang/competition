package com.szu.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
//排序订单
public class SortNode implements Comparable<SortNode>, Serializable {
	/**
	 * 记录 O2O 起始订单的名称
	 */
	public List<String> o2oPickupOrderNameList = new ArrayList<>();
	/**
	 * 记录 O2O 结束订单的名称
	 */
	public List<String> o2oDeliveryOrderNameList = new ArrayList<>();
	/**
	 * 记录电商订单名称
	 */
	public List<String> staticOderNameList = new ArrayList<>();
	/**
	 * 计算两点间的距离
	 */
	public int dist;

	@Override
	public int compareTo(SortNode o) {
		if (this.dist < o.dist)
			return -1;
		if (this.dist > o.dist)
			return 1;
		return 0;
	}//升序排序
}
