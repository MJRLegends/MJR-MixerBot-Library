package com.mjr.mjrmixer;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class MixerReconnectThread extends BasicKillableThread {
	private CopyOnWriteArrayList<MixerBotBase> mixerBotsChat = new CopyOnWriteArrayList<MixerBotBase>();
	private CopyOnWriteArrayList<MixerBotBase> mixerBotsConstell = new CopyOnWriteArrayList<MixerBotBase>();

	private int mixerBotSleepTime;

	public MixerReconnectThread(int mixerBotSleepTime) {
		super("Mixer Framework Reconnect Thread");
		this.mixerBotSleepTime = mixerBotSleepTime;
	}

	@Override
	public void run() {
		while (!this.isKillThread()) {
			try {
				ExecutorService threadPool = Executors.newCachedThreadPool();
				if (mixerBotsChat.size() != 0) {
					Iterator<MixerBotBase> iterator = mixerBotsChat.iterator();
					int attempt = 0;
					while (iterator.hasNext()) {
						MixerBotBase bot = iterator.next();
						do {
							if (bot.getReconnectData().getLastDisconnectTimeChat() + 5000 <= System.currentTimeMillis()) {
								attempt = attempt + 1;
								if (bot.isChatConnectionClosed()) {
									Callable<Boolean> callable = () -> {
										bot.reconnectChat();
										return true;
									};
									Future<Boolean> future = threadPool.submit(callable);
									try {
										future.get(30, TimeUnit.SECONDS);
									} catch (TimeoutException e) {
										MixerEventHooks.triggerOnErrorEvent("[Mixer Framework Reconnect] Timeout", e);
										MixerEventHooks.triggerOnInfoEvent(bot.getCoreData().getChannelName(), bot.getCoreData().getChannelID(), "[Mixer Framework Reconnect] Chat reconnect has timed out for taking to long will skip and retry! Attempt " + attempt);
										if (!bot.isChatConnectionClosed()) {
											MixerEventHooks.triggerOnFailedReconnectEvent(attempt, bot.getCoreData().getChannelName(), bot.getCoreData().getChannelID());
											bot.disconnectChat();
											Thread.sleep(getBackOffAmount(attempt));
										}
									}
								}
								//Thread.sleep((mixerBotSleepTime * 1000));
								if (!bot.isChatConnectionClosed())
									mixerBotsChat.remove(bot);
							} else
								Thread.sleep(5 * 1000);
						} while (bot.isChatConnectionClosed() && attempt < 10);
						attempt = 0;
					}
				}
				if (mixerBotsConstell.size() != 0) {
					Iterator<MixerBotBase> iterator = mixerBotsConstell.iterator();
					int attempt = 0;
					while (iterator.hasNext()) {
						MixerBotBase bot = iterator.next();
						do {
							if (bot.getReconnectData().getLastDisconnectTimeConstel() + 5000 <= System.currentTimeMillis()) {
								attempt = attempt + 1;
								if (bot.isConstellationConnectionClosed()) {
									Callable<Boolean> callable = () -> {
										bot.reconnectConstellation();
										return true;
									};
									Future<Boolean> future = threadPool.submit(callable);
									try {
										future.get(30, TimeUnit.SECONDS);
									} catch (TimeoutException e) {
										MixerEventHooks.triggerOnErrorEvent("[Mixer Framework Reconnect] Timeout", e);
										MixerEventHooks.triggerOnInfoEvent(bot.getCoreData().getChannelName(), bot.getCoreData().getChannelID(), "[Mixer Framework Reconnect] Constellation reconnect has timed out for taking to long will skip and retry! Attempt " + attempt);
										if (!bot.isConstellationConnectionClosed()) {
											MixerEventHooks.triggerOnFailedReconnectEvent(attempt, bot.getCoreData().getChannelName(), bot.getCoreData().getChannelID());
											bot.disconnectConstellation();
											Thread.sleep(getBackOffAmount(attempt));
										}
									}
								}
								//Thread.sleep((mixerBotSleepTime * 1000));
								if (!bot.isConstellationConnectionClosed())
									mixerBotsConstell.remove(bot);
							} else
								Thread.sleep(5 * 1000);
						} while (bot.isConstellationConnectionClosed() && attempt < 10);
						attempt = 0;
					}
				}

				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e) {
					MixerEventHooks.triggerOnErrorEvent("Mixer Framework: ReconnectThread Errored", e);

				}
			} catch (Exception e) {
				MixerEventHooks.triggerOnErrorEvent("Mixer Framework: ReconnectThread Errored", e);
			}
		}

	}

	public int getBackOffAmount(int attempt) {
		switch (attempt) {
		default:
		case 1:
			return 2000;
		case 2:
			return 5000;
		case 3:
			return 10000;
		case 4:
			return 20000;
		case 5:
			return 30000;
		case 6:
			return 40000;
		case 7:
			return 50000;
		case 8:
			return 60000;
		case 9:
			return 80000;
		case 10:
			return 100000;
		}
	}

	public List<MixerBotBase> getMixerBotChatBases() {
		return mixerBotsChat;
	}

	public List<MixerBotBase> getMixerBotConstellationBases() {
		return mixerBotsConstell;
	}

	public void addMixerBotChatBase(MixerBotBase bot) {
		if (!mixerBotsChat.contains(bot))
			this.mixerBotsChat.add(bot);
	}

	public void addMixerBotChatConstellation(MixerBotBase bot) {
		if (!mixerBotsConstell.contains(bot))
			this.mixerBotsConstell.add(bot);
	}

	public int getMixerBotBaseSleepTime() {
		return mixerBotSleepTime;
	}
}
