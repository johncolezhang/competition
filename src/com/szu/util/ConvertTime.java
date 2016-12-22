package com.szu.util;

public class ConvertTime {
	public static int String2Int(String time) {//把当前时间转换为分钟
		String[] result = time.split(":");
		int startTime = 8;
		int total = (Integer.parseInt(result[0]) - startTime) * 60
				+ Integer.parseInt(result[1]);
		return total;
	}
}
