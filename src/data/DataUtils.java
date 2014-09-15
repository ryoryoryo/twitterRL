package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import beta.TwitterTDLeastSquares;

public class DataUtils {

	public static void main(String[] args) {
		Map<Integer, double[]> map = readStateMap(TwitterTDLeastSquares.STATE_FILE_PASS);
		for(int key : map.keySet()) {
			double[] tmp = map.get(key);
			for(int i = 0; i < tmp.length; i++) {
				System.out.println(tmp[i]);
			}
		}
	}

	/**
	 * 状態ファイル読み込み
	 *
	 * @param filepass
	 * @return
	 */
	public static Map<Integer, double[]> readStateMap(String filepass) {
		return readDoubleFile(filepass, "\t");
	}

	/**
	 * 中心点ファイル読み込み
	 *
	 * @param filepass
	 * @return
	 */
	public static double[] readCenter(String filepass) {
		return readLine(filepass);
	}

	/**
	 * 行動ファイル読み込み
	 *
	 * @param filepass
	 * @return
	 */
	public static Map<Integer, int[]> readActionMap(String filepass) {
		return readIntFile(filepass, "\t");
	}

	/**
	 * 報酬ファイル読み込み
	 *
	 * @param filepass
	 * @return
	 */
	public static Map<Integer, double[]> readRewardMap(int actionNum,
			Map<Integer, int[]> actionMap, String filepass) {
		Map<Integer, double[]> tmpMap = readDoubleFile(filepass, "\t");
		for (int time : actionMap.keySet()) {
			int[] index = actionMap.get(time);
			double[] values = tmpMap.get(time);
			double[] tmpValues = new double[actionNum];
			for (int i = 0; i < index.length; i++) {
				tmpValues[index[i]] = values[i];
			}
			tmpMap.put(time, tmpValues);
		}
		return tmpMap;
	}

	/**
	 * 1行をdoubleで読み込み
	 *
	 * @param filepass
	 * @return
	 */
	private static double[] readLine(String filepass) {
		List<Double> resultList = new ArrayList<Double>();
		try {
			File csvfile = new File(filepass);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(csvfile), "UTF-8"));
			String line = "";
			while ((line = br.readLine()) != null) {
				double value = Double.valueOf(line);
				resultList.add(value);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double[] result = new double[resultList.size()];
		for (int i = 0; i < resultList.size(); i++) {
			result[i] = resultList.get(i);
		}
		return result;
	}

	/**
	 * CSV:1列目をIntegerで2列目意向をdouble[]で取得
	 *
	 * @param filepass
	 * @param split
	 * @return
	 */
	private static Map<Integer, double[]> readDoubleFile(String filepass,
			String split) {
		Map<Integer, double[]> result = new HashMap<Integer, double[]>();
		try {
			File csvfile = new File(filepass);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(csvfile), "UTF-8"));
			String line = "";
			while ((line = br.readLine()) != null) {
				Pattern pattern = Pattern.compile(split);
				String[] strs = pattern.split(line);
				Integer time = Integer.parseInt(strs[0]);
				double[] value = new double[strs.length - 1];
				for (int i = 1; i < strs.length; i++) {
					value[i - 1] = Double.valueOf(strs[i]);
				}
				result.put(time, value);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * CSV:1列目をIntegerで2列目意向をint[]で取得
	 *
	 * @param filepass
	 * @param split
	 * @return
	 */
	private static Map<Integer, int[]> readIntFile(String filepass, String split) {
		Map<Integer, int[]> result = new HashMap<Integer, int[]>();
		try {
			File csvfile = new File(filepass);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(csvfile), "UTF-8"));
			String line = "";
			while ((line = br.readLine()) != null) {
				Pattern pattern = Pattern.compile(split);
				String[] strs = pattern.split(line);
				Integer time = Integer.parseInt(strs[0]);
				int[] value = new int[strs.length - 1];
				for (int i = 1; i < strs.length; i++) {
					value[i - 1] = Integer.valueOf(strs[i]);
				}
				result.put(time, value);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
