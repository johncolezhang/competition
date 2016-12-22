package com.szu.util;

import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

import com.szu.model.Node;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;

public class CopyOfBranchAndBoundForTSP {

	private final static int INF = Integer.MAX_VALUE;
	/**
	 * n*n的一个矩阵
	 */
	private int n;
	private int mp[][] = new int[22][22];// 最少3个点，最多15个点

	/**
	 * 输入距离矩阵
	 */
	public void in(List<ResultOrder> list) {
		n = list.size();
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				if (i == j) {
					mp[i][j] = INF;
					continue;
				}
				Node node = ServiceData.localPacageMaps.get(list.get(i - 1));
				Node node2 = ServiceData.localPacageMaps.get(list.get(j - 1));
				mp[i][j] = Rule.distanceTime(node, node2);//计算每个点之间的距离存到mp中
			}
		}
	}

	public void in() {
		n = 5;
		int[][] test = { { INF, 3, 1, 5, 8 }, { 3, INF, 6, 7, 9 },
				{ 1, 6, INF, 4, 2 }, { 5, 7, 4, INF, 3 }, { 8, 9, 2, 3, INF } };
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++) {
				// if (i == j) {
				// mp[i][j] = INF;
				// continue;
				// }
				mp[i][j] = test[i - 1][j - 1];
			}
		}
		System.out.println("开始输出");
		for (int i = 1; i <= n; i++) {
			for (int j = 1; j <= n; j++)
				System.out.print(mp[i][j] + "  ");
			System.out.println("换行");
		}
		System.out.println("输出结束");
	}

	class node implements Comparable<node> {
		int visp[] = new int[22];// 标记哪些点走了
		int st;// 起点
		int st_p;// 起点的邻接点
		int ed;// 终点
		int ed_p;// 终点的邻接点
		int k;// 走过的点数
		int sumv;// 经过路径的距离
		int lb;// 目标函数的值

		// boolean operator <(const node &p )const
		// {
		// return lb>p.lb;
		// }
		
		public int compareTo(node o) {
			if (this.lb < o.lb)
				return -1;
			if (this.lb > o.lb)//降序排序
				return 1;
			return 0;
		}
	};

	private PriorityQueue<node> q = new PriorityQueue<>();
	private int low, up;
	private int[] inq = new int[22];

	// 确定上界
	private int dfs(int u, int k, int l) {
		if (k == n)
			return l + mp[u][1];//第一列
		int minlen = INF, p = 1;
		for (int i = 1; i <= n; i++) {
			if (inq[i] == 0 && minlen > mp[u][i])/* 取与所有点的连边中最小的边 */
			{
				minlen = mp[u][i];
				p = i;
			}
		}
		inq[p] = 1;
		return dfs(p, k + 1, l + minlen);
	}

	private int get_lb(node p) {
		int ret = p.sumv * 2;// 路径上的点的距离
		int min1 = INF, min2 = INF;// 起点和终点连出来的边
		for (int i = 1; i <= n; i++) {
			if (p.visp[i] == 0 && min1 > mp[i][p.st]) {
				min1 = mp[i][p.st];
			}
		}
		ret += min1;
		for (int i = 1; i <= n; i++) {
			if (p.visp[i] == 0 && min2 > mp[p.ed][i]) {
				min2 = mp[p.ed][i];
			}
		}
		ret += min2;
		for (int i = 1; i <= n; i++) {
			if (p.visp[i] == 0) {
				min1 = min2 = INF;
				for (int j = 1; j <= n; j++) {
					if (min1 > mp[i][j])
						min1 = mp[i][j];
				}
				for (int j = 1; j <= n; j++) {
					if (min2 > mp[j][i])
						min2 = mp[j][i];
				}
				ret += min1 + min2;
			}
		}
		return ret % 2 == 0 ? (ret / 2) : (ret / 2 + 1);
	}

	private void get_up() {
		inq[1] = 1;
		up = dfs(1, 1, 0);
	}

	private void get_low() {
		low = 0;
		for (int i = 1; i <= n; i++) {
			/* 通过排序求两个最小值 */
			int min1 = INF, min2 = INF;
			int[] tmpA = new int[22];
			for (int j = 1; j <= n; j++) {
				tmpA[j] = mp[i][j];
			}
			Arrays.sort(tmpA, 1, n + 1);// 对临时的数组进行排序
			low += tmpA[1];
			// low += tmpA[2];
		}
		// low /= 2;
	}

	public int solve() {
		/* 贪心法确定上界 */
		get_up();

		/* 取每行最小的边之和作为下界 */
		get_low();
		/* 设置初始点,默认从1开始 */
		node star = new node();
		star.st = 1;
		star.ed = 1;
		star.k = 1;
		for (int i = 1; i <= n; i++)
			star.visp[i] = 0;
		star.visp[1] = 1;
		star.sumv = 0;
		star.lb = low;

		/* ret为问题的解 */
		int ret = INF;
		q.add(star);
		while (!q.isEmpty()) {
			node tmp = q.poll();
			if (tmp.k == n - 1) {
				/* 找最后一个没有走的点 */
				int p = 1;
				for (int i = 1; i <= n; i++) {
					// 找到第一个等于0的节点
					if (tmp.visp[i] == 0) {
						p = i;
						break;
					}
				}
				int ans = tmp.sumv + mp[p][tmp.st] + mp[tmp.ed][p];
				node judge = q.peek();

				/* 如果当前的路径和比所有的目标函数值都小则跳出 */
				if (ans <= judge.lb) {
					ret = Math.min(ans, ret);
					System.out.println(ret);
					break;
				}
				/* 否则继续求其他可能的路径和，并更新上界 */
				else {
					up = Math.min(up, ans);
					ret = Math.min(ret, ans);
					continue;
				}
			}
			System.out.println(tmp.k + "  " + tmp.lb);
			/* 当前点可以向下扩展的点入优先级队列 */
			node next = new node();
			for (int i = 1; i <= n; i++) {
				if (tmp.visp[i] == 0) {
					next.st = tmp.st;
					/* 更新路径和 */
					next.sumv = tmp.sumv + mp[tmp.ed][i];
					/* 更新最后一个点 */
					next.ed = i;
					/* 更新顶点数 */
					next.k = tmp.k + 1;
					/* 更新经过的顶点 */
					for (int j = 1; j <= n; j++)
						next.visp[j] = tmp.visp[j];
					next.visp[i] = 1;
					/* 求目标函数 */
					next.lb = get_lb(next);
					/* 如果大于上界就不加入队列 */
					if (next.lb > up)
						continue;
					q.add(next);
				}
			}
		}
		return ret;
	}

	public static void main(String[] args) {
		CopyOfBranchAndBoundForTSP boundForTSP = new CopyOfBranchAndBoundForTSP();
		boundForTSP.in();
		System.out.printf("%d    %d\n", Integer.MAX_VALUE, boundForTSP.solve());
	}
}
