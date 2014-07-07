package alfa;



public class TwitterAlfaSimulator {
	public static void main(String[] args) {
		int L = 10; // 反復回数
		int M = 100; // エピソード数
		int T = 48; // ステップ数
		int PTYPE = 3; // 政策改善手法
		double EPSILON = 0.1; // e-greedyのパラメータe
		double TAU = 0.1; // softmaxのパラメータtau
		double GAMMA = 0.9;
		int N = 1000; // 状態長(汎用性ないかも) フォロワー数
		int STATE_CATEGO = 3;
		int N_STATES = 1; // 状態数{NA,N,F,U}
		for (int i = 0; i < STATE_CATEGO; i++) {
			N_STATES = N_STATES * N;
		}
		int N_ACTIONS = 2; // 行動数 ツイートする・しない

		long start = System.currentTimeMillis();
		TwitterMC.init(L, M, T, STATE_CATEGO, N, PTYPE, EPSILON, TAU, GAMMA,
				N_STATES, N_ACTIONS);
		TwitterMC.start();
		long stop = System.currentTimeMillis();
	    System.out.println("run time:" + (stop - start) / 1000 + "s");
	}
}
