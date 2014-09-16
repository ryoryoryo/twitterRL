package clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * データ読み込みクラス
 *
 * @author miyagi
 *
 */
public class ClusterDataReader {

	public static void main(String[] args) {
		double[][] result = readTf(
				"R:/twitter-experiment-result/data-analysis/kingjim-2/twitter-bow/",
				"kingjim");
		for (int i = 0; i < result.length; i++) {
			System.out.print(i + "\t");
			for (int j = 0; j < result[i].length; j++) {
				System.out.print(result[i][j] + "\t");
			}
			System.out.println();
		}
	}

	// public static void main(String[] args) {
	// double[][] result = readLdaTheta(
	// "R:/twitter-experiment-result/data-analysis/kingjim-1/lda-tweet/theta.txt",
	// "kingjim");
	// for (int i = 0; i < result.length; i++) {
	// System.out.print(i + "\t");
	// for (int j = 0; j < result[i].length; j++) {
	// System.out.print(result[i][j] + "\t");
	// }
	// System.out.println();
	// }
	// }

	/**
	 * テストデータ
	 *
	 * @return
	 */
	public static double[][] readTestRawData() {
		double[][] rawData = new double[20][2];
		rawData[0] = new double[] { 65.0, 220.0, 100 };
		rawData[1] = new double[] { 73.0, 160.0, 100 };
		rawData[2] = new double[] { 59.0, 110.0, 100 };
		rawData[3] = new double[] { 61.0, 120.0, 100 };
		rawData[4] = new double[] { 75.0, 150.0, 100 };
		rawData[5] = new double[] { 67.0, 240.0, 100 };
		rawData[6] = new double[] { 68.0, 230.0, 100 };
		rawData[7] = new double[] { 70.0, 220.0, 100 };
		rawData[8] = new double[] { 62.0, 130.0, 100 };
		rawData[9] = new double[] { 66.0, 210.0, 100 };
		rawData[10] = new double[] { 77.0, 190.0, 100 };
		rawData[11] = new double[] { 75.0, 180.0, 100 };
		rawData[12] = new double[] { 74.0, 170.0, 100 };
		rawData[13] = new double[] { 70.0, 210.0, 100 };
		rawData[14] = new double[] { 61.0, 110.0, 100 };
		rawData[15] = new double[] { 58.0, 100.0, 100 };
		rawData[16] = new double[] { 66.0, 230.0, 100 };
		rawData[17] = new double[] { 59.0, 120.0, 100 };
		rawData[18] = new double[] { 68.0, 210.0, 100 };
		rawData[19] = new double[] { 61.0, 130.0, 500 };
		return rawData;
	}

	/**
	 * tfidf vector
	 *
	 * @param inputFilePass
	 * @param userName
	 * @return
	 */
	public static double[][] readTf(String inputFilePass, String userName) {
		List<File> files = readTfFiles(inputFilePass, userName);
		Set<String> wordSet = new HashSet<String>();
		List<Map<String, Double>> mapList = new ArrayList<Map<String, Double>>();
		for (File file : files) {
			Map<String, Double> map = readTfidfFile(
					file.getPath().replace(file.getName(), ""), file.getName(),
					"\t");
			mapList.add(map);
			wordSet.addAll(map.keySet());
		}
		List<String> wordList = new ArrayList<String>(wordSet);
		double[][] result = new double[mapList.size()][wordList.size()];
		for (int i = 0; i < mapList.size(); i++) {
			for (int j = 0; j < wordList.size(); j++) {
				Map<String, Double> map = mapList.get(i);
				String word = wordList.get(j);
				if (map.containsKey(word)) {
					result[i][j] = map.get(word);
				} else {
					result[i][j] = 0.0;
				}
			}
		}

		return result;
	}

	/**
	 * ファイルを読み込み、Map(String, Double)に格納して返す
	 *
	 * @param directory
	 *            ディレクトリパス
	 * @param filename
	 *            ファイルネーム
	 * @param split
	 *            1行をStringで分割する
	 * @return ファイルの中身が入ったMap
	 */
	public static Map<String, Double> readTfidfFile(String directory,
			String filename, String split) {
		// return用
		Map<String, Double> map = new HashMap<String, Double>();
		try {
			FileInputStream fis = new FileInputStream(directory + filename);
			InputStreamReader ir = new InputStreamReader(fis, "UTF-8");
			BufferedReader br = new BufferedReader(ir);

			String temp = "";
			// 読み込んで格納
			while ((temp = br.readLine()) != null) {
				String[] line = temp.split(split);
				String key = line[0];
				double value = Double.valueOf(line[1]);
				map.put(key, value);
			}

			fis.close();
			ir.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * ファイルを読み込み、Map(String, Double)に格納して返す
	 *
	 * @param directory
	 *            ディレクトリパス
	 * @param filename
	 *            ファイルネーム
	 * @param split
	 *            1行をStringで分割する
	 * @return ファイルの中身が入ったMap
	 */
	public static Map<String, Integer> readTfFile(String directory,
			String filename, String split) {
		// return用
		Map<String, Integer> map = new HashMap<String, Integer>();
		try {
			FileInputStream fis = new FileInputStream(directory + filename);
			InputStreamReader ir = new InputStreamReader(fis, "UTF-8");
			BufferedReader br = new BufferedReader(ir);

			String temp = "";
			// 読み込んで格納
			while ((temp = br.readLine()) != null) {
				String[] line = temp.split(split);
				String key = line[0];
				int value = Integer.valueOf(line[1]);
				map.put(key, value);
			}

			fis.close();
			ir.close();
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * 指定したパス以下の全てのファイルを取得するメソッド
	 *
	 * @param path
	 *            パス
	 * @return ファイルリスト
	 */
	private static List<File> readTfFiles(String inputFilePass, String userName) {
		List<File> list = new ArrayList<File>();
		File f = new File(inputFilePass);
		if (!f.isDirectory()) {
			if (f.getName().contains(userName)) {
				list.add(f);
			}
			return list;
		} else {
			for (File folder : f.listFiles()) {
				list.addAll(readTfFiles(folder.getAbsolutePath(), userName));
			}
		}
		return list;
	}

	/**
	 * LDAのtheta取得
	 *
	 * @param inputFilePass
	 * @param userName
	 * @return
	 */
	public static double[][] readLdaTheta100(String inputFilePass,
			String userName) {
		double[][] result = readLdaTheta(inputFilePass, userName);
		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result[i].length; j++) {
				result[i][j] = result[i][j] * 10000;
			}
		}
		return result;
	}

	public static double[][] readLdaTheta(String inputFilePass, String userName) {
		BufferedReader br = openBufferedReader(inputFilePass, "UTF-8");
		List<List<String>> tmpResult = new ArrayList<List<String>>();
		List<String> valueList = new ArrayList<String>();
		try {
			String line = br.readLine();
			boolean firstFlag = true;
			boolean getFlag = false;
			while (line != null) {
				if (line.contains("[doc]")) {
					if (line.contains(userName)) {
						if (firstFlag) {
							firstFlag = false;
						} else {
							tmpResult.add(valueList);
							valueList = new ArrayList<String>();
						}
						getFlag = true;
					} else {
						if (getFlag) {
							tmpResult.add(valueList);
							valueList = new ArrayList<String>();
						}
						getFlag = false;
					}
				} else if (getFlag) {
					valueList.add(line);
				}
				line = br.readLine();
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		double[][] result = new double[tmpResult.size()][tmpResult.get(0)
				.size()];
		for (int i = 0; i < tmpResult.size(); i++) {
			List<String> valueStrList = tmpResult.get(i);
			for (int j = 0; j < valueStrList.size(); j++) {
				String values[] = valueStrList.get(j).split("\t");
				result[i][j] = Double.valueOf(values[1]);
			}
		}
		return result;
	}

	/**
	 * ファイルパスを指定してBufferedReader作成
	 *
	 * @param pass
	 *            ファイルパス
	 * @param encode
	 *            文字コード
	 * @return BufferedReader
	 */
	public static BufferedReader openBufferedReader(String pass, String encode) {
		if (encode.length() == 0)
			encode = "UTF-8";
		try {
			FileInputStream fis = new FileInputStream(pass);
			InputStreamReader ir = new InputStreamReader(fis, encode);
			BufferedReader br = new BufferedReader(ir);
			return br;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
