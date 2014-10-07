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
 * カーネル法による実装
 *
 * @author miyagi
 *
 */
public class TwitterKernalLeastSquaresPolicyIteration {

	private static final String INPUT_DIR = "R:/twitter-experiment-result/verβ/ver0/input/";

	private static final String OUTPUT_DIR = "R:/twitter-experiment-result/verβ/ver0/output-k0/";

	private static final String OUTPUT_PARAM_DIR = OUTPUT_DIR + "parameter/";

	/** 状態ファイルパス */
	public static final String STATE_FILE_PASS = INPUT_DIR
			+ "state-combine-5-2.txt";

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
	private static final int episordNum = 100;

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

	/** モデルパラメータθ */
	private static double[][] theta; // episordNum*timeNum

	/** 行列X */
	private static double[][] k; // episordNum*timeNum* episordNum*timeNum

	/** 行列data */
	private static double[][] data; // episordNum*timeNum* (5+1=状態次元＋行動次元)

	/** 行列data */
	private static double[][] pdata; // episordNum*timeNum* (5+1=状態次元＋行動次元)

	/** 行列r */
	private static double[][] r; // episordNum*timeNum

	/** ランダム */
	private static Random random;

	private static final int FIRST_TIME = 9;

	private static final int actionStateDimention = 6;

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
		random = new Random();
		stateMap = DataUtils.readStateMap(STATE_FILE_PASS); // ステップ・状態
		actionMap = DataUtils.readActionMap(ACTIONS_FILE_PASS); // ステップ・クラスタ
		actionNum = initActionNum();
		rewardMap = DataUtils.readRewardMap(actionNum, actionMap,
				REWARD_FILE_PASS); // ステップ・報酬
		setTheta();
		k = new double[episordNum * (timeNum)][episordNum * (timeNum)];
		r = new double[episordNum * (timeNum)][1];
		data = new double[episordNum * (timeNum)][actionStateDimention];
		pdata = new double[episordNum * (timeNum)][actionStateDimention];
	}

	private static void setTheta() {
		theta = new double[episordNum * (timeNum)][1];
		for (int i = 0; i < episordNum; i++) {
			for (int j = 0; j < timeNum; j++) {
				theta[i * (timeNum) + j][0] = random.nextDouble();
			}
		}
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
		double[][] results = new double[iterationNum][episordNum]; // 結果
		for (int l = 0; l < iterationNum; l++) {
			System.out.println("iteration:" + l);
			// outputPolicy(l);
			for (int e = 0; e < episordNum; e++) {
				double rewardSum = 0;
				for (int t = 0; t < timeNum; t++) {
					double[] policy = new double[actionNum];
					int time = FIRST_TIME + t;
					double state[] = readState(time); // 状態観測
					double[] q = getQ(state); // 現在状態価値 actionNum
					if (l == 0) {
						for (int i = 0; i < policy.length; i++) {
							policy[i] = 1.0 / actionNum;
						}
					} else {
						policy = updatePolicy(q); // 政策改善 actionL
					}
					int action = selectAction(time, policy); // 行動選択
					double reward = doAction(time, action); // 行動実行
					rewardSum += reward;
					updateData(e, t, state, action);
					updateR(e, t, reward);
				}
				results[l][e] = rewardSum;
			}
			evaluatePolicy();// 政策評価
			outputTheta(l);
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
	 * 中心ベクトルと状態ベクトルの距離取得
	 *
	 * @return
	 */
	private static double[] getDistance(double[] state, int action) {
		double[] stateAction = new double[actionStateDimention];
		for (int i = 0; i < state.length; i++) {
			stateAction[i] = state[i];
		}
		stateAction[stateAction.length - 1] = action;

		double[] distances = new double[episordNum * (timeNum)]; // M*T
		// 中心ベクトルと状態ベクトルの距離
		for (int i = 0; i < episordNum; i++) {
			for (int j = 0; j < timeNum; j++) {
				for (int t = 0; t < actionStateDimention; t++) {
					distances[i * (timeNum) + j] += (pdata[i * (timeNum) + j][t] - stateAction[t])
							* (pdata[i * (timeNum) + j][t] - stateAction[t]);
				}
			}
		}
		return distances;
	}

	/**
	 * 基底関数φ取得
	 *
	 * @return
	 */
	private static double[][] getCurrentPhis(double[] state, int action) {
		double[] distance = getDistance(state, action); // 距離 B
		double[][] phis = new double[episordNum * (timeNum)][1];
		// 動径基底関数(RBF)
		for (int i = 0; i < phis.length; i++) {
			phis[i][0] = Math.exp(-distance[i] / (2 * width * width));
		}
		return phis;
	}

	/**
	 * 価値関数取得 actionL
	 *
	 * @return
	 */
	private static double[] getQ(double[] state) {
		double[] q = new double[actionNum];
		for (int i = 0; i < actionNum; i++) {
			double[][] phis = getCurrentPhis(state, i);
			for (int s = 0; s < episordNum; s++) {
				for (int t = 0; t < timeNum; t++) {
					q[i] += theta[s * (timeNum) + t][0]
							* phis[s * (timeNum) + t][0];
				}
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
		double[] policy = new double[actionNum];
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
		double[] policy = new double[actionNum];
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
			a = selectRandomAction(policy);
			for (int i = 0; i < actions.length; i++) {
				if (actions[i] == a) {
					return a;
				}
			}
			count++;
		}
		return selectLastAction(policy);
	}

	/**
	 * 行動ランダム
	 *
	 * @return
	 */
	private static int selectRandomAction(double[] policy) {
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
	private static int selectLastAction(double[] policy) {
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
	 * 行列data更新
	 *
	 * @param e
	 * @param t
	 */
	private static void updateData(int e, int t, double[] state, int action) {
		for (int i = 0; i < state.length; i++) {
			data[e * (timeNum) + t][i] = state[i];
		}
		data[e * (timeNum) + t][state.length - 1] = action;
	}

	/**
	 * 動径基底関数(RBF)φ取得 B*actionL
	 *
	 * @param state
	 * @param action
	 * @return
	 */
	private static double[] getKernelPhi(int mt) {
		double[] distance = getKernelDistance(mt); // 距離 B
		double[] phis = new double[distance.length];
		// 動径基底関数(RBF)
		for (int i = 0; i < phis.length; i++) {
			phis[i] = Math.exp(-distance[i] / (2 * width * width));
		}
		return phis;
	}

	private static double[] getKernelDistance(int mt) {
		double[] distance = new double[episordNum * (timeNum)];
		for (int i = 0; i < episordNum; i++) {
			for (int j = 0; j < timeNum; j++) {
				for (int s = 0; s < actionStateDimention; s++) {
					double tmp = data[i * (timeNum) + j][s] - data[mt][s];
					distance[i * (timeNum) + j] += tmp * tmp;
				}
			}
		}
		return distance;
	}

	/**
	 * 行列r更新
	 *
	 * @param e
	 * @param t
	 * @param reward
	 */
	private static void updateR(int e, int t, double reward) {
		r[e * (timeNum) + t][0] = reward;
	}

	/**
	 * 政策評価
	 *
	 * @return
	 */
	private static void evaluatePolicy() {
		for (int i = 0; i < episordNum * (timeNum); i++) {
			k[i] = getKernelPhi(i);
		}
		RealMatrix realK = MatrixUtils.createRealMatrix(k);
		RealMatrix realTransposedK = realK.transpose();
		RealMatrix inverseK = new SingularValueDecomposition(realTransposedK)
				.getSolver().getInverse(); // Moore Penrose逆行列
		RealMatrix realR = MatrixUtils.createRealMatrix(r);
		RealMatrix result = inverseK.multiply(realR);
		updateTheta(result.getData());
		pdata = data;
	}

	private static void updateTheta(double[][] data) {
		for (int i = 0; i < episordNum; i++) { // B
			for (int j = 0; j < timeNum; j++) { // actionNum
				theta[i * (timeNum) + j][0] = data[i * (timeNum) + j][0];
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
	private static void outputPolicy(int l, double[] policy) {
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < policy.length - 1; i++) {
			result.append(policy[i]).append("\t");
		}
		result.append(policy[policy.length - 1]);
		stringOutputString(result.toString(), OUTPUT_PARAM_DIR, "policy-" + l
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
