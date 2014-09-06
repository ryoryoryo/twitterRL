package clustering;

import java.util.Random;

/**
 * k-means <br>
 * Reference：http://msdn.microsoft.com/ja-jp/magazine/jj891054.aspx
 *
 * @author miyagi
 *
 */
public class KMeans {

	/** クラスタ */
	private static int[] clustering;

	/** 平均 */
	private static double means[][];

	/** 重心 */
	private static double centroids[][];

	/**
	 * 実行
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		KMeans cluster = new KMeans();

		// パラメータ
		int numClusters = 3; // k(クラスタ数)
		int maxCount = 30; // 最大繰り返し数

		// 入力データ
		double[][] rawData = cluster.readTestRawData();

		// 実行
		int[] clustering = cluster.executeClustering(rawData, numClusters,
				maxCount);

		// 出力
		for (int i = 0; i < clustering.length; i++) {
			System.out.println(i + "\t" + rawData[i][0] + ":" + rawData[i][1]
					+ "\t" + clustering[i]);
		}

		for (int i = 0; i < centroids.length; i++) {
			for (int j = 0; j < centroids[i].length; j++) {
				System.out.println(centroids[i][j]);
			}
		}
	}

	public double[][] readTestRawData() {
		double[][] rawData = new double[20][2];
		rawData[0] = new double[] { 65.0, 220.0 };
		rawData[1] = new double[] { 73.0, 160.0 };
		rawData[2] = new double[] { 59.0, 110.0 };
		rawData[3] = new double[] { 61.0, 120.0 };
		rawData[4] = new double[] { 75.0, 150.0 };
		rawData[5] = new double[] { 67.0, 240.0 };
		rawData[6] = new double[] { 68.0, 230.0 };
		rawData[7] = new double[] { 70.0, 220.0 };
		rawData[8] = new double[] { 62.0, 130.0 };
		rawData[9] = new double[] { 66.0, 210.0 };
		rawData[10] = new double[] { 77.0, 190.0 };
		rawData[11] = new double[] { 75.0, 180.0 };
		rawData[12] = new double[] { 74.0, 170.0 };
		rawData[13] = new double[] { 70.0, 210.0 };
		rawData[14] = new double[] { 61.0, 110.0 };
		rawData[15] = new double[] { 58.0, 100.0 };
		rawData[16] = new double[] { 66.0, 230.0 };
		rawData[17] = new double[] { 59.0, 120.0 };
		rawData[18] = new double[] { 68.0, 210.0 };
		rawData[19] = new double[] { 61.0, 130.0 };
		return rawData;
	}

	/**
	 * クラスタリング実行
	 *
	 * @param rawData
	 * @param numClusters
	 * @param numAttributes
	 * @param maxCount
	 * @return
	 */
	public int[] executeClustering(double[][] rawData, int numClusters,
			int maxCount) {
		int numTuplues = rawData.length;
		int numAttributes = rawData[0].length;
		boolean changedFlag = true;
		int count = 0;

		clustering = initClustering(numTuplues, numClusters, 0); // クラスタ初期化
		means = new double[numClusters][numAttributes]; // 平均タプル初期化
		centroids = new double[numClusters][numAttributes]; // 重心初期化

		means = updateMeans(rawData, clustering, means);
		centroids = updateCentroids(rawData, clustering, means, centroids);

		// 繰り返し
		while (changedFlag == true && count < maxCount) {
			count++;
			changedFlag = assignCluster(rawData, clustering, centroids);
			means = updateMeans(rawData, clustering, means);
			centroids = updateCentroids(rawData, clustering, means, centroids);
		}
		return clustering;
	}

	/**
	 * クラスタ初期化
	 *
	 * @param numTuples
	 * @param numClusters
	 * @param randomSeed
	 * @return
	 */
	private static int[] initClustering(int numTuples, int numClusters,
			int randomSeed) {
		Random random = new Random(randomSeed);
		int[] clustering = new int[numTuples];
		for (int i = 0; i < numClusters; i++) {
			clustering[i] = i;
		}
		for (int i = numClusters; i < clustering.length; i++) {
			clustering[i] = random.nextInt(numClusters);
		}
		return clustering;
	}

	/**
	 * クラスタごとの平均タプルを更新
	 *
	 * @param rawData
	 * @param clustering
	 * @param means
	 * @return
	 */
	private static double[][] updateMeans(double[][] rawData, int[] clustering,
			double[][] means) {
		int numClusters = means.length; // クラスタ数
		// 初期化
		for (int i = 0; i < means.length; i++) {
			for (int j = 0; j < means[i].length; j++) {
				means[i][j] = 0.0;
			}
		}

		int[] clusterCounts = new int[numClusters]; // 各クラスタ数
		for (int i = 0; i < rawData.length; i++) {
			int cluster = clustering[i];
			clusterCounts[cluster]++;
			for (int j = 0; j < rawData[i].length; j++) {
				means[cluster][j] += rawData[i][j]; // 加算
			}
		}
		for (int i = 0; i < means.length; i++) {
			for (int j = 0; j < means[i].length; j++) {
				means[i][j] = means[i][j] / clusterCounts[i]; // 平均
			}
		}
		return means;
	}

	/**
	 * 重心を計算(平均に近いタプル)
	 *
	 * @param rawData
	 * @param clustering
	 * @param cluster
	 * @param means
	 * @return
	 */
	private static double[] computeCentroid(double[][] rawData,
			int[] clustering, int cluster, double[][] means) {
		int numAttributes = means[0].length;
		double[] centroid = new double[numAttributes];
		double minDistance = Double.MAX_VALUE;
		for (int i = 0; i < rawData.length; i++) {
			int rawDataCluster = clustering[i];
			if (cluster == rawDataCluster) {
				double currentDistance = getDistance(rawData[i], means[cluster]);
				if (currentDistance < minDistance) {
					minDistance = currentDistance;
					for (int j = 0; j < centroid.length; j++) {
						centroid[j] = rawData[i][j];
					}
				}
			}
		}
		return centroid;
	}

	/**
	 * 各クラスタの重心を更新
	 *
	 * @param rawData
	 * @param clustering
	 * @param means
	 * @param centroids
	 * @return
	 */
	private static double[][] updateCentroids(double[][] rawData,
			int[] clustering, double[][] means, double[][] centroids) {
		for (int i = 0; i < centroids.length; i++) {
			centroids[i] = computeCentroid(rawData, clustering, i, means);
		}
		return centroids;
	}

	/**
	 * ユークリッド距離
	 *
	 * @param rawVector
	 * @param meanVector
	 * @return
	 */
	private static double getDistance(double[] rawVector, double[] meanVector) {
		double sumSquaredDiffs = 0.0;
		for (int i = 0; i < rawVector.length; i++) {
			sumSquaredDiffs += Math.pow((rawVector[i] - meanVector[i]), 2);
		}
		return Math.sqrt(sumSquaredDiffs);
	}

	/**
	 * タプルをクラスタに割当
	 *
	 * @param rawData
	 * @param clustering
	 * @param centroids
	 * @return
	 */
	private static boolean assignCluster(double[][] rawData, int[] clustering,
			double[][] centroids) {
		int numClusters = centroids.length;
		boolean changedFlag = false;
		double[] distances = new double[numClusters];
		for (int i = 0; i < rawData.length; i++) {
			for (int j = 0; j < numClusters; j++) {
				distances[j] = getDistance(rawData[i], centroids[j]);
			}
			int newCluster = getMinIndex(distances);
			if (newCluster != clustering[i]) {
				changedFlag = true;
				clustering[i] = newCluster;
			}
		}
		return changedFlag;
	}

	/**
	 * 最短距離値のインデックス取得
	 *
	 * @param distances
	 * @return
	 */
	private static int getMinIndex(double[] distances) {
		int minIndex = 0;
		double smallDistance = distances[0];
		for (int i = 0; i < distances.length; i++) {
			if (distances[i] < smallDistance) {
				smallDistance = distances[i];
				minIndex = i;
			}
		}
		return minIndex;
	}

	public static int[] getClustering() {
		return clustering;
	}

	public static void setClustering(int[] clustering) {
		KMeans.clustering = clustering;
	}

	public static double[][] getMeans() {
		return means;
	}

	public static void setMeans(double means[][]) {
		KMeans.means = means;
	}

	public static double[][] getCentroids() {
		return centroids;
	}

	public static void setCentroids(double centroids[][]) {
		KMeans.centroids = centroids;
	}
}
