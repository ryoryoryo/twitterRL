package clustering;

import java.util.Stack;

/**
 * x-means<br>
 * Reference:<br>
 * http://d.hatena.ne.jp/sstoyosawa/20081212/<br>
 * http://www.cs.cmu.edu/~dpelleg/download/xmeans.pdf
 *
 *
 * @author miyagi
 *
 */
public class XMeans {

	private static int numFirstCluster = 3;

	/**
	 * 実行
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		XMeans xmeans = new XMeans();
		KMeans kmeans = new KMeans();

		double[][] rawData = ClusterDataReader
				.readTf("R:/twitter-experiment-result/data-analysis/kingjim-2/twitter-bow/",
						"kingjim");// 入力データ
		// double[][] rawData = ClusterDataReader.readTestRawData();

		// 実行
		int[] clustering = xmeans.executeClustering(rawData);

		// 出力
		for (int i = 0; i < clustering.length; i++) {
			System.out.println(i + "\t" + rawData[i][0] + ":" + rawData[i][1]
					+ "\t" + clustering[i]);
		}
	}

	public static int[] executeClustering(double[][] rawData) {
		KMeans kmeans = new KMeans();
		int maxCount = 30; // 最大繰り返し数
		int[] clustering = kmeans.executeClustering(rawData, numFirstCluster,
				maxCount); // 初期クラスタリング
		double centroids[][] = kmeans.getCentroids();
		Stack<Integer> stack = new Stack<Integer>(); // 処理するクラスタ
		for (int i = 0; i < numFirstCluster; i++) {
			stack.push(i);
		}
		int clusterCount = numFirstCluster;

		while (!stack.isEmpty()) {
			KMeans subKmeans = new KMeans();
			int processingCluster = stack.pop();// 処理するクラスタ
			int numProcessingCluster = getProcessingClusterCount(clustering,
					processingCluster);
			double[][] processingData = getProcessingData(rawData, clustering,
					processingCluster, numProcessingCluster); // 上から順に追加
			int[] subClustering = subKmeans.executeClustering(processingData,
					2, maxCount);
			double subCentroids[][] = subKmeans.getCentroids();

			double bic = getBic(rawData, processingCluster, 1, clustering,
					centroids, clusterCount);
			double bicBoth = getBicBoth(processingData, subClustering,
					subCentroids);
			if (bic > bicBoth) {
				// 更新
				clustering = updateClustering(clustering, subClustering,
						processingCluster);
				centroids = updateCentroids(centroids, subCentroids,
						processingCluster);
				stack = updateStack(stack, processingCluster);
				clusterCount++;
			}
		}
		System.out.println("cluster num: " + clusterCount);
		return clustering;
	}

	/**
	 * ベイズ情報量基準
	 *
	 * @param rawData
	 * @param processingCluster
	 *            処理するクラスタ
	 * @param numCluster
	 * @return
	 */
	private static double getBic(double[][] rawData, int processingCluster,
			int numCluster, int[] clustering, double centroids[][],
			int clusterCount) {
		double variance = getVariance(rawData, clustering, processingCluster,
				centroids);
		return getLogLikelihood(rawData, clusterCount, numCluster, variance)
				- (rawData[0].length / 2.0) * Math.log(rawData.length);
	}

	/**
	 * ベイズ情報量基準(2変数)
	 *
	 * @param rawData
	 * @param subClustering
	 * @param subKmeans
	 * @return
	 */
	private static double getBicBoth(double[][] rawData, int[] subClustering,
			double subCentroids[][]) {
		double variance0 = getVariance(rawData, subClustering, 0, subCentroids);
		double variance1 = getVariance(rawData, subClustering, 1, subCentroids);
		return getLogLikelihood(rawData, 2, 2, variance0)
				+ getLogLikelihood(rawData, 2, 2, variance1)
				- ((1 + rawData[0].length + 2) / 2.0)
				* Math.log(rawData.length);
	}

	/**
	 * 分散取得
	 *
	 * @param rawData
	 * @return
	 */
	private static double getVariance(double[][] rawData, int[] clustering,
			int processingCluster, double centroids[][]) {
		double sumDistance = getDistance(centroids, rawData, clustering,
				processingCluster);
		return ((double) 1 / (rawData.length * rawData[0].length))
				* sumDistance;
	}

	/**
	 * ベクトル２乗
	 *
	 * @param centroids
	 * @param rawData
	 * @param clustering
	 * @param numProcessingCluster
	 * @return
	 */
	private static double getDistance(double[][] centroids, double[][] rawData,
			int[] clustering, int processingCluster) {
		double distance = 0.0;
		for (int i = 0; i < clustering.length; i++) {
			if (clustering[i] == processingCluster) {
				for (int j = 0; j < centroids[clustering[i]].length; j++) {
					distance += (centroids[clustering[i]][j] - rawData[i][j])
							* (centroids[clustering[i]][j] - rawData[i][j]);
				}
			}
		}
		return distance;
	}

	/**
	 * 対数尤度
	 *
	 * @param rawData
	 * @param numProcessingCluster
	 *            クラスタ数
	 * @param numCluster
	 * @param variance
	 * @return
	 */
	private static double getLogLikelihood(double[][] rawData,
			int clusterCount, int numCluster, double variance) {
		double numData = (double) rawData.length;
		double dimension = (double) rawData[0].length;
		return -(clusterCount / 2.0) * Math.log(2 * Math.PI)
				- ((clusterCount * dimension) / 2.0) * Math.log(variance)
				- (clusterCount - numCluster) / 2.0 + clusterCount
				* Math.log(clusterCount) - clusterCount * Math.log(numData);
	}

	/**
	 * 処理するクラスタ数
	 *
	 * @param clustering
	 * @param processingCluster
	 * @return
	 */
	private static int getProcessingClusterCount(int[] clustering,
			int processingCluster) {
		int processingCount = 0;
		for (int i = 0; i < clustering.length; i++) {
			if (clustering[i] == processingCluster) {
				processingCount++;
			}
		}
		return processingCount;
	}

	/**
	 * 指定したクラスタのデータ取得
	 *
	 * @param rawData
	 * @param clustering
	 * @param processingCluster
	 * @return
	 */
	private static double[][] getProcessingData(double[][] rawData,
			int[] clustering, int processingCluster, int processingCount) {
		double[][] result = new double[processingCount][];
		int index = 0;
		for (int i = 0; i < clustering.length; i++) {
			if (clustering[i] == processingCluster) {
				result[index] = rawData[i];
				index++;
			}
		}
		return result;
	}

	/**
	 * スタックを更新
	 *
	 * @param stack
	 * @param processingCluster
	 * @return
	 */
	private static Stack<Integer> updateStack(Stack<Integer> stack,
			int processingCluster) {
		stack.push(processingCluster);
		stack.push(processingCluster + 1);
		return stack;
	}

	/**
	 * クラスタ更新
	 *
	 * @param clustering
	 * @param subClustering
	 * @param processingCluster
	 * @return
	 */
	private static int[] updateClustering(int[] clustering,
			int[] subClustering, int processingCluster) {
		int[] newClustering = new int[clustering.length];
		int processingIndex = 0;
		for (int i = 0; i < clustering.length; i++) {
			int cluster = clustering[i];
			if (cluster < processingCluster) {
				// 処理クラスタより小さいクラスタそのまま
				newClustering[i] = cluster;
			} else if (cluster == processingCluster) {
				// 処理クラスタはsubClusterで割当 上から順に追加
				newClustering[i] = processingCluster
						+ subClustering[processingIndex];
				processingIndex++;
			} else if (cluster > processingCluster) {
				// 処理クラスタより大きいクラスタ +1
				newClustering[i] = cluster + 1;
			}
		}

		return newClustering;
	}

	/**
	 * 重心更新
	 *
	 * @param centroids
	 * @param subCentroids
	 * @param processingCluster
	 * @return
	 */
	private static double[][] updateCentroids(double centroids[][],
			double subCentroids[][], int processingCluster) {
		double[][] newCentroids = new double[centroids.length + 1][centroids[0].length];
		int processingIndex = 0;
		for (int i = 0; i < centroids.length; i++) {
			if (i != processingCluster) {
				for (int j = 0; j < centroids[i].length; j++) {
					newCentroids[processingIndex][j] = centroids[i][j];
				}
				processingIndex++;
			} else if (i == processingCluster) {
				for (int j = 0; j < centroids[i].length; j++) {
					newCentroids[processingIndex][j] = subCentroids[0][j];
				}
				processingIndex++;
				for (int j = 0; j < centroids[i].length; j++) {
					newCentroids[processingIndex][j] = subCentroids[1][j];
				}
				processingIndex++;
			}
		}
		return newCentroids;
	}
}
