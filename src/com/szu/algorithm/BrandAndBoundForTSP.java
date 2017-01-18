package com.szu.algorithm;
/**
 * 静态结点生成tsp排序的方法
 */
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class BrandAndBoundForTSP {
	private int n;// 点的个数

	private final static int MAX_NUM = 35;
	private final static int INF = Integer.MAX_VALUE;
	private double[][] dist = new double[MAX_NUM][MAX_NUM];//dist是从dist[1][1]开始的

	public void init() {
		double[][] test = { { INF, 3, 1, 5, 8 }, { 3, INF, 6, 7, 9 },
				{ 1, 6, INF, 4, 2 }, { 5, 7, 4, INF, 3 }, { 8, 9, 2, 3, INF } };//距离相量
		n = 5;//设点个数为5
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				dist[i][j] = test[i - 1][j - 1];
			}
		}
	}

	public <T> void init(List<T> list, Distance distance) {
		n = list.size();
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) {
					dist[i][j] = INF;
					continue;
				}
				dist[i][j] = distance.dist(list.get(i - 1), list.get(j - 1));//两节点的距离
			}
		}
	}//求点到点之间的距离

	/**
	 * 改city为预先准备好要走的路径
	 * 获取lb的方法，参考算法书第十章
	 * 
	 * @param city
	 * @return
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
				continue;//跳过继续执行for循环
			min = Math.min(min, dist[cur][i]);
		}
		return min;
	}

	
	public double getLength(int cur) {
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
					}//降序排序
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

	/**
	 * 获取最佳的静态结点tsp排序
	 * @return
	 */
	public City getBest() {
		City startCity = new City();
		startCity.route = new int[MAX_NUM];
		startCity.visited = new boolean[MAX_NUM];
		startCity.route[1] = startCity.st;// 初始化第一个访问的节点
		startCity.visited[1] = true;
		startCity.pos = 1;// 当前的位置
		startCity.lb = getLb(startCity);

		// // 如果是两个节点，则直接返回
		// if (n == 2) {
		// startCity.route[2] = 2;
		// return startCity;
		// }
		City bestCity = null;
		Queue<City> queue = new LinkedList<>();
		queue.add(startCity);
		while (!queue.isEmpty()) {// 没有空
			City tempCity = queue.poll();// 将当前的队列放出来
			if (tempCity.pos == n - 1) {// 如果到了最后一个节点
				for (int i = 1; i <= n; i++) {
					if (!tempCity.visited[i]) {// 将最后一个节点加到访问队列里
						tempCity.visited[i] = true;
						tempCity.pos++;// 指向下一个
						tempCity.route[tempCity.pos] = i;
						tempCity.lb = getLb(tempCity);//计算当前lb
						break;//计算最后一点，算完跳出循环
					}
				}
				if (bestCity == null || bestCity.lb > tempCity.lb)
					bestCity = tempCity;
			} else {
				double minLb = Double.MAX_VALUE;//找最短开始点的lb
				List<City> list = new LinkedList<>();
				for (int i = 1; i <= n; i++) {
					if (!tempCity.visited[i]) {// 没有访问过，要创建相应对象
						City city = new City();
						city.pos = tempCity.pos + 1;//位置加1
						city.route = Arrays.copyOf(tempCity.route, MAX_NUM);
						city.visited = Arrays.copyOf(tempCity.visited, MAX_NUM);
						city.visited[i] = true;
						city.route[city.pos] = i;
						city.lb = getLb(city);
						if (city.lb <= minLb) {
							list.add(city);
							minLb = city.lb;
						}
					}
				}
				for (City city : list) {//便历加到list中的起始点
					if (city.lb == minLb) {
						queue.add(city);// 最短的添加到队列里
					}
				}
			}
		}
		return bestCity;
	}

	public class City {
		public double lb;
		int pos;// 记录当前走的位置
		int st = 1;// 第一个作为开始
		boolean visited[];
		public int route[];

		@Override
		public String toString() {
			return "City [lb=" + lb + ", route=" + Arrays.toString(route) + "]";
		}
	}

	public static void main(String[] args) {
		BrandAndBoundForTSP boundForTSP = new BrandAndBoundForTSP();
		// boundForTSP.init();
		Data data = new Data();
		boundForTSP.init(data.getData(), data);
		double time = System.currentTimeMillis();
		City city = boundForTSP.getBest();
		System.out.println(city.toString());
		System.out.println("耗费时间：" + (System.currentTimeMillis() - time));
	}
}