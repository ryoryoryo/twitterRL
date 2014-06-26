package alfa;

import java.util.Random;

public class TwitterMC {
	/** 反復回数 */
	private static int iterationNum;

	/** エピソード数 */
	private static int episordnum;

	/** ステップ数 */
	private static int stepNum;

	/** 政策改善手法 */
	private static int ptype;

	/** e-greedyのパラメータe */
	private static double epsilon;

	/** softmaxのパラメータtau */
	private static double tau;

	/** 割引gamma */
	private static double gamma;

	/** 状態長(汎用性ないかも) */
	private static int statesLength;

	/** 状態種類数 */
	private static int statesFeatureNum;

	/** 状態数 */
	private static int statesNum;

	/** 行動数 */
	private static int actionsNum;

	/** 状態群 */
	private static int states[][];

	/** 行動群 */
	private static int actions[][];

	/** 報酬群 */
	private static int rewards[][];

	/** 割引報酬 */
	private static double drewards[][];

	/** 遷移群 */
	private static int visits[][];

	/** 現在の行動 */
	private static int currentAction;

	/** 現在の報酬 */
	private static int currentReward;

	/** greedy */
	public final static int IMPROVE_POLICY_TYPE_GREEDY = 1;

	/** e-greedy */
	public final static int IMPROVE_POLICY_TYPE_EGREEDY = 2;

	/** softmax */
	public final static int IMPROVE_POLICY_TYPE_SOFTMAX = 3;

	/** ノンアクティブ */
	public final static int STATE_N = 0;

	/** ファン */
	public final static int STATE_F = 1;

	/** アンチ */
	public final static int STATE_U = 2;

	public static Random rnd;

	/**
	 * 初期化
	 *
	 * @param l
	 * @param m
	 * @param t
	 * @param n
	 * @param type
	 * @param e
	 * @param ta
	 * @param ns
	 * @param na
	 */
	public static void init(int l, int m, int t, int n, int sc, int type,
			double e, double ta, double g, int ns, int na) {
		iterationNum = l;
		episordnum = m;
		stepNum = t;
		statesLength = n;
		statesFeatureNum = sc;
		ptype = type;
		epsilon = e;
		tau = ta;
		gamma = g;
		statesNum = ns;
		actionsNum = na;
		states = new int[episordnum][stepNum];
		actions = new int[episordnum][stepNum];
		rewards = new int[episordnum][stepNum];
		visits = getInitVisits(statesNum, actionsNum);
		drewards = new double[episordnum][stepNum];
		rnd = new Random();
	}

	public static void start() {
		double q[][] = new double[statesNum][actionsNum]; // 状態・行動価値関数(バックアップテーブル)

		// 政策反復
		double results[][] = new double[iterationNum][episordnum]; // 結果
		for (int l = 0; l < iterationNum; l++) {
			// long start = System.currentTimeMillis();

			// エピソード
			for (int m = 0; m < episordnum; m++) {
				// System.out.println("episord " + m + "/" + episordnum);
				int engagementSum = 0;
				int state[] = new int[statesLength];// 状態

				// ステップ
				for (int t = 0; t < stepNum; t++) {
					// System.out.println("step " + t + "/" + stepNum);
					int encodeState = encodeState(state);// エンコード
					double policy[] = improvePolicy(encodeState, q); // 政策改善
					state = actionSimulate(policy, t, state); // 次状態観測

					// 更新
					states[m][t] = encodeState;// 状態更新
					actions[m][t] = currentAction;// 行動更新
					rewards[m][t] = currentReward;// 報酬更新
					visits[encodeState][currentAction] += 1;// 遷移更新
					engagementSum += currentReward;

					// 割引報酬和の計算
					drewards[m][t] = rewards[m][t];
					for (int i = t - 1; i > -1; i--) {
						drewards[m][i] = gamma * drewards[m][i + 1];
					}
				}
				results[l][m] = engagementSum; // 結果
			}

			// 政策評価
			q = evaluatePolicy(q);
		}

		// 出力
		System.out.println("合計");
		for (int l = 0; l < iterationNum; l++) {
			int average = 0;
			for (int m = 0; m < episordnum; m++) {
				average += results[l][m];
			}
			System.out.println(l + "\t" + average);// / episordnum
		}
//		System.out.println("すべて");
//		for (int l = 0; l < iterationNum; l++) {
//			for (int m = 0; m < episordnum; m++) {
//				System.out.println(l + "\t" + m + "\t" + results[l][m]);
//			}
//		}
	}

	/**
	 * 行動シミュレーション
	 *
	 * @param policy
	 * @param t
	 * @param state
	 * @return
	 */
	private static int[] actionSimulate(double[] policy, int t, int[] state) {
		// 行動実行
		currentAction = getMaxAction(policy);

		// 環境(受信者)シミュレート⇒状態観測
		state = simpleEnvironmentSimulate(state);

		// 報酬
		if (currentAction == 0) {
			// ツイート配信しなかったとき
			currentReward = 0;
		} else {
			int engagement = 0;
			for (int i = 0; i < state.length; i++) {
				if (state[i] == STATE_N) {
					engagement += 0;
				} else if (state[i] == STATE_F) {
					engagement += 10;
				} else if (state[i] == STATE_U) {
					engagement += -1;
				}
			}
			currentReward = engagement;
		}

		return state;
	}

	/**
	 * 環境シミュレート
	 *
	 * @param state
	 * @return
	 */
	private static int[] simpleEnvironmentSimulate(int[] state) {
		Random rnd = new Random();
		for (int i = 0; i < state.length; i++) {
			int ran = rnd.nextInt(10);
			// すべての場合で0.6でノンアクティブ
			if (ran < 6) {
				state[i] = STATE_N;
			} else {
				if (state[i] == STATE_N) {
					if (ran == 6 || ran == 7) {
						state[i] = STATE_U;
					} else if (ran == 8 || ran == 9) {
						state[i] = STATE_F;
					}
				} else if (state[i] == STATE_U) {
					if (ran == 6) {
						state[i] = STATE_F;
					} else {
						state[i] = STATE_U;
					}
				} else if (state[i] == STATE_F) {
					if (ran == 6) {
						state[i] = STATE_U;
					} else {
						state[i] = STATE_F;
					}
				}
			}
		}
		return state;
	}

	/**
	 * 政策改善
	 *
	 * @param encodeState
	 * @param q
	 * @return
	 */
	public static double[] improvePolicy(int encodeState, double q[][]) {
		double policy[] = new double[actionsNum];
		double currentq[] = q[encodeState];
		if (ptype == IMPROVE_POLICY_TYPE_GREEDY) {
			policy[getMaxAction(currentq)] = 1;
		} else if (ptype == IMPROVE_POLICY_TYPE_EGREEDY) {
			for (int i = 0; i < actionsNum; i++) {
				policy[i] = epsilon / actionsNum;
			}
			policy[getMaxAction(currentq)] = 1 - epsilon + epsilon / actionsNum;
		} else if (ptype == IMPROVE_POLICY_TYPE_SOFTMAX) {
			double denominator = 0.0;
			for (int j = 0; j < actionsNum; j++) {
				denominator += Math.exp(currentq[j] / tau);
			}
			for (int i = 0; i < actionsNum; i++) {
				policy[i] = Math.exp(currentq[i] / tau) / denominator;
			}
		}
		return policy;
	}

	/**
	 * 最大値行動
	 *
	 * @param values
	 * @return
	 */
	public static int getMaxAction(double values[]) {
		double max = values[0];
		int maxIndex = 0;
		boolean diferenceFlag = false;

		for (int i = 1; i < values.length; i++) {
			if (values[i] > max) {
				max = values[i];
				maxIndex = i;
				diferenceFlag = true;
			}
		}

		if (diferenceFlag) {
			return maxIndex;
		} else {
			// もしすべて同じ値だったら、ランダム
			Random rnd = new Random();
			maxIndex = rnd.nextInt(actionsNum);
			return maxIndex;
		}
	}

	/**
	 * 政策評価
	 *
	 * @param q
	 * @return
	 */
	public static double[][] evaluatePolicy(double[][] q) {
		for (int m = 0; m < episordnum; m++) {
			for (int t = 0; t < stepNum; t++) {
				int s = states[m][t];
				int a = actions[m][t];
				if (t != 0 && s == 0) {
					break;
				}
				q[s][a] += drewards[m][t];
			}
		}
		for (int i = 0; i < statesNum; i++) {
			for (int j = 0; j < actionsNum; j++) {
				q[i][j] = q[i][j] / visits[i][j];
			}
		}
		return q;
	}

	/**
	 * 出現回数初期値 すべて1
	 *
	 * @param nstates
	 * @param nactions
	 * @return
	 */
	private static int[][] getInitVisits(int nstates, int nactions) {
		int visits[][] = new int[nstates][nactions];
		for (int i = 0; i < nstates; i++) {
			for (int j = 0; j < nactions; j++) {
				visits[i][j] = 1;
			}
		}
		return visits;
	}

	/**
	 * 状態変換
	 *
	 * @param state
	 * @return
	 */
	private static int encodeState(int state[]) {
		StringBuffer numlist = new StringBuffer();
		for (int i = 0; i < state.length; i++) {
			numlist.append(state[i]);
		}
		return Integer.parseInt(numlist.toString(), statesFeatureNum);
	}
}
