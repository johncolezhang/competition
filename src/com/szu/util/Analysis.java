package com.szu.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.szu.algorithm.DynamicVehicle;
import com.szu.model.Order;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;
import com.szu.model.SortNode;

public class Analysis {
	/**
	 * 282193 280451 283596 124875 minPickup:210 maxPickup:629 minDelivery:300
	 * maxDelivery:719 125853 62848
	 */
/**
 * 分支界限法
 * @param args
 */
	public static void main(String[] args) {
		ServiceData.loadData();
		DynamicVehicle dynamicVehicle = new DynamicVehicle();
		List<List<ResultOrder>> results = new ArrayList<>();
		Map<String, List<List<ResultOrder>>> o2oResultMap = new HashMap<>();
		Map<String, List<List<ResultOrder>>> o2oPrioResultMap = new HashMap<>();
		int totalSize = 0;
		int totalRemain = 0;
		int i = 1;
		System.out.println("node:" + ServiceData.localPacageMaps.size());//所有地址的信息个数9951
		System.out.println("Order:" + ServiceData.OrderPackageMaps.size());//所有订单的个数12631
		System.out.println("o2o:" + ServiceData.o2oOrders.size());//o2o订单个数3417
		int depotSize = 0;
		for (List list : ServiceData.depotMaps.values()) {//遍历所有静态仓库的订单
			depotSize += list.size();
		}
		System.out.println("depot:" + depotSize);//静态订单的个数9214
		// 创建 o2o 订单并合并中间部分
		for (Entry<String, List<Order>> entry : ServiceData.depotMaps
				.entrySet()) {
			String key = entry.getKey();
			totalSize += entry.getValue().size();
			System.out.println(i + ".begin:" + key);
			i++;
			List<Order> o2oList = new ArrayList<>();
			o2oList.addAll(ServiceData.o2oOrderMaps.get(key));//距离该仓库最近的o2o订单
			List<List<ResultOrder>> o2oLists = dynamicVehicle
					.dealingO2oOrder(o2oList);//生成段，该仓库的o2o段集合
			List<List<ResultOrder>> mergeO2oLists = MergeOrderUtils
					.mergeO2oOrderWithDepotOrders(o2oLists, key);//o2o与静态订单合并，用到分支界限
			o2oResultMap.put(key, mergeO2oLists);//每个静态点的o2o段集合
			totalRemain += entry.getValue().size();
		}
		System.out.println(totalSize + "  " + totalRemain);
		i = 1;
		// 合并 o2o 订单开始部分
		totalRemain = 0;
		totalSize = 0;
		for (Entry<String, List<Order>> entry : ServiceData.depotMaps
				.entrySet()) {
			String key = entry.getKey();
			totalSize += entry.getValue().size();
			i++;
			List<List<ResultOrder>> mergeO2oLists = o2oResultMap.get(key);
			Map<String, List<SortNode>> sortLib = ServiceData.sortLibMap;//获取排序表
			List<List<ResultOrder>> o2oLists = MergeOrderUtils.mergeO2oOrder(
					sortLib, mergeO2oLists, key);//插一些静态到o2o中
			o2oPrioResultMap.put(key, o2oLists);
			totalRemain += entry.getValue().size();
		}
		System.out.println(totalSize + "  " + totalRemain);
		totalRemain = 0;
		totalSize = 0;
		i = 1;
		for (Entry<String, List<Order>> entry : ServiceData.depotMaps
				.entrySet()) {
			String key = entry.getKey();
			totalSize += entry.getValue().size();
			List<List<ResultOrder>> depotLists = dynamicVehicle.dealDepot(entry
					.getValue());//处理静态订单
			i++;
			List<List<ResultOrder>> o2oLists = o2oPrioResultMap.get(key);
			List<List<ResultOrder>> resultLists = dynamicVehicle
					.mergeResultOrders(depotLists, o2oLists);//合并
			results.addAll(resultLists);
			totalRemain += entry.getValue().size();
		}
		System.out.println(totalSize + "  " + totalRemain);
		int fitting = 0;
		int time = 0;
		int waitTime = 0;
		int totalPrioWaitTime = 0;
		i = 0;
		for (List<ResultOrder> list : results) {
			ResultOrder depotOrder = list.get(0).clone();
			fitting += Rule.calFitting(list, depotOrder, 0);
			waitTime += Rule.calWaitingTime(list);
			time += list.get(list.size() - 1).Departure;
		}
		CSVFileUtil.writeResult("result", results);
		System.out.println("punish:" + (fitting - time - 148985) + "  postMan:"
				+ results.size() + "  waitTime:" + waitTime + "  fitting:"
				+ fitting + "  totalPrioWaitTime:" + totalPrioWaitTime);
		System.out.println(MergeOrderUtils.calTime);
		System.out.println(MergeOrderUtils.totalPrio + "   "
				+ Utils.totalIllegal);
	}
}
