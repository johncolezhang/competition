package com.szu.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.szu.util.CSVFileUtil;
import com.szu.util.Classify;
import com.szu.util.LocalSearch;
//建立缓存，将文件中的所有信息读取后用对象存储缓存
public class ServiceData {
	/**
	 * 包含所有的地点，键为地点名称，值为地点的信息对象
	 */
	public static Map<String, Node> localPacageMaps = new HashMap<>();//网点映射
	/**
	 * 包含所有的订单,键为订单 Id，值为订单信息对象
	 */
	public static Map<String, Order> OrderPackageMaps = new HashMap<>();//订单映射
	/**
	 * 仓库订单，键为仓库名称，值为在该仓库的订单信息列表
	 * 该静态仓库的所有订单
	 */
	public static Map<String, List<Order>> depotMaps = new HashMap<>();//仓库订单映射，该静态仓库的所有订单
	/**
	 * o2o 订单
	 */
	public static List<Order> o2oOrders = new ArrayList<>();//o2o订单序列
	/**
	 * 仓库订单，键为仓库名称，值为在该仓库的订单信息列表
	 */
	public static Map<String, List<Order>> o2oOrderMaps = new HashMap<>();
	/**
	 * 结果信息，键为快递员，值为结果信息
	 */
	public static Map<String, List<ResultOrder>> ResultMaps = new HashMap<>();
	// 保存键值间关系
	// 按值降序
	public static List<String> courierLists = new ArrayList<>();
	/**
	 * 排序哈希表
	 */
	public static Map<String, List<SortNode>> sortLibMap;

	public static void loadData() {
		localPacageMaps.clear();
		OrderPackageMaps.clear();
		depotMaps.clear();
		o2oOrders.clear();
		courierLists.clear();
		ResultMaps.clear();
		o2oOrderMaps.clear();
		secondTime();//建立缓存
		// firstTime();
		// CSVFileUtil.readResult("res/4838525.csv", ResultMaps);
		Classify.attach2depot(ServiceData.o2oOrderMaps);//划分o2o订单，最近原则，不考虑时间
		Classify.attachOrderToNode();
		ServiceData.sortLibMap = (Map<String, List<SortNode>>) CSVFileUtil
				.readObject();
		if (ServiceData.sortLibMap == null) {
			ServiceData.sortLibMap = LocalSearch.createSortLib();
			CSVFileUtil.writeObject(ServiceData.sortLibMap);
		}

	}

	/**
	 * kamyang 2016年8月12日
	 */
	private static void secondTime() {
		CSVFileUtil.readId("res/new_1.csv", localPacageMaps);//网点
		CSVFileUtil.readId("res/new_2.csv", localPacageMaps);//配送点
		CSVFileUtil.readId("res/new_3.csv", localPacageMaps);//商户点
		CSVFileUtil.readOrder("res/new_4.csv", OrderPackageMaps);//电商订单
		CSVFileUtil.readOrder("res/new_5.csv", OrderPackageMaps);//o2o订单
		CSVFileUtil.readDepot("res/new_4.csv", depotMaps);//出发点的所有订单
		CSVFileUtil.readO2OOrder("res/new_5.csv", o2oOrders);//o2o订单
		CSVFileUtil.readCourier("res/new_6.csv", courierLists);//快递员信息
	}

	/**
	 * kamyang 2016年8月12日
	 */
	private static void firstTime() {
		CSVFileUtil.readId("res/1.csv", localPacageMaps);
		CSVFileUtil.readId("res/2.csv", localPacageMaps);
		CSVFileUtil.readId("res/3.csv", localPacageMaps);
		CSVFileUtil.readOrder("res/4.csv", OrderPackageMaps);
		CSVFileUtil.readOrder("res/5.csv", OrderPackageMaps);
		CSVFileUtil.readDepot("res/4.csv", depotMaps);
		CSVFileUtil.readO2OOrder("res/5.csv", o2oOrders);
		CSVFileUtil.readCourier("res/6.csv", courierLists);
	}

	public static List<Map.Entry<String, Double>> sort(Map map) {
		// 将map.entrySet()转换成list
		List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(
				map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			// 降序排序
			@Override
			public int compare(Entry<String, Double> o1,
					Entry<String, Double> o2) {
				// return o1.getValue().compareTo(o2.getValue());// 按值升序
				return o2.getValue().compareTo(o1.getValue()); // 按值降序
				// return o1.getKey().compareTo(o2.getKey());// 按键升序
			}
		});
		return list;
	}

	public static void main(String[] args) {
		// for (int i = 0; i < 20; i++) {
		loadData();
		int fitting = 0;
		Schedule schedule = new Schedule();
		schedule.run();
		int totalWeight = 0, totalLine = 0;
		boolean superWeight = false;
		for (Entry<String, List<ResultOrder>> entry : ServiceData.ResultMaps
				.entrySet()) {
			for (ResultOrder resultOrder : entry.getValue()) {
				totalWeight += resultOrder.Amount;
				totalLine++;
				if (totalWeight > 140) {
					System.out.println(entry.getKey() + ":" + totalWeight);
					superWeight = true;
				}
			}
		}
		// CSVFileUtil.deleteResult();
		CSVFileUtil.writeResult("result_" + schedule.getFitting() + "_"
				+ schedule.getTime(), ResultMaps);
		System.out.println("fitting : " + schedule.getFitting() + " time : "
				+ schedule.getTime() + " isServiceAll : "
				+ (totalLine == 24686));
		System.out.println(totalLine - 24686);
		// }
	}

	// 458887
	// 543082
	/*
	 * 24974 24686 9878235 9878235 8327060 8874970 8152900 3016706 Map<String,
	 * Double> map = new HashMap<>(); for (int i = 0; i < 20; i++) {
	 * map.put(String.valueOf(i), i * 1.1); } List<Map.Entry<String, Double>>
	 * list = sort(map); for (Map.Entry<String, Double> entry : list) {
	 * System.out.println(entry.getKey() + "    " + entry.getValue()); }
	 */
}
