package com.szu.model;

public class OrderNode implements Comparable<OrderNode> {
	String orderName;
	int dist;

	@Override
	public int compareTo(OrderNode o) {
		if (this.dist < o.dist)
			return -1;
		if (this.dist > o.dist)
			return 1;
		return 0;
	}
}
