package com.szu.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.szu.model.Node;
import com.szu.model.Order;
import com.szu.model.ResultOrder;

public class CSVFileUtil {
	public static final String ENCODE = "utf-8";

	public static boolean deleteResult() {
		boolean flag = false;
		String filePath = "res/";
		File dirFile = new File(filePath);
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		}
		// 删除文件夹下的所有文件(包括子目录)
		File[] files = dirFile.listFiles();
		for (File file : files) {
			if (file.getName().startsWith("result")) {
				file.delete();
				flag = true;
			}
		}
		return flag;
	}

	public static void readId(String filename, Map map) {
		try {
			FileInputStream fis = new FileInputStream(filename);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, ENCODE));
			String inString;
			boolean flag = false;
			int type = 0;
			while ((inString = reader.readLine()) != null) {
				String[] result = inString.split(",");
				if (flag) {
					Node node = new Node();
					node.name = result[0];
					node.Lng = Double.parseDouble(result[1]);
					node.Lat = Double.parseDouble(result[2]);
					node.type = type;
					map.put(result[0], node);
				} else {
					flag = true;
					if (result[0].equals("Site_id")) {
						type = Node.Site;
					} else if (result[0].equals("Spot_id")) {
						type = Node.Spot;
					} else if (result[0].equals("Shop_id")) {
						type = Node.Shop;
					}
				}
			}
			fis.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readCourier(String filename, List list) {
		try {
			FileInputStream fis = new FileInputStream(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, ENCODE));
			String inString;
			boolean flag = false;
			int type = 0;
			while ((inString = reader.readLine()) != null) {
				if (flag) {
					list.add(inString);
				} else {
					// System.out.println(inString);
					flag = true;
				}
			}
			fis.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readOrder(String filename, Map map) {
		try {
			FileInputStream fis = new FileInputStream(filename);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, ENCODE));
			String inString;
			boolean flag = false;
			int type = 0;
			while ((inString = reader.readLine()) != null) {
				String[] result = inString.split(",");
				if (flag) {
					Order order = new Order();
					order.order_id = result[0];
					order.dest_id = result[1];
					order.src_id = result[2];
					if (result.length == 4) {// 普通包裹,4项
						order.num = Integer.parseInt(result[3]);
					} else {//O2O包裹
						order.pickup_time = ConvertTime.String2Int(result[3]);
						order.delivery_time = ConvertTime.String2Int(result[4]);
						order.num = Integer.parseInt(result[5]);
					}
					map.put(result[0], order);
				} else {
					// System.out.println(inString);
					flag = true;
				}
			}
			fis.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readO2OOrder(String filename, List list) {
		try {
			FileInputStream fis = new FileInputStream(filename);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, ENCODE));
			String inString;
			boolean flag = false;
			int type = 0;
			while ((inString = reader.readLine()) != null) {
				String[] result = inString.split(",");
				if (flag) {
					Order order = new Order();
					order.order_id = result[0];
					order.dest_id = result[1];
					order.src_id = result[2];
					order.pickup_time = ConvertTime.String2Int(result[3]);
					order.delivery_time = ConvertTime.String2Int(result[4]);
					order.num = Integer.parseInt(result[5]);
					list.add(order);
				} else {
					// System.out.println(inString);
					flag = true;
				}
			}
			fis.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readDepot(String filename, Map<String, List<Order>> map) {
		try {
			FileInputStream fis = new FileInputStream(filename);

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, ENCODE));
			String inString;
			boolean flag = false;
			int type = 0;
			while ((inString = reader.readLine()) != null) {
				String[] result = inString.split(",");
				if (flag) {
					Order order = new Order();
					order.order_id = result[0];
					order.dest_id = result[1];//送达点
					order.src_id = result[2];//出发点，A为网点普通，S为商户o2o
					order.num = Integer.parseInt(result[3]);
					if (map.containsKey(order.src_id))
						map.get(order.src_id).add(order);
					else {
						List<Order> list = new ArrayList<>();
						list.add(order);
						map.put(order.src_id, list);
					}
				} else {
					// System.out.println(inString);
					flag = true;
				}
			}
			fis.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void readResult(String filename, Map map) {
		try {
			FileInputStream fis = new FileInputStream(filename);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					fis, ENCODE));
			String inString;
			List<ResultOrder> list = new ArrayList<>();
			String courier_id = "", cur_courier_id = null;
			while ((inString = reader.readLine()) != null) {
				String[] result = inString.split(",");
				if (!courier_id.equals(result[0])) {
					courier_id = result[0];
					// 第一次初始化
					if (cur_courier_id == null) {
						cur_courier_id = courier_id;
					} else {
						map.put(cur_courier_id, list);
						cur_courier_id = courier_id;
						list = new ArrayList<>();
					}
				}
				ResultOrder resultOrder = new ResultOrder();
				resultOrder.Courier_id = result[0];
				resultOrder.Addr = result[1];
				resultOrder.Arrival_time = Integer.parseInt(result[2]);
				resultOrder.Departure = Integer.parseInt(result[3]);
				resultOrder.Amount = Integer.parseInt(result[4]);
				resultOrder.Order_id = result[5];
				list.add(resultOrder);
			}
			map.put(cur_courier_id, list);
			fis.close();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void writeResult(String name, Map map) {
		// Courier_id
		// Addr
		// Arrival_time
		// Departure
		// Amount
		// Order_id
		String filename = "res/" + name + ".csv";
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					fos, ENCODE));
			List<Map.Entry<String, List<ResultOrder>>> listmaps = sort(map);
			StringBuffer sb = new StringBuffer();
			for (Entry<String, List<ResultOrder>> entry : listmaps) {
				List<ResultOrder> lists = entry.getValue();
				for (ResultOrder resultOrder : lists) {
					sb.setLength(0);// 清空内容
					sb.append(resultOrder.Courier_id);
					sb.append(',');
					sb.append(resultOrder.Addr);
					sb.append(',');
					sb.append(resultOrder.Arrival_time);
					sb.append(',');
					sb.append(resultOrder.Departure);
					sb.append(',');
					sb.append(resultOrder.Amount);
					sb.append(',');
					sb.append(resultOrder.Order_id);
					sb.append('\r');
					writer.write(sb.toString());
				}
			}
			writer.flush();// 必须刷新，不然数据不会写进去
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void writeResult(String name, List<List<ResultOrder>> lists) {
		// Courier_id
		// Addr
		// Arrival_time
		// Departure
		// Amount
		// Order_id
		String filename = "res/" + name + ".csv";
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					fos, ENCODE));
			StringBuffer sb = new StringBuffer();
			int totalLine = 0, o2oLine = 0, depotLine = 0;
			int totalWait = 0, longWait = 0;
			int firstWait = 0;
			int firstLongWait = 0;
			for (int i = 0; i < lists.size(); i++) {
				List<ResultOrder> list = lists.get(i);
				boolean isFirst = true;
				for (ResultOrder resultOrder : list) {
					sb.setLength(0);// 清空内容
					sb.append(String.format("%s%04d", "D", i + 1));// ServiceData.courierLists.get(i));
					sb.append(',');
					sb.append(resultOrder.Addr);
					sb.append(',');
					sb.append(resultOrder.Arrival_time);
					sb.append(',');
					sb.append(resultOrder.Departure);
					sb.append(',');
					sb.append(resultOrder.Amount);
					sb.append(',');
					sb.append(resultOrder.Order_id);
					if (resultOrder.Amount > 0
							&& resultOrder.Departure != resultOrder.Arrival_time) {
						int wait = resultOrder.Departure
								- resultOrder.Arrival_time;
						totalWait += wait;
						if (wait > 50) {
							longWait += wait;
							// sb.append(',');
							// sb.append(wait);
						}
						if (isFirst) {
							firstWait += wait;
							// sb.append(",YES");
							if (wait > 50)
								firstLongWait += wait;
							isFirst = false;
						}
					}
					sb.append('\r');
					writer.write(sb.toString());
					totalLine++;
					if (resultOrder.Addr.charAt(0) == 'S')
						o2oLine++;
					else if (resultOrder.Addr.charAt(0) == 'A')
						depotLine++;

				}
			}
			writer.flush();// 必须刷新，不然数据不会写进去
			fos.close();
			System.out.println("总相差行数:" + (25262 - totalLine) + " 电商相差行数:"
					+ (9214 - depotLine) + " o2o 相差行数:" + (3417 - o2oLine)
					+ " 等待时间：" + totalWait + "  较长的等待时间：" + longWait
					+ " 第一次等待时间总和：" + firstWait + " 第一次长等待" + firstLongWait);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static List<Map.Entry<String, List<ResultOrder>>> sort(Map map) {
		List<Map.Entry<String, List<ResultOrder>>> list = new ArrayList<>(
				map.entrySet());
		Collections.sort(list,
				new Comparator<Map.Entry<String, List<ResultOrder>>>() {
					// 降序排序
					@Override
					public int compare(Entry<String, List<ResultOrder>> o1,
							Entry<String, List<ResultOrder>> o2) {
						// return o1.getValue().compareTo(o2.getValue());// 按值升序
						// return o2.getValue().compareTo(o1.getValue()); //
						// 按值降序
						return o1.getKey().compareTo(o2.getKey());// 按键升序
					}
				});
		return list;
	}

	public static void writeObject(List<? extends Object> lists) {
		String filename = "res/result.txt";
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					fos, ENCODE));
			for (Object object : lists) {
				writer.append(object.toString());
				writer.append('\n');
			}
			writer.flush();// 必须刷新，不然数据不会写进去
			fos.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
}