package com.szu.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;
import com.szu.util.Rule;
import com.szu.util.Utils;

public class DynamicBrandAndBoundForTSP {
	private int MAX_NUM;
	private double[][] dist;
	private int n;
	private static int INF = Integer.MAX_VALUE;
	private int o2oStartIndex;
	private int depotWeight;
	private int o2oWeight;
	private int punish;
	private int startTime;
	private List<ResultOrder> list;
	private List<ResultOrder> o2oList;
	private List<Order> depotList;
	private int totalSize;
	public static String error = "";
	private City city;

	/**
	 * 
	 * @param depotOrders 静态订单集合（距离o2oResultOrders出发点最近的）
	 * @param o2oResultOrders o2o订单的一段
	 * @param resultOrder o2oResultOrders出发点
	 * @param startTime 开始时间
	 * 生成各点之间的距离（包含静态和动态）
	 */
	public void init(List<Order> depotOrders,
			List<ResultOrder> o2oResultOrders, ResultOrder resultOrder,
			int startTime) {
		o2oList = o2oResultOrders;//o2o订单的一段
		depotList = depotOrders;//静态订单集合
		totalSize = o2oList.size();
		totalSize += depotList.size() * 2;
		list = getResultOrder(depotOrders, o2oResultOrders, resultOrder);//生成总调度
		this.startTime = startTime;
		n = list.size();
		MAX_NUM = n + 2;//数组大小
		dist = new double[MAX_NUM][MAX_NUM];
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				// 第一个o2o 点只能被非 o2o 点连接
				if ((j == o2oStartIndex && i > o2oStartIndex + 1)) {
					dist[i][j] = INF;
					continue;
				}
				// 第二个 o2o 点只能被第一个连接
				// if (j == o2oStartIndex + 1) {
				// if (i == o2oStartIndex)
				// dist[i][j] = 0;
				// else
				// dist[i][j] = INF;
				// continue;
				// }
				if (i == j) {
					dist[i][j] = INF;
					continue;
				}
				if (i == o2oStartIndex) {// 第一个 o2o 点只能连接下一个 o2o 点
					if (j == o2oStartIndex + 1)
						dist[i][j] = 0;
					else
						dist[i][j] = INF;
					continue;
				}
				if (i == o2oStartIndex + 1 && j == o2oStartIndex) {
					dist[i][j] = 0;
					continue;
				}
				dist[i][j] = getDist(list.get(i - 1), list.get(j - 1));//两点之间的距离
			}
		}
	}

	public List<ResultOrder> getResultOrders() {
		List<ResultOrder> resultOrders = new ArrayList<>();
		City city = getBest();
		if (city == null) {// 没有找到合适的
			// if (startTime == 0) {
			// for (ResultOrder resultOrder : list) {
			// System.out.println(resultOrder.toString());
			// }
			// for (int i = 1; i <= n; i++) {
			// for (int j = 1; j <= n; j++) {
			// double a = dist[i][j];
			// if (a > 720)
			// System.out.printf("%1.0e	", a);
			// else
			// System.out.print(a + "	");
			// }
			// System.out.println();
			// }
			// }
			if (!error.equals("exceedWeigth")) {
				System.out.println(error + "  o2oStartIndex:" + o2oStartIndex
						+ "  o2oNum" + o2oList.size() + "  depotNum:"
						+ depotList.size());
				System.out.println(this.city.toString());
			}
			return resultOrders;
		}
		for (Order order : depotList) {
			ResultOrder resultOrder = Utils.createResultOrder(order, true);
			resultOrders.add(resultOrder);
		}
		for (int i : city.route) {
			i--;// 将其移位，与 list 对应上去
			if (i > 0) {// 排除第一个仓库点
				if (i == o2oStartIndex - 1) {
					if (punish > 0) {// 动态点只有两个
						for (ResultOrder resultOrder2 : o2oList) {
							resultOrders.add(resultOrder2.clone());
						}
					} else {// 将 o2o 收件点加上去
						for (int j = 0; j < o2oList.size() / 2; j++) {
							ResultOrder resultOrder2 = o2oList.get(j);
							resultOrders.add(resultOrder2.clone());
						}
					}
				} else if (i == o2oStartIndex) {// 第二个 o2o 点
					continue;
				} else {
					resultOrders.add(list.get(i));
				}
			}
		}
		Rule.calFitting(resultOrders, resultOrders.get(0).clone(), startTime);
		if (startTime == 0) {// 开头部分，进行紧凑处理
			// System.out.println("进行紧凑处理" + resultOrders.size());
			tightResultOrders(resultOrders);
		}
		if (resultOrders.size() != totalSize) {
			System.out.println(city.toString());
			System.out
					.println("规划前" + depotList.size() + "  " + o2oList.size());
			for (ResultOrder resultOrder : list) {
				System.out.println(resultOrder.toString());
			}
			System.out.println("规划后   " + startTime);
			for (ResultOrder resultOrder : resultOrders) {
				System.out.println(resultOrder.toString());
			}
			for (int i = 1; i <= n; i++) {
				for (int j = 1; j <= n; j++) {
					System.out.print(dist[i][j] + "	");
				}
				System.out.println();
			}
		}
		return resultOrders;
	}

	/**
	 * 进行紧凑处理，将前面的点占据的时间空处来<BR>
	 * kamyang Sep 6, 2016
	 * 
	 * @param resultOrders
	 */
	private void tightResultOrders(List<ResultOrder> resultOrders) {
		int startTime = 0;
		for (ResultOrder resultOrder : resultOrders) {
			// 第一个 o2o 收件点
			startTime = resultOrder.Departure - resultOrder.Arrival_time;
			if (resultOrder.Amount > 0 && startTime > 0) {
				break;
			}
		}
		Rule.calFitting(resultOrders, resultOrders.get(0).clone(), startTime);
	}

	private City getBest() {
		City startCity = new City();
		startCity.route = new int[MAX_NUM];
		startCity.visited = new boolean[MAX_NUM];
		startCity.route[1] = startCity.st;// 初始化第一个访问的节点
		startCity.visited[1] = true;
		startCity.pos = 1;// 当前的位置
		startCity.lb = getLb(startCity);
		City bestCity = null;
		Queue<City> queue = new LinkedList<>();
		queue.add(startCity);
		while (!queue.isEmpty()) {// 没有空
			City tempCity = queue.poll();// 将当前的队列放出来
			if (tempCity.pos == n - 1) {// 最后一个节点
				for (int i = 1; i <= n; i++) {
					if (!tempCity.visited[i]) {// 将最后一个节点加到访问队列里
						tempCity.visited[i] = true;
						tempCity.pos++;// 指向下一个
						tempCity.route[tempCity.pos] = i;
						tempCity.lb = getLb(tempCity);
						break;
					}
				}
				if (bestCity == null || bestCity.lb > tempCity.lb)
					bestCity = tempCity;
			} else {
				double minLb = Double.MAX_VALUE;
				List<City> list = new LinkedList<>();
				for (int i = 1; i <= n; i++) {
					if (!tempCity.visited[i]) {// 没有访问过，要创建相应对象
						City city = new City();
						city.pos = tempCity.pos + 1;
						city.route = Arrays.copyOf(tempCity.route, MAX_NUM);
						city.visited = Arrays.copyOf(tempCity.visited, MAX_NUM);
						city.visited[i] = true;
						city.route[city.pos] = i;
						if (i == o2oStartIndex) {// 当前为 o2o起始位置
							city.pos++;
							i++;
							city.visited[i] = true;
							city.route[city.pos] = i;
							city.lb = getLb(city);
							queue.add(city);
							continue;
						}
						city.lb = getLb(city);
						// 超重了，直接忽略，跳过
						// 出现违法现象，即在 o2o 商户点出现前出现了 o2o 派送点
						this.city = city;
						if (exceedWeigth(city) || isIllegal(city)
								|| isPunish(city))
							continue;

						if (city.lb <= minLb || i == o2oStartIndex) {
							list.add(city);
							minLb = Math.min(city.lb, minLb);
						}
					}
				}
				for (City city : list) {
					// 最小值的地方，或者该处为 o2o 首次出现的地方
					// if (o2oList.size() == 2 && depotList.size() > 8) {
					if (city.lb == minLb
							|| city.route[city.pos] == o2oStartIndex)
						queue.add(city);// 添加到队列里
						// } else {
						// queue.add(city);
						// }
				}
			}
		}
		return bestCity;
	}

	/**
	 * 处罚了，要进行处理，丢弃该路径<BR>
	 * kamyang Sep 5, 2016
	 * 
	 * @param city
	 * @return
	 */
	private boolean isPunish(City city) {
		error = "isPunish";
		// 如果本身出现惩罚情况，则o2o 点本身就只有两个，后面一定会连接上的，
		// 并不会增加新的惩罚情况，故不用处理，如果是含有处罚情况，
		// 则已经被归为两个 o2o 点，故不会在路径上显示出来
		int curTime = startTime;
		int curNodeIndex = city.route[1];
		boolean isAccessO2o = false;
		for (int i = 2; i <= city.pos; i++) {
			int nextNodeIndex = city.route[i];
			if (curNodeIndex < o2oStartIndex) {// 为静态点
				curTime += dist[curNodeIndex][nextNodeIndex];// 获取到达下一个点的时间
			} else if (curNodeIndex == o2oStartIndex) {
				// 出现了处罚情况
				isAccessO2o = true;
				ResultOrder resultOrder = o2oList.get(0);
				// list.get(curNodeIndex - 1);
				if (curTime > resultOrder.Arrival_time)
					return true;
				// 更新起始时间
				curTime = (int) (resultOrder.Arrival_time + dist[curNodeIndex][nextNodeIndex]);
			} else if (curNodeIndex == o2oStartIndex + 1) {
				// 新的起点，o2o 点收货时间不变
				ResultOrder resultOrder = list.get(curNodeIndex - 1);
				curTime = (int) (resultOrder.Departure + dist[curNodeIndex][nextNodeIndex]);
			} else {
				if (exceedDelivryTime(curTime, curNodeIndex))
					return true;
				curTime += dist[curNodeIndex][nextNodeIndex];// 获取到达下一个点的时间
			}
			curNodeIndex = nextNodeIndex;
		}
		if (isAccessO2o) {// 进入了，要判断后面的 o2o 点派件是否超时
			if (curNodeIndex == o2oStartIndex + 1) {
				// 新的起点，o2o 点收货时间不变
				ResultOrder resultOrder = list.get(curNodeIndex - 1);
				curTime = resultOrder.Departure;
			}
			for (int i = o2oStartIndex; i <= n; i++) {
				int nextNodeIndex = i;// 必须为没有访问过的 o2o 点
				if (city.visited[nextNodeIndex])// 访问过
					continue;
				if (exceedDelivryTime(curTime, curNodeIndex))
					return true;
				curTime += dist[curNodeIndex][nextNodeIndex];// 获取到达下一个点的时间
				curNodeIndex = nextNodeIndex;
			}
			// 最后一个 o2o 点，没有访问过，并且超时了
			if (!city.visited[curNodeIndex]
					&& exceedDelivryTime(curTime, curNodeIndex))
				return true;
		} else {// 没有进入 o2o，故只需判断是否在收件时超时即可
			if (curNodeIndex < o2oStartIndex) {// 没有到达 o2o 点
				curTime += dist[curNodeIndex][o2oStartIndex];// 可以假设刚好到
			}
			ResultOrder resultOrder = list.get(o2oStartIndex - 1);
			if (curTime > resultOrder.Arrival_time)
				return true;
		}
		return false;
	}

	/**
	 * kamyang Sep 5, 2016
	 * 
	 * @param curTime
	 * @param curNodeIndex
	 * @return
	 */
	private boolean exceedDelivryTime(int curTime, int curNodeIndex) {
		// 此处为 o2o 派件点，要进行判断是否超出派件时间
		String order_Id = list.get(curNodeIndex - 1).Order_id;
		Order order = ServiceData.OrderPackageMaps.get(order_Id);
		int serviceTime = Rule.dealTime(order);
		// 超时了
		if (curTime + serviceTime > order.delivery_time)
			return true;
		return false;
	}

	/**
	 * 判断是否出现违规路径，即 o2o 派送点出现在收件前<BR>
	 * kamyang Sep 5, 2016
	 * 
	 * @param city
	 * @return
	 */
	private boolean isIllegal(City city) {
		error = "isIllegal";
		for (int i = 1; i <= city.pos; i++) {
			int nodeIndex = city.route[i];
			if (nodeIndex == this.o2oStartIndex) {// 当前为 o2o 起始点
				nodeIndex = city.route[i + 1];
				// 下一个点只能是下一个 o2o 点或者是没有访问
				if (nodeIndex == this.o2oStartIndex + 1 || nodeIndex == 0)
					return false;
				else
					return true;
			} else if (nodeIndex > this.o2oStartIndex) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断是否超重<BR>
	 * kamyang Sep 5, 2016
	 * 
	 * @param route
	 * @param o2oIndex
	 * @return 超重，返回 true，否则返回 false
	 */
	private boolean exceedWeigth(City city) {
		error = "exceedWeigth";
		int weight = depotWeight;// 初始为 depot 的重量，然后进行派送，不能超过140，第一个 o2o
									// 点的重量已经放上去了
		for (int i = 2; i <= city.pos; i++) {
			int nodeIndex = city.route[i];
			if (nodeIndex == this.o2oStartIndex) {// o2o 起始点
				weight += o2oWeight;
				if (weight > 140) // 派送包裹量最大的地方，如果没用超重，则后面都不会超重
					return true;
				else
					return false;
			}
			weight += list.get(nodeIndex - 1).Amount;// 派送
		}
		return false;
	}

	/**
	 * 改city为预先准备好要走的路径
	 * 
	 * @param city
	 * @returns
	 */
	private double getLb(City city) {
		double lb = 0;
		// 第一个默认就是有数据的
		int i;
		for (i = 1; i <= city.pos; i++) {
			int left = city.route[i - 1];
			int cur = city.route[i];// 第一个为开始节点
			int right = city.route[i + 1];// 第二个规划的节点
			if (i == n)
				right = city.route[1];// 构成一个环形
			if (left == 0 && right == 0)// 左右等于0的时候即只有一个元素的时候
				break;
			lb += getLength(left, cur, right);
		}
		for (i = 1; i <= n; i++) {
			if (city.visited[i] && city.pos != 1)// 已经规划过路径，不止一个
				continue;
			lb += getLength(i);
		}
		lb = Math.ceil(lb / 2);// 向上取整，故如果余数存在1，则必定要取整
		return lb;
	}

	private int getLength(int left, int cur, int right) {
		int lb = 0;
		if (left != 0 && right != 0) {
			lb += dist[left][cur];
			lb += dist[cur][right];
		} else if (left == 0) {
			lb += getMinExj(cur, right);
			lb += dist[cur][right];
		} else {
			lb += getMinExj(cur, left);
			lb += dist[left][cur];
		}
		return lb;
	}

	private double getMinExj(int cur, int j) {
		double min = Double.MAX_VALUE;
		for (int i = 1; i <= n; i++) {
			if (i == j)
				continue;
			min = Math.min(min, dist[cur][i]);
		}
		return min;
	}

	private double getLength(int cur) {
		double lb = 0;
		// 定义一个最大堆
		PriorityQueue<Double> maxHeap = new PriorityQueue<>(2,
				new Comparator<Double>() {

					@Override
					public int compare(Double o1, Double o2) {
						if (o1 > o2)
							return -1;
						if (o1 < o2)
							return 1;
						return 0;
					}
				});

		maxHeap.add(dist[cur][1]);
		maxHeap.add(dist[cur][2]);
		for (int j = 3; j <= n; j++) {
			// 保持两个，并且是最小的两个
			if (maxHeap.peek() > dist[cur][j]) {
				maxHeap.poll();
				maxHeap.add(dist[cur][j]);
			}
		}
		lb += maxHeap.poll();
		lb += maxHeap.poll();
		return lb;
	}

	public class City {
		public double lb;
		int pos;// 记录当前走的位置
		int st = 1;// 第一个作为开始
		boolean visited[];
		public int route[];
		int o2oStartIndex = 0; // 记录 o2o 在该路径上的起始时间

		@Override
		public String toString() {
			return "City [lb=" + lb + ", route=" + Arrays.toString(route) + "]";
		}
	}

	/**
	 * 获取两订单的距离，这里的距离包括服务第一个订单的时间<Br>
	 * kamyang Sep 5, 2016
	 * 
	 * @param resultOrder
	 *            第一个订单
	 * @param resultOrder2
	 *            第二个订单
	 * @return
	 */
	private double getDist(ResultOrder resultOrder, ResultOrder resultOrder2) {
		Node node = ServiceData.localPacageMaps.get(resultOrder.Addr);
		Node node2 = ServiceData.localPacageMaps.get(resultOrder2.Addr);
		int dist = Rule.distanceTime(node, node2);
		int service = Rule.dealTime(resultOrder);
		return (dist + service);
	}

	/**
	 * kamyang Sep 4, 2016
	 * 
	 * @param depotOrders 静态订单集合
	 * @param o2oResultOrders o2o订单集合
	 * @param resultOrder o2o出发调度
	 * @return 将所有点单生成调度，静态在前，o2o在后
	 */
	private List<ResultOrder> getResultOrder(List<Order> depotOrders,
			List<ResultOrder> o2oResultOrders, ResultOrder resultOrder) {
		List<ResultOrder> list = new ArrayList<>();
		// ResultOrder resultOrder = Utils.createResultOrder(depotOrders.get(0),
		// true);
		list.add(resultOrder);
		depotWeight = 0;
		for (Order order : depotOrders) {//遍历所有静态订单
			depotWeight += order.num;//计算总重量
			resultOrder = Utils.createResultOrder(order, false);//创建静态订单调度
			list.add(resultOrder);
		}
		int o2osize = o2oResultOrders.size();
		o2oWeight = 0;
		for (int i = 0; i < o2osize / 2; i++) {//o2o有收发，所以要除2，计算总重
			resultOrder = o2oResultOrders.get(i);
			o2oWeight += resultOrder.Amount;
		}
		resultOrder = o2oResultOrders.get(0).clone();
		list.add(resultOrder);// o2o起始位置
		o2oStartIndex = list.size(); // o2o 起始位置
		punish = Rule.calFitting(o2oResultOrders, resultOrder.clone(),
				resultOrder.Arrival_time);//计算积分
		punish -= (o2oResultOrders.get(o2osize - 1).Departure - resultOrder.Arrival_time);//最后的到达时间减去出发时间
		if (punish > 0) {// 则两个就是完整的 o2o 点了
			resultOrder = o2oResultOrders.get(1).clone();
			list.add(resultOrder);
		} else {// 包含两个 o2o 收货点，其中可以获取仓库的起始收货时间和收完货后的离开时间
			for (int i = o2osize / 2 - 1; i < o2oResultOrders.size(); i++) {
				resultOrder = o2oResultOrders.get(i).clone();
				list.add(resultOrder);
			}
		}
		return list;
	}
}
