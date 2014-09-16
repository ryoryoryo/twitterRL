package beta;

import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import data.DataUtils;

/**
 * TD２乗誤差(近似：最少２乗法)
 *
 * @author miyagi
 *
 */
public class TwitterTDLeastSquares {

	private static final String INPUT_DIR = "R:/twitter-experiment-result/verβ/ver1/input-realtime";

	/** 状態ファイルパス */
	public static final String STATE_FILE_PASS = INPUT_DIR + "/state/";

	/** 中心点ファイルパス */
	private static final String CENTERS_FILE_PASS = INPUT_DIR + "/centers.txt";

	/** 行動ファイルパス */
	private static final String ACTIONS_FILE_PASS = INPUT_DIR + "/action/";

	/** 報酬ファイルパス */
	private static final String REWARD_FILE_PASS = INPUT_DIR
			+ "/global-reward/";

	/** 状態マップ */
	private static Map<Integer, Map<Integer, double[]>> stateMap; // エピソード・ステップ・状態

	/** 中心点マップ */
	private static double[] center; // 中心点

	/** 行動マップ */
	private static Map<Integer, Map<Integer, int[]>> actionMap; // エピソード・ステップ・クラスタ

	/** 報酬マップ */
	private static Map<Integer, Map<Integer, double[]>> rewardMap; // エピソード・ステップ・報酬

	/** 反復回数 */
	private static final int iterationNum = 10;

	/** エピソード回数 */
	private static final int episordNum = 3;

	/** ステップ数 */
	private static final int timeNum = 14;

	/** RBF(動径基底関数)の幅 */
	private static final double width = 0.03;

	/** ソフトマックス関数τ */
	private static final double tau = 1.0;

	/** 割引率γ */
	private static final double gamma = 0.95;

	/** 行動 */
	private static int[] actions;

	/** 中心点 */
	private static double[][] centers;

	/** 政策 */
	private static double[] policy;

	/** モデルパラメータθ */
	private static double[][] theta; // B*actionL

	/** 行列X */
	private static double[][][][] x; // episordNum*timeNum*B * actionL

	/** 行列r */
	private static double[][] r; // episordNum*timeNum

	/** 前ステップ行動 */
	private static int paction;

	/** 前ステップ状態 */
	private static double[] pstate;

	/** ランダム */
	private static Random random;

	private static final int FIRST_TIME = 9;

	private static int actionNum = 0;

	public static void main(String[] args) {
		init();
		double[][] results = iteration();
		output(results);
	}

	/**
	 * データ初期化
	 */
	private static void init() {
		stateMap = DataUtils.readEpisordeStateMap(STATE_FILE_PASS); // ステップ・状態
		actionMap = DataUtils.readEpisordeActionMap(ACTIONS_FILE_PASS); // ステップ・クラスタ
		actionNum = initActionNum();
		rewardMap = DataUtils.readEpisordeRewardMap(actionNum, actionMap,
				REWARD_FILE_PASS); // ステップ・報酬
		setCenters();
		random = new Random();
		policy = new double[actionNum];
		theta = new double[centers.length][actionNum];
		x = new double[episordNum][timeNum - 1][centers.length][actionNum];
		r = new double[episordNum][timeNum - 1];
	}

	/**
	 * 行動数初期化
	 *
	 * @return
	 */
	private static int initActionNum() {
		int max = 0;
		for (int episorde : actionMap.keySet()) {
			Map<Integer, int[]> tmpMap = actionMap.get(episorde);
			for (int time : tmpMap.keySet()) {
				int[] tmp = tmpMap.get(time);
				if (max < tmp.length) {
					max = tmp.length;
				}
			}
		}
		return max;
	}

	/**
	 * 反復
	 */
	private static double[][] iteration() {
		double[][] results = new double[iterationNum][episordNum]; // 結果
		for (int l = 0; l < iterationNum; l++) {
			System.out.println("iteration:" + l);
			for (int e = 0; e < episordNum; e++) {
				double rewardSum = 0;
				for (int t = 0; t < timeNum; t++) {
					int time = FIRST_TIME + t;
					double[] state = readState(e, time); // 状態観測
					double[] q = getQ(state); // 現在状態価値 actionL
					updatePolicy(q); // 政策改善 actionL
					int action = selectAction(e, time, policy); // 行動選択
					if (action != Integer.MAX_VALUE) {
						double reward = doAction(e, time, action); // 行動実行
						rewardSum += reward;
						if (t > 0) {
							updateX(e, t, state, action);
							updateR(e, t, reward);
						}
						paction = action;
						pstate = state;
					}
				}
				results[l][e] = rewardSum;
			}
			evaluatePolicy();// 政策評価
		}
		return results;
	}

	/**
	 * 状態取得
	 *
	 * @return
	 */
	private static double[] readState(int episorde, int time) {
		Map<Integer, double[]> timeMap = stateMap.get(episorde);
		return timeMap.get(time);
	}

	/**
	 * 中心ベクトル取得 B * stateL
	 *
	 * @return
	 */
	private static void setCenters() {
		center = DataUtils.readCenter(CENTERS_FILE_PASS); // 中心点
		Map<Integer, double[]> tmpMap = stateMap.get(0);
		int stateLength = tmpMap.get(FIRST_TIME).length;
		int b = 1;
		for (int i = 0; i < stateLength; i++) {
			b *= center.length;
		}
		centers = new double[b][stateLength];
		for (int i = 0; i < b; i++) {
			int[] index = new int[stateLength];
			int tmpCount = i;
			for (int j = 0; j < index.length; j++) {
				index[j] = tmpCount % center.length;
				tmpCount = tmpCount / center.length;
			}
			for (int j = 0; j < index.length; j++) {
				centers[i][j] = center[index[j]];
			}
		}
	}

	/**
	 * 中心ベクトルと状態ベクトルの距離取得
	 *
	 * @return
	 */
	private static double[] getDistance(double[] state) {
		double[] distances = new double[centers.length]; // B
		// 中心ベクトルと状態ベクトルの距離
		for (int i = 0; i < distances.length; i++) {
			for (int j = 0; j < state.length; j++) {
				distances[i] += (centers[i][j] - state[j])
						* (centers[i][j] - state[j]);
			}
		}
		return distances;
	}

	/**
	 * 基底関数φ取得
	 *
	 * @return
	 */
	private static double[] getCurrentPhis(double[] state) {
		double[] distance = getDistance(state); // 距離 B
		double[] phis = new double[distance.length];
		// 動径基底関数(RBF)
		for (int i = 0; i < phis.length; i++) {
			phis[i] = Math.exp(-distance[i] / (2 * width * width));
		}
		return phis;
	}

	/**
	 * 価値関数取得 actionL
	 *
	 * @return
	 */
	private static double[] getQ(double[] state) {
		double[] phis = getCurrentPhis(state); // RBF B
		double[] q = new double[actionNum];
		for (int i = 0; i < q.length; i++) {
			for (int j = 0; j < phis.length; j++) {
				q[i] += theta[j][i] * phis[j];
			}
		}
		return q;
	}

	/**
	 * 政策改善(ソフトマックス関数) actionL
	 *
	 * @return
	 */
	private static double[] updatePolicy(double[] q) {
		double denominator = 0.0;
		for (int i = 0; i < q.length; i++) {
			denominator += Math.exp(q[i] / tau);
		}
		for (int i = 0; i < policy.length; i++) {
			if (denominator != 0.0 && !Double.isInfinite(denominator)) {
				policy[i] = Math.exp(q[i] / tau) / denominator;
			} else {
				policy[i] = 0.0;
			}
		}
		return policy;
	}

	/**
	 * 行動選択
	 *
	 * @return
	 */
	private static int selectAction(int episorde, int time, double[] policy) {
		Map<Integer, int[]> tmpActionMap = actionMap.get(episorde);
		actions = tmpActionMap.get(time);
		int a = 0;
		int count = 0;
		while (count < 10) {
			a = selectRandomAction();
			for (int i = 0; i < actions.length; i++) {
				if (actions[i] == a) {
					return a;
				}
			}
			count++;
		}
		return Integer.MAX_VALUE;
	}

	/**
	 * 行動ランダム
	 *
	 * @return
	 */
	private static int selectRandomAction() {
		double value = random.nextDouble();
		double culmulateThreshold = 0;
		for (int i = 0; i < policy.length; i++) {
			culmulateThreshold += policy[i];
			if (culmulateThreshold > value) {
				return i;
			}
		}
		return policy.length;
	}

	/**
	 * 行動実行
	 *
	 * @return
	 */
	private static double doAction(int episorde, int time, int action) {
		Map<Integer, double[]> tmpMap = rewardMap.get(episorde);
		double[] rewards = tmpMap.get(time);
		return rewards[action];
	}

	/**
	 * 行列X更新
	 *
	 * @param e
	 * @param t
	 */
	private static void updateX(int e, int t, double[] state, int action) {
		double[][] aphi = getAphi(state, action); // B*actionL
		double[][] pphi = getPphi(); // B*actionL
		for (int i = 0; i < aphi.length; i++) {
			for (int j = 0; j < aphi[i].length; j++) {
				x[e][t - 1][i][j] = pphi[i][j] - gamma * aphi[i][j];
			}
		}
	}

	/**
	 * aphi B*actionL
	 *
	 * @param state
	 * @param action
	 * @return
	 */
	private static double[][] getAphi(double[] state, int action) {
		double[][] aphi = new double[centers.length][actionNum];
		// 初期化
		for (int i = 0; i < aphi.length; i++) {
			for (int j = 0; j < aphi[i].length; j++) {
				aphi[i][j] = 1.0;
			}
		}
		// 更新
		for (int a = 0; a < actionNum; a++) {
			double[][] tmpPhi = getPhi(state, a); // B*actionL
			for (int i = 0; i < aphi.length; i++) {
				for (int j = 0; j < aphi[i].length; j++) {
					aphi[i][j] += tmpPhi[i][j] * policy[a];
				}
			}
		}
		return aphi;
	}

	/**
	 * pphi取得 B*actionL
	 *
	 * @return
	 */
	private static double[][] getPphi() {
		return getPhi(pstate, paction);
	}

	/**
	 * 動径基底関数(RBF)φ取得 B*actionL
	 *
	 * @param state
	 * @param action
	 * @return
	 */
	private static double[][] getPhi(double state[], int action) {
		double[] distance = getDistance(state); // 距離 B
		double[][] phis = new double[distance.length][actionNum];
		// 動径基底関数(RBF)
		for (int i = 0; i < phis.length; i++) {
			phis[i][action] = Math.exp(-distance[i] / (2 * width * width));
		}
		return phis;
	}

	/**
	 * 行列r更新
	 *
	 * @param e
	 * @param t
	 * @param reward
	 */
	private static void updateR(int e, int t, double reward) {
		r[e][t - 1] = reward;
	}

	/**
	 * 政策評価
	 *
	 * @return
	 */
	private static void evaluatePolicy() {
		double[][] tmpX = convertX();
		double[][] tmpR = convertR();
		RealMatrix realX = MatrixUtils.createRealMatrix(tmpX);
		RealMatrix realTransposedX = realX.transpose();
		RealMatrix realR = MatrixUtils.createRealMatrix(tmpR);
		RealMatrix realXX = realTransposedX.multiply(realX);
		// RealMatrix realXX = multipleMatrix(realTransposedX.getData(),
		// realX.getData());
		RealMatrix inverse = new SingularValueDecomposition(realXX).getSolver()
				.getInverse(); // Moore Penrose逆行列
		RealMatrix result = inverse.multiply(realTransposedX).multiply(realR);
		updateTheta(result.getData());
	}

	private static void updateTheta(double[][] data) {
		for (int i = 0; i < theta.length; i++) { // B
			for (int j = 0; j < theta[i].length; j++) { // actionNum
				theta[i][j] = data[i * actionNum + j][0];
			}
		}
	}

	private static RealMatrix multipleMatrix(double a[][], double b[][]) {
		double[][] result = new double[a.length][b[0].length];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < b[0].length; j++) {
				double sum = 0.0;
				for (int k = 0; k < b.length; k++) {
					sum += a[i][k] * b[k][j];
				}
				result[i][j] = sum;
			}
		}
		RealMatrix matrix = MatrixUtils.createRealMatrix(result);
		return matrix;
	}

	/**
	 * 4次元を２次元に変換
	 *
	 * @return
	 */
	private static double[][] convertX() {
		double[][] result = new double[episordNum * timeNum][centers.length
				* actionNum];
		for (int e = 0; e < x.length; e++) {
			for (int t = 0; t < x[e].length; t++) {
				for (int c = 0; c < x[e][t].length; c++) {
					for (int a = 0; a < x[e][t][c].length; a++) {
						result[e * timeNum + t][c * actionNum + a] = x[e][t][c][a];
					}
				}
			}
		}
		return result;
	}

	/**
	 * 2次元を1次元に変換
	 *
	 * @return
	 */
	private static double[][] convertR() {
		double[][] result = new double[episordNum * timeNum][1];
		for (int e = 0; e < r.length; e++) {
			for (int t = 0; t < r[e].length; t++) {
				result[e * timeNum + t][0] = r[e][t];
			}
		}
		return result;
	}

	/**
	 * 出力
	 *
	 * @param results
	 */
	private static void output(double[][] results) {
		for (int i = 0; i < results.length; i++) {
			double average = 0;
			for (int j = 0; j < results[i].length; j++) {
				average += results[i][j];
				// System.out.println(i + ":" + j + "\t" + results[i][j]);
			}
			System.out.println(average / episordNum);
		}
	}
}
