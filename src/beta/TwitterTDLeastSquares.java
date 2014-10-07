package beta;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
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

	private static final String INPUT_DIR = "R:/twitter-experiment-result/verβ/ver0/input/";

	private static final String OUTPUT_DIR = "R:/twitter-experiment-result/verβ/ver0/output3-2/";

	private static final String OUTPUT_PARAM_DIR = OUTPUT_DIR + "parameter/";

	/** 状態ファイルパス */
	public static final String STATE_FILE_PASS = INPUT_DIR
			+ "state-combine-5-2.txt";

	/** 中心点ファイルパス */
	private static final String CENTERS_FILE_PASS = INPUT_DIR + "centers.txt";

	/** 行動ファイルパス */
	private static final String ACTIONS_FILE_PASS = INPUT_DIR
			+ "action-combine.txt";

	/** 報酬ファイルパス */
	private static final String REWARD_FILE_PASS = INPUT_DIR
			+ "local-reward-combine3.txt";

	/** 状態マップ */
	private static Map<Integer, double[]> stateMap; // ステップ・状態

	/** 中心点マップ */
	private static double[] center; // 中心点

	/** 行動マップ */
	private static Map<Integer, int[]> actionMap; // ステップ・クラスタ

	/** 報酬マップ */
	private static Map<Integer, double[]> rewardMap; // ステップ・報酬

	/** 反復回数 */
	private static final int iterationNum = 5;

	/** エピソード回数 */
	private static final int episordNum = 1000;

	/** ステップ数 */
	private static final int timeNum = 14;

	/** RBF(動径基底関数)の幅 */
	private static double width = 0.4;

	/** ソフトマックス関数τ */
	private static final double tau = 0.1;

	/** 割引率γ */
	private static final double gamma = 0.9;

	/** 行動 */
	private static int[] actions;

	/** 中心点 */
	private static double[][] centers;

	/** 政策 */
	private static double[] policy;

	/** モデルパラメータθ */
	private static double[][] theta; // B*actionL

	/** 行列X */
	private static double[][] x; // episordNum*timeNum*B * actionL

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
		System.out.println(width);
		init();
		double[][] results = iteration();
		output(results, "result-w" + width);
	}

	/**
	 * データ初期化
	 */
	private static void init() {
		stateMap = DataUtils.readStateMap(STATE_FILE_PASS); // ステップ・状態
		actionMap = DataUtils.readActionMap(ACTIONS_FILE_PASS); // ステップ・クラスタ
		actionNum = initActionNum();
		rewardMap = DataUtils.readRewardMap(actionNum, actionMap,
				REWARD_FILE_PASS); // ステップ・報酬
		setCenters();
		random = new Random();
		policy = new double[actionNum];
		theta = new double[centers.length][actionNum];
		x = new double[episordNum * (timeNum - 1)][centers.length * actionNum];
		r = new double[episordNum * (timeNum - 1)][1];
	}

	/**
	 * 行動数初期化
	 *
	 * @return
	 */
	private static int initActionNum() {
		int max = 0;
		for (int time : actionMap.keySet()) {
			if (actionMap.get(time).length > max) {
				int[] value = actionMap.get(time);
				for (int i = 0; i < value.length; i++) {
					if (max < value[i]) {
						max = value[i];
					}
				}
			}
		}
		return max + 1;
	}

	/**
	 * 反復
	 */
	private static double[][] iteration() {
		StringBuffer outputAction = new StringBuffer();
		double[][] results = new double[iterationNum][episordNum]; // 結果
		for (int l = 0; l < iterationNum; l++) {
			System.out.println("iteration:" + l);
			outputAction.append(l);
			for (int e = 0; e < episordNum; e++) {
				double rewardSum = 0;
				for (int t = 0; t < timeNum; t++) {
					int time = FIRST_TIME + t;
					double state[] = readState(time); // 状態観測
					double[] q = getQ(state); // 現在状態価値 actionNum
					updatePolicy(q); // 政策改善 actionL
					int action = selectAction(time, policy); // 行動選択
					outputAction.append("\t").append(action);
					double reward = doAction(time, action); // 行動実行
					rewardSum += reward;
					if (t > 0) {
						updateX(e, t, state, action);
						updateR(e, t, reward);
					}
					paction = action;
					pstate = state;
				}
				results[l][e] = rewardSum;
			}
			outputAction.append("\n");
			// outputXR(l);
			evaluatePolicy();// 政策評価
			// outputPolicy(l);
			outputTheta(l);
			stringOutputString(outputAction.toString(), OUTPUT_PARAM_DIR,
					"action-" + l + ".txt", "UTF-8");
			double avgReward = 0.0;
			for (int i = 0; i < episordNum; i++) {
				avgReward += results[l][i];
			}
			avgReward = avgReward / episordNum;
			if (avgReward > 80) {
				System.out.println();
			}
		}
		return results;
	}

	/**
	 * 状態取得
	 *
	 * @return
	 */
	private static double[] readState(int time) {
		return stateMap.get(time);
	}

	/**
	 * 中心ベクトル取得 B * stateL
	 *
	 * @return
	 */
	private static void setCenters() {
		center = DataUtils.readCenter(CENTERS_FILE_PASS); // 中心点
		int stateLength = stateMap.get(FIRST_TIME).length;
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
		outputCenters();
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
		outputPhi(phis);
		double[] q = new double[actionNum];
		for (int i = 0; i < q.length; i++) {
			for (int j = 0; j < phis.length; j++) {
				q[i] += theta[j][i] * phis[j];
			}
		}
		return q;
	}

	/**
	 * max policy actionL
	 *
	 * @return
	 */
	private static double[] updatePolicy2(double[] q) {
		int maxQIndex = 0;
		double maxQ = 0.0;
		for (int i = 0; i < q.length; i++) {
			if (maxQ < q[i]) {
				maxQIndex = i;
				maxQ = q[i];
			}
		}
		for (int i = 0; i < q.length; i++) {
			if (i == maxQIndex) {
				policy[i] = 1;
			} else {
				policy[i] = 0;
			}
		}
		return policy;
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
	private static int selectAction(int time, double[] policy) {
		actions = actionMap.get(time);
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
		return selectLastAction();
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
	 * 行動ランダム
	 *
	 * @return
	 */
	private static int selectLastAction() {
		int maxAction = Integer.MAX_VALUE;
		double maxValue = 0;

		for (int i = 0; i < actions.length; i++) {
			double value = policy[actions[i]];
			if (value > maxValue) {
				maxValue = value;
				maxAction = actions[i];
			}
		}
		return random.nextInt(policy.length);
	}

	/**
	 * 行動実行
	 *
	 * @return
	 */
	private static double doAction(int time, int action) {
		double[] rewards = rewardMap.get(time);
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
				x[e * (timeNum - 1) + (t - 1)][i * actionNum + j] = pphi[i][j]
						- gamma * aphi[i][j];
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
		r[e * (timeNum - 1) + (t - 1)][0] = reward;
	}

	/**
	 * 政策評価
	 *
	 * @return
	 */
	private static void evaluatePolicy() {
		RealMatrix realX = MatrixUtils.createRealMatrix(x);
		RealMatrix realTransposedX = realX.transpose();
		RealMatrix realR = MatrixUtils.createRealMatrix(r);
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
	 * 出力
	 *
	 * @param results
	 */
	private static void output(double[][] results, String fileName) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < results.length; i++) {
			double average = 0;
			for (int j = 0; j < results[i].length; j++) {
				average += results[i][j];
				// System.out.println(i + ":" + j + "\t" + results[i][j]);
			}
			System.out.println(average / episordNum);
			result.append(average / episordNum).append("\n");
		}
		stringOutputString(result.toString(), OUTPUT_DIR, fileName + ".txt",
				"UTF-8");
	}

	/**
	 * 政策出力
	 */
	private static void outputPolicy(int l) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < policy.length - 1; i++) {
			result.append(policy[i]).append("\t");
		}
		result.append(policy[policy.length - 1]);
		stringOutputString(result.toString(), OUTPUT_PARAM_DIR, "policy-" + l
				+ ".txt", "UTF-8");
	}

	// private static void outputXR(int l) {
	// double[][] tmpX = convertX();
	// StringBuffer resultX = new StringBuffer();
	// for (int i = 0; i < tmpX.length; i++) {
	// for (int j = 0; j < tmpX[i].length; j++) {
	// resultX.append("\t").append(tmpX[i][j]);
	// }
	// resultX.append("\n");
	// }
	// stringOutputString(resultX.toString(), OUTPUT_PARAM_DIR, "x-" + l
	// + ".txt", "UTF-8");
	//
	// double[][] tmpR = convertR();
	// StringBuffer resultR = new StringBuffer();
	// for (int i = 0; i < tmpR.length; i++) {
	// for (int j = 0; j < tmpR[i].length; j++) {
	// resultR.append("\t").append(tmpR[i][j]);
	// }
	// resultR.append("\n");
	// }
	// stringOutputString(resultR.toString(), OUTPUT_PARAM_DIR, "r-" + l
	// + ".txt", "UTF-8");
	// }

	private static void outputQ(int l) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < policy.length - 1; i++) {
			result.append(policy[i]).append("\t");
		}
		result.append(policy[policy.length - 1]);
		stringOutputString(result.toString(), OUTPUT_PARAM_DIR, "q-" + l
				+ ".txt", "UTF-8");
	}

	/**
	 * θ出力
	 */
	private static void outputTheta(int l) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < theta.length; i++) {
			for (int j = 0; j < theta[i].length - 1; j++) {
				result.append(theta[i][j]).append("\t");
			}
			result.append(theta[i][theta[i].length - 1]).append("\n");
		}
		stringOutputString(result.toString(), OUTPUT_PARAM_DIR, "theta-" + l
				+ ".txt", "UTF-8");
	}

	/**
	 * phi出力
	 */
	private static void outputPhi(double[] phi) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < phi.length; i++) {
			result.append(phi[i]).append("\n");
		}
		stringOutputString(result.toString(), OUTPUT_PARAM_DIR, "phi.txt",
				"UTF-8");
	}

	private static void outputCenters() {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < centers.length; i++) {
			for (int j = 0; j < centers[i].length - 1; j++) {
				result.append(centers[i][j]).append("\t");
			}
			result.append(centers[i][centers[i].length - 1]).append("\n");
		}
		stringOutputString(result.toString(), OUTPUT_PARAM_DIR, "centers.txt",
				"UTF-8");
	}

	/**
	 * Stringを出力するメソッド
	 *
	 * @param content
	 * @param directory
	 * @param filename
	 * @param character
	 */
	private static void stringOutputString(String content, String directory,
			String filename, String character) {
		try {
			FileOutputStream fos = new FileOutputStream(directory + filename);
			OutputStreamWriter osw = new OutputStreamWriter(fos, character);
			BufferedWriter bw = new BufferedWriter(osw);

			bw.write(content);

			bw.close();
			osw.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
