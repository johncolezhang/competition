package com.szu.algorithm;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
//no need
public class Data implements Distance<Node> {
	List<Node> data = new ArrayList<>();

	public Data() {
		readData();
	}

	private void readData() {
		try {
			FileInputStream inputStream = new FileInputStream("res/data.txt");
			InputStreamReader inStream = new InputStreamReader(inputStream,
					"utf-8");
			BufferedReader reader = new BufferedReader(inStream);
			String str = null;
			while ((str = reader.readLine()) != null) {
				String[] strs = str.split(" ");
				Node node = new Node();
				node.x = Integer.valueOf(strs[0]);
				node.y = Integer.valueOf(strs[1]);
				data.add(node);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Node> getData() {
		return data;
	}

	@Override
	public double dist(Node node, Node node2) {
		return Math.sqrt((node.x - node2.x) * (node.x - node2.x)
				+ (node.y - node2.y) * (node.y - node2.y));
	}

}
