import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.szu.algorithm.DynamicVehicle;
import com.szu.algorithm.MergeOrder;
import com.szu.model.Order;
import com.szu.model.ResultOrder;
import com.szu.model.ServiceData;
import com.szu.model.SortNode;
import com.szu.util.CSVFileUtil;
import com.szu.util.Rule;

public class Main {

	public static void main(String[] args) {
		// 515767 419295 422537 753
		// 750601 632808 563916 934
		// 711312 562327
		// 148985
		 test();
		// ServiceData.loadData();
		// Node node = ServiceData.localPacageMaps.get("S103");
		// Node node2 = ServiceData.localPacageMaps.get("B6540");
		// System.out.println(Rule.distanceTime(node, node2));
		double a = Integer.MAX_VALUE;
		System.out.printf("%1.0e", a);
		// 为最小堆
		 //finalTest();
		// System.out.println(632808 - 563916);
	}

	public static void finalTest() {
		ServiceData.loadData();
		int total = 0;
		MergeOrder mergeOrder = new MergeOrder();
		List<List<ResultOrder>> results = new ArrayList<>();
		Map<String, List<List<ResultOrder>>> o2oListsMap = new HashMap<>();
		System.out.println("O2O:");
		int i = 1;
		for (Entry<String, List<Order>> entry : ServiceData.o2oOrderMaps
				.entrySet()) {
			mergeOrder.o2oNumberMap
					.put(entry.getKey(), entry.getValue().size());//该商家的o2o订单个数映射
		}
		for (Entry<String, List<Order>> entry : ServiceData.o2oOrderMaps
				.entrySet()) {
			String key = entry.getKey();//o2o仓库名称
			List<Order> o2oList = new ArrayList<>();
			System.out.println(i + " begin:");
			i++;
			o2oList.addAll(entry.getValue());//获取o2o订单
			List<List<ResultOrder>> resultOrders = mergeOrder
					.dealingO2oOrder(o2oList);//将订单处理成调度，并按时间排序好调度
			resultOrders = mergeOrder.mergeMiddleO2oOrdersWithDepot(
					resultOrders, key);//将o2o订单合并在该仓库的调度中
			o2oListsMap.put(key, resultOrders);
		}
		// 获取合并好的 o2o 点
		o2oListsMap = mergeOrder.mergePrioO2oOrdersWithDepot(o2oListsMap);

		for (Entry<String, List<Order>> entry : ServiceData.depotMaps
				.entrySet()) {
			String key = entry.getKey();
			List<Order> depotList = entry.getValue();
			List<List<ResultOrder>> resultOrders = mergeOrder
					.dealDepot(depotList);
			resultOrders = mergeOrder.mergeResultOrders(resultOrders,
					o2oListsMap.get(key));
			results.addAll(resultOrders);
		}
		int fitting = 0;
		int time = 0;
		for (List<ResultOrder> list : results) {
			ResultOrder depotOrder = list.get(0).clone();
			fitting += Rule.calFitting(list, depotOrder, 0);
			time += list.get(list.size() - 1).Departure;
		}
		// if (results.size() < 1000)
		CSVFileUtil.writeResult("result", results);
		System.out.println(fitting + "  " + time + "  " + results.size());
	}

	private static void getSortLibData() {
		ServiceData.loadData();
		List<SortNode> list = ServiceData.sortLibMap.get("B2071");
		for (SortNode sortNode : list) {
			System.out.println(sortNode.dist);
			for (String string : sortNode.staticOderNameList) {
				Order order = ServiceData.OrderPackageMaps.get(string);
				System.out.print(string + "  " + order.num + "  ");
			}
			System.out.println();
		}
	}

	public static void test() {
		ServiceData.loadData();//加载文件，建立缓存在servicedata中
		int total = 0;
		DynamicVehicle dynamicVehicle = new DynamicVehicle();
		List<List<ResultOrder>> depotLists, o2oLists, resultLists;//所有配送点的调度结果列表
		List<List<ResultOrder>> results = new ArrayList<>();
		List<ResultOrder> depots = new ArrayList<>();
		int i = 1;
		for (Entry<String, List<Order>> entry : ServiceData.depotMaps
				.entrySet()) { //遍历所有仓库
			String key = entry.getKey();//获取key值
			System.out.print(i + ".begin:" + key + "  "
					+ entry.getValue().get(0).dest_id);//开始点及结束点
			i++;
			List<Order> o2oList = new ArrayList<>();
			o2oList.addAll(ServiceData.o2oOrderMaps.get(key));//根据仓库编号获取o2o订单
			List<Order> depotsList = new ArrayList<>();
			depotsList.addAll(entry.getValue());//获取仓库

			// o2oLists = dynamicVehicle.mergeO2oOrdersWithDepot(o2oLists,
			// depotsList);
			depotLists = dynamicVehicle.dealDepot(depotsList);//根据仓库订单生产调度列表
			o2oLists = dynamicVehicle.dealingO2oOrder(o2oList);//根据仓库订单生产o2o调度列表
			//System.out.println("  depot Size:" + total);
			resultLists = dynamicVehicle
					.mergeResultOrders(depotLists, o2oLists);//合并仓库订单和O2O订单
			results.addAll(resultLists);
			ResultOrder resultOrder = createResultOrder(
					entry.getValue().get(0), true);
			for (int j = 0; j < resultLists.size(); j++) {
				depots.add(resultOrder.clone());
			}
		}
		
		
		int fitting = 0;
		int time = 0;
		int waitTime = 0;
		i = 0;
		for (List<ResultOrder> list : results) {
			ResultOrder depotOrder = depots.get(i);//获取一个仓库的调度列表
			i++;
			fitting += Rule.calFitting(list, depotOrder, 0);
			waitTime += Rule.calWaitingTime(list);
			time += list.get(list.size() - 1).Departure;//离开时间
		}
		// if (results.size() < 1000)
		CSVFileUtil.writeResult("result", results);
		System.out.println("punish:" + (fitting - time - 148985) + "  postMan:"
				+ results.size() + "  waitTime:" + waitTime + "  fitting:"
				+ fitting);//惩罚
	}

	public static Object readObject() {
		Object object = null;
		try {
			FileInputStream in = new FileInputStream("result.txt");
			ObjectInputStream inputStream = new ObjectInputStream(in);
			object = inputStream.readObject();
			inputStream.close();
			in.close();
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return object;
	}

	public static void writeObject(Object object) {

		try {
			FileOutputStream out = new FileOutputStream("result.txt");
			ObjectOutputStream outputStream = new ObjectOutputStream(out);
			outputStream.writeObject(object);
			outputStream.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ResultOrder createResultOrder(Order order, boolean isSrc) {
		ResultOrder resultOrder = new ResultOrder();
		resultOrder.Courier_id = "";
		if (isSrc) {
			resultOrder.Addr = order.src_id;
			resultOrder.Amount = order.num;
		} else {
			resultOrder.Addr = order.dest_id;
			resultOrder.Amount = -order.num;
		}
		resultOrder.Order_id = order.order_id;
		return resultOrder;
	}//创建调度结果
}
