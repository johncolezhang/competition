package com.szu.util;

import java.util.List;

import com.szu.model.SortNode;
/**
 * 建成最大顶堆
 * @author johncole
 *
 */
public class MaxHeap {
	// 堆的存储结构 - 数组
	private SortNode[] data;

	// 将一个数组传入构造方法，并转换成一个小根堆
	public MaxHeap(SortNode[] data) {
		this.data = data;
		buildHeap();
	}

	// 将数组转换成最小堆
	private void buildHeap() {
		// 完全二叉树只有数组下标小于或等于 (data.length) / 2 - 1 的元素有孩子结点，遍历这些结点。
		// *比如上面的图中，数组有10个元素， (data.length) / 2 - 1的值为4，a[4]有孩子结点，但a[5]没有*
		for (int i = (data.length) / 2 - 1; i >= 0; i--) {
			// 对有孩子结点的元素heapify
			heapify(i);
		}
	}

	/**
	 * 父节点最大，下层小于上层，且左大于右
	 * @param i
	 */
	private void heapify(int i) {
		// 获取左右结点的数组下标
		int l = left(i);//返回i结点的做孩子结点
		int r = right(i);//返回右结点
		// 这是一个临时变量，表示 跟结点、左结点、右结点中最小的值的结点的下标
		int smallest = i;
		// 存在左结点，且左结点的值大于根结点的比价大的值
		if (l < data.length && data[l].dist > data[i].dist)
			smallest = l;
		// 存在右结点，且右结点的值大于以上比较的较大值
		if (r < data.length && data[r].dist > data[smallest].dist)
			smallest = r;
		// 左右结点的值都大于根节点，直接return，不做任何操作
		if (i == smallest)
			return;
		// 交换根节点和左右结点中最小的那个值，把根节点的值替换下去
		swap(i, smallest);
		// 由于替换后左右子树会被影响，所以要对受影响的子树再进行heapify
		heapify(smallest);
	}

	// 获取右结点的数组下标
	private int right(int i) {
		return (i + 1) << 1;
	}//乘2加1

	// 获取左结点的数组下标
	private int left(int i) {
		return ((i + 1) << 1) - 1;
	}//乘2减1

	// 交换元素位置
	private void swap(int i, int j) {
		SortNode tmp = data[i];
		data[i] = data[j];
		data[j] = tmp;
	}

	// 获取对中的最小的元素，根元素
	public SortNode getRoot() {
		return data[0];
	}

	// 替换根元素，并重新heapify
	public void setRoot(SortNode root) {
		data[0] = root;
		heapify(0);
	}

	/**
	 *  从data数组中获取最小的k个数
	 * @param data
	 * @param k
	 * @return
	 */
	public static SortNode[] topK(List<SortNode> data, int k) {
		// 先取K个元素放入一个数组topk中
		SortNode[] topk = new SortNode[k];
		for (int i = 0; i < k; i++) {
			topk[i] = data.get(i);
		}

		// 转换成最小堆
		MaxHeap heap = new MaxHeap(topk);

		// 从k开始，遍历data
		for (int i = k; i < data.size(); i++) {
			SortNode root = heap.getRoot();
			// 当数据大于堆中最小的数（根节点）时，替换堆中的根节点，再转换成堆
			if (data.get(i).dist < root.dist) {
				heap.setRoot(data.get(i));
			}
		}
		return topk;
	}
}
