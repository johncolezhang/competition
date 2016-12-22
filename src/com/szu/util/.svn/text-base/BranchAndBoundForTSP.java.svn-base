package com.szu.util;

public class BranchAndBoundForTSP {
	private final static int NoEdge = Integer.MAX_VALUE;

	class MinHeapNode {
		int lcost; // 子树费用的下界
		int cc; // 当前费用
		int rcost; // x[s:n-1]中顶点最小出边费用和
		int s; // 根节点到当前节点的路径为x[0:s]
		int[] x; // 需要进一步搜索的顶点是//x[s+1:n-1]
		MinHeapNode next;
	};

	public int n; // 图G的顶点数
	int[][] a; // 图G的邻接矩阵
	// int NoEdge; //图G的无边标记
	int cc; // 当前费用
	int bestc; // 当前最小费用
	MinHeapNode head; /* 堆头 */
	MinHeapNode lq; /* 堆第一个元素 */
	MinHeapNode fq; /* 堆最后一个元素 */

	int DeleteMin(MinHeapNode E) {
		MinHeapNode tmp;
		tmp = fq;
		// w = fq.weight ;
		E = fq;
		if (E == null)
			return 0;
		head.next = fq.next; /* 一定不能丢了链表头 */
		fq = fq.next;
		// free(tmp) ;
		return 0;
	}

	int Insert(MinHeapNode hn) {
		if (head.next == null) {
			head.next = hn; // 将元素放入链表中
			fq = lq = head.next; // 一定要使元素放到链中
		} else {
			MinHeapNode tmp;
			tmp = fq;
			if (tmp.cc > hn.cc) {
				hn.next = tmp;
				head.next = hn;
				fq = head.next; /* 链表只有一个元素的情况 */
			} else {
				for (; tmp != null;) {
					if (tmp.next != null && tmp.cc > hn.cc) {
						hn.next = tmp.next;
						tmp.next = hn;
						break;
					}
					tmp = tmp.next;
				}
			}
			if (tmp == null) {
				lq.next = hn;
				lq = lq.next;
			}
		}
		return 0;
	}

	int BBTSP(int v[]) {// 解旅行售货员问题的优先队列式分支限界法

		/* 初始化最优队列的头结点 */
		// int lcost; // 子树费用的下界
		// int cc; // 当前费用
		// int rcost; // x[s:n-1]中顶点最小出边费用和
		// int s; // 根节点到当前节点的路径为x[0:s]
		// int[] x = new int[22]; // 需要进一步搜索的顶点是//x[s+1:n-1]
		// MinHeapNode next;
		head = new MinHeapNode();
		head.cc = 0;
		head.x = null; // 数组
		head.lcost = 0;
		head.next = null;
		head.rcost = 0;
		head.s = 0;
		int[] MinOut = new int[n + 1]; /* 定义定点i的最小出边费用 */
		// 计算MinOut[i]=顶点i的最小出边费用
		int MinSum = 0;// 最小出边费用总合
		for (int i = 1; i <= n; i++) {
			int Min = NoEdge; /* 定义当前最小值 */
			for (int j = 1; j <= n; j++)
				if (a[i][j] != NoEdge && /* 当定点i,j之间存在回路时 */
				(a[i][j] < Min || Min == NoEdge)) /* 当顶点i,j之间的距离小于Min */
					Min = a[i][j]; /* 更新当前最小值 */
			if (Min == NoEdge)
				return NoEdge;// 无回路
			MinOut[i] = Min;
			// printf("%d\n",MinOut[i]);/*顶点i的最小出边费用*/
			MinSum += Min;
			// printf("%d\n",MinSum); /*最小出边费用的总和*/
		}

		MinHeapNode E;
		E = new MinHeapNode();
		E.x = new int[n];
		// E.x=new int[n];
		for (int i = 0; i < n; i++)
			E.x[i] = i + 1;
		E.s = 0;
		E.cc = 0;
		E.rcost = MinSum;
		E.next = null; // 初始化当前扩展节点
		int bestc = NoEdge; /* 记录当前最小值 */
		// 搜索排列空间树
		while (E.s < n - 1) {// 非叶结点
			if (E.s == n - 2) {// 当前扩展结点是叶结点的父结点
				/*
				 * 首先考虑s=n-2的情形，此时当前扩展结点是排列树中某个叶结点的父结点。如果该叶结点相应一条可行回路
				 * 且费用小于当前最小费用，则将该叶结点插入到优先队列中，否则舍去该叶结点
				 */
				if (a[E.x[n - 2]][E.x[n - 1]] != NoEdge && /* 当前要扩展和叶节点有边存在 */
				a[E.x[n - 1]][1] != NoEdge && /* 当前页节点有回路 */
				(E.cc + a[E.x[n - 2]][E.x[n - 1]] + a[E.x[n - 1]][1] < bestc /* 该节点相应费用小于最小费用 */
				|| bestc == NoEdge)) {
					bestc = E.cc + a[E.x[n - 2]][E.x[n - 1]] + a[E.x[n - 1]][1]; /* 更新当前最新费用 */
					E.cc = bestc;
					E.lcost = bestc;
					E.s++;
					E.next = null;
					Insert(E); /* 将该页节点插入到优先队列中 */
				} else
					E.x = null;// 该页节点不满足条件舍弃扩展结点
			} else {/*
					 * 产生当前扩展结点的儿子结点
					 * 当s<n-2时，算法依次产生当前扩展结点的所有儿子结点。由于当前扩展结点所相应的路径是x[0:s]，
					 * 其可行儿子结点是从剩余顶点x
					 * [s+1:n-1]中选取的顶点x[i]，且(x[s]，x[i])是所给有向图G中的一条边。
					 * 对于当前扩展结点的每一个可行儿子结点，计算出其前缀(x[0:s]，x[i])的费用cc和相应的下界lcost。
					 * 当lcost<bestc时，将这个可行儿子结点插入到活结点优先队列中。
					 */
				for (int i = E.s + 1; i < n; i++)
					if (a[E.x[E.s]][E.x[i]] != NoEdge) { /* 当前扩展节点到其他节点有边存在 */
						// 可行儿子结点
						int cc = E.cc + a[E.x[E.s]][E.x[i]]; /* 加上节点i后当前节点路径 */
						int rcost = E.rcost - MinOut[E.x[E.s]]; /* 剩余节点的和 */
						int b = cc + rcost; // 下界
						if (b < bestc || bestc == NoEdge) {// 子树可能含最优解,结点插入最小堆
							MinHeapNode N = new MinHeapNode();
							N.x = new int[n];
							for (int j = 0; j < n; j++)
								N.x[j] = E.x[j];
							N.x[E.s + 1] = E.x[i];
							N.x[i] = E.x[E.s + 1];/* 添加当前路径 */
							N.cc = cc; /* 更新当前路径距离 */
							N.s = E.s + 1; /* 更新当前节点 */
							N.lcost = b; /* 更新当前下界 */
							N.rcost = rcost;
							N.next = null;
							Insert(N); /* 将这个可行儿子结点插入到活结点优先队列中 */
						}
					}
				E.x = null;
			}// 完成结点扩展
			DeleteMin(E);// 取下一扩展结点
			if (E == null)
				break; // 堆已空
		}
		if (bestc == NoEdge)
			return NoEdge;// 无回路
		for (int i = 0; i < n; i++)
			v[i + 1] = E.x[i];// 将最优解复制到v[1:n]
		while (true) {// 释放最小堆中所有结点
			DeleteMin(E);
			if (E == null)
				break;
		}
		return bestc;
	}

	public static void main(String[] args) {
		BranchAndBoundForTSP boundForTSP = new BranchAndBoundForTSP();
		boundForTSP.n = 0;
		int i = 0;
		// FILE *in, *out;
		// in = fopen("input.txt", "r");
		// out = fopen("output.txt", "w");
		// if(in == NULL || out == NULL)
		// {
		// printf("没有输入输出文件\n");
		// return 1;
		// }
		// fscanf(in, "%d", &n);
		boundForTSP.n = 5;
		boundForTSP.a = new int[boundForTSP.n + 1][boundForTSP.n + 1];

		// for(i = 1; i <= n; i++)
		// for(int j = 1; j <= n; j++)
		// //fscanf(in, "%d", &a[i][j]);
		// a[i][j]=1;
		boundForTSP.a[1][1] = 0;
		boundForTSP.a[1][2] = 5;
		boundForTSP.a[1][3] = 8;
		boundForTSP.a[1][4] = 5;
		boundForTSP.a[1][5] = 4;

		boundForTSP.a[2][1] = 5;
		boundForTSP.a[2][2] = 0;
		boundForTSP.a[2][3] = 5;
		boundForTSP.a[2][4] = 6;
		boundForTSP.a[2][5] = 3;

		boundForTSP.a[3][1] = 8;
		boundForTSP.a[3][2] = 5;
		boundForTSP.a[3][3] = 0;
		boundForTSP.a[3][4] = 5;
		boundForTSP.a[3][5] = 4;

		boundForTSP.a[4][1] = 5;
		boundForTSP.a[4][2] = 6;
		boundForTSP.a[4][3] = 6;
		boundForTSP.a[4][4] = 0;
		boundForTSP.a[4][5] = 3;

		boundForTSP.a[5][1] = 4;
		boundForTSP.a[5][2] = 3;
		boundForTSP.a[5][3] = 4;
		boundForTSP.a[5][4] = 3;
		boundForTSP.a[5][5] = 0;

		// prev = (int*)malloc(sizeof(int)*(n+1)) ;
		int[] v = new int[boundForTSP.n + 1];// MaxLoading(w , c , n) ;
		for (i = 1; i <= boundForTSP.n; i++)
			v[i] = 0;
		boundForTSP.bestc = boundForTSP.BBTSP(v);

		System.out.printf("\n");
		for (i = 1; i <= boundForTSP.n; i++)
			System.out.printf("%d\t", v[i]);
		System.out.printf("\n");
		System.out.printf("%d\n", boundForTSP.bestc);
	}
}
