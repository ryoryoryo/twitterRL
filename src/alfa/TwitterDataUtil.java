package alfa;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import model.TwitterUser;
import model.UserTweet;
import rlanalysis.RLTweetAnalysisUtil;
import rlanalysis.RLUserAnalysisUtil;

public class TwitterDataUtil {

	private final static String OUTPUT = "R:/twitter-experiment-result/verα/real-data/environment/";

	public static void main(String[] args) {
		// outputLearningUsersState("112480180");
		getRetweets();
	}

	private static List<UserTweet> getRetweets() {
		List<TwitterUser> userlist = RLUserAnalysisUtil.getUserList();
		List<String> retweetIds = new ArrayList<String>();
		for (TwitterUser user : userlist) {
			if (user.getRtTweetIdStr() != null) {
				retweetIds.add(user.getRtTweetIdStr());
			}
		}

		List<UserTweet> allTweetList = RLTweetAnalysisUtil.getAllTweet(); // 全ツイート
		List<UserTweet> tweetlist = new ArrayList<UserTweet>();
		for(UserTweet tweet : allTweetList) {
			if(retweetIds.contains(tweet.getTweetIdStr())) {
				tweetlist.add(tweet);
			}
		}
		return tweetlist;
	}

	/**
	 * 発信者のツイート時間から時間取得
	 *
	 * @param originatorTweetList
	 * @return
	 */
	public static int[] getOriginatorTimes(List<UserTweet> originatorTweetList) {
		int startTime = 30;
		int endTime = 0;
		SimpleDateFormat sdf = new SimpleDateFormat("HH");
		for (UserTweet tweet : originatorTweetList) {
			int currentTime = Integer
					.parseInt(sdf.format(tweet.getCreatedAt()));
			if (currentTime < startTime) {
				startTime = currentTime;
			}
			if (currentTime > endTime) {
				endTime = currentTime;
			}
		}

		int timeSize = (endTime + 1) - startTime + 1;
		int times[] = new int[timeSize];
		for (int i = 0; i < timeSize; i++) {
			times[i] = startTime + i;
		}

		return times;
	}

	/**
	 * 学習用の発信者ツイート
	 *
	 * @param tweetList
	 */
	public static Map<Integer, List<UserTweet>> getLearningOriginatorTweets(
			List<UserTweet> tweetList) {
		Map<Integer, List<UserTweet>> tweetTimeMap = new HashMap<Integer, List<UserTweet>>();
		SimpleDateFormat sdf = new SimpleDateFormat("HH");
		Date startDate = tweetList.get(0).getCreatedAt();
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDate);
		cal.set(Calendar.DATE, cal.get(Calendar.DATE) + 7);

		for (UserTweet tweet : tweetList) {
			if (cal.getTime().before(tweet.getCreatedAt())) {
				break;// 最初の7日まで
			}
			int time = Integer.parseInt(sdf.format(tweet.getCreatedAt()));
			if (tweetTimeMap.containsKey(time)) {
				List<UserTweet> tweets = tweetTimeMap.get(time);
				tweets.add(tweet);
				tweetTimeMap.put(time, tweets);
			} else {
				List<UserTweet> tweets = new ArrayList<UserTweet>();
				tweets.add(tweet);
				tweetTimeMap.put(time, tweets);
			}
		}
		return tweetTimeMap;
	}

	/**
	 * アクティブorノンアクティブMap
	 *
	 * @param originatorId
	 * @param userTweetMap
	 * @param originatorTweetList
	 * @return
	 */
	private static Map<String, Map<Integer, Integer>> getActiveStateMap(
			String originatorId, Map<String, List<UserTweet>> userTweetMap,
			List<UserTweet> originatorTweetList) {
		Map<String, Map<Integer, Integer>> activeStateMap = new HashMap<String, Map<Integer, Integer>>();// ユーザ、時間、状態
		SimpleDateFormat sdf = new SimpleDateFormat("HH");
		int times[] = getOriginatorTimes(originatorTweetList); // ステップ
		// アクティブかノンアクティブか
		for (String userId : userTweetMap.keySet()) {
			if (!userId.equals(originatorId)) {
				Map<Integer, Integer> stateMap = new HashMap<Integer, Integer>();// 状態
				List<UserTweet> tweets = userTweetMap.get(userId);// ユーザのツイート群
				// ツイート
				for (UserTweet tweet : tweets) {
					int time = Integer
							.parseInt(sdf.format(tweet.getCreatedAt()));// ツイートの時間帯取得
					stateMap.put(time, TwitterMC.STATE_U);// アクティブのとき一時的に全員アンチ
				}
				// 入力されていない時間帯をノンアクティブにする
				for (int i = 0; i < times.length; i++) {
					if (!stateMap.containsKey(times[i])) {
						stateMap.put(times[i], TwitterMC.STATE_N);
					}
				}
				activeStateMap.put(userId, stateMap);
			}
		}

		return activeStateMap;
	}

	/**
	 * 各ユーザの状態取得
	 *
	 * @param userTweetMap
	 * @param times
	 * @param originatorId
	 */
	public static void outputLearningUsersState(String originatorId) {
		System.out.println("読み込み中");
		List<TwitterUser> uniqueUserList = RLUserAnalysisUtil
				.getUniqueUserList(RLUserAnalysisUtil.getUserList()); // ユーザ一覧
		Map<String, List<UserTweet>> userTweetMap = RLTweetAnalysisUtil
				.getUsersTweets(uniqueUserList); // ユーザごとのツイート一覧
		List<UserTweet> originatorTweetList = userTweetMap.get(originatorId);// 発信者のツイート群
		Map<String, Map<Integer, Integer>> activeStateMap = getActiveStateMap(
				originatorId, userTweetMap, originatorTweetList);// ユーザ、時間、状態
		List<UserTweet> allTweetList = RLTweetAnalysisUtil.getAllTweet(); // 全ツイート

		try {
			BufferedWriter bw = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(OUTPUT
							+ "originator-tweet-state-first.txt"), "UTF-8"));
			// 配信ツイートに対するユーザの状態変化
			Map<Integer, List<UserTweet>> originatorTimeTweets = getLearningOriginatorTweets(originatorTweetList); // 発信者の時間帯ごとツイート群
			for (int step : originatorTimeTweets.keySet()) {// ステップ
				System.out.println(step);
				List<UserTweet> originatorTweets = originatorTimeTweets
						.get(step);
				for (UserTweet originatorTweet : originatorTweets) { // ステップ内の配信ツイート
					if (originatorTweet.getInReplyToScreenName() == null) {
						// リツイート群探索
						List<UserTweet> retweets = new ArrayList<UserTweet>();
						Set<String> retweetUser = new HashSet<String>();
						for (UserTweet tweet : allTweetList) {
							if (tweet.getText().contains(
									originatorTweet.getText())) {
								retweets.add(tweet);
								retweetUser.add(tweet.getUserIdStr());
							}
						}
						// 時間帯処理入れる？
						// 結果
						bw.write(step + "\t" + originatorTweet.getTweetIdStr());
						for (TwitterUser user : uniqueUserList) {
							if (retweetUser.contains(user.getUserIdStr())) {
								bw.write("\t" + TwitterMC.STATE_F);
							} else {
								bw.write("\t"
										+ activeStateMap.get(
												user.getUserIdStr()).get(step));
							}
						}
						bw.write("\t" + originatorTweet.getRetweetCount());
						bw.write("\n");
					}
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
