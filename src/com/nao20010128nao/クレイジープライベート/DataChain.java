package com.nao20010128nao.クレイジープライベート;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Map;

import com.google.gson.Gson;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;

public class DataChain {
	static File filesDir = new File("files");
	static final String ALPHABET_SMALL = "abcdefghijklmnopqrstuvwxyz_-";
	static final String RANDOM_CHARS = ALPHABET_SMALL + ALPHABET_SMALL.toUpperCase() + ALPHABET_SMALL
			+ ALPHABET_SMALL.toUpperCase();

	CPMain main;
	SecureRandom sr = new SecureRandom();
	Gson gson = new Gson();

	public DataChain(CPMain server) {
		// TODO 自動生成されたコンストラクター・スタブ
		main = server;
		if (!filesDir.exists()) {
			filesDir.mkdirs();
		}
	}

	public Response newChain(String path, String query) {
		Map<String, String> queryMap = Utils.getQueryMap(query);
		Response result = null;
		if (path.startsWith("/new/easy_redirect")) {
			// 転送型
			NodeParent np = new NodeParent();
			np.publicKey = createKey(true);
			np.privateKey = createKey(false);
			np.mode = "easyRedirect";
			np.prefix = queryMap.get("path");
			File dir = new File(filesDir, np.publicKey);
			dir.mkdirs();
			String json = gson.toJson(np, NodeParent.class);
			try {
				Files.write(new File(dir, "chain.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}
			EasyRedirectOptions opt = new EasyRedirectOptions();
			opt.address = queryMap.get("address");
			json = gson.toJson(opt, EasyRedirectOptions.class);
			try {
				Files.write(new File(dir, "options.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}
			dir = new File(dir, "sessions");
			dir.mkdirs();
			result = CPMain.newRedirectResponse("http://" + CPMain.HOST + "/yourtrace?private=" + np.privateKey);
		}
		if (path.startsWith("/new/gps_get")) {
			// GPS型
			NodeParent np = new NodeParent();
			np.publicKey = createKey(true);
			np.privateKey = createKey(false);
			np.mode = "gpsGet";
			np.prefix = queryMap.get("path");
			File dir = new File(filesDir, np.publicKey);
			dir.mkdirs();
			String json = gson.toJson(np);
			try {
				Files.write(new File(dir, "chain.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}
			GPSGetOptions opt = new GPSGetOptions();
			opt.address = queryMap.get("address");
			opt.title = queryMap.get("title");
			opt.message = queryMap.get("message");
			opt.close = "on".equals(queryMap.getOrDefault("close", "off"));
			json = gson.toJson(opt, GPSGetOptions.class);
			try {
				Files.write(new File(dir, "options.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}
			dir.mkdirs();
			result = CPMain.newRedirectResponse("http://" + CPMain.HOST + "/yourtrace?private=" + np.privateKey);
		}
		return result;
	}

	public Response getInfoPage(String path, String query) {
		Map<String, String> queryMap = Utils.getQueryMap(query);
		Response result = null;
		if (path.startsWith("/yourtrace")) {
			String publicKey = queryMap.get("private");
			NodeParent np = findChain(publicKey, false);
			if (np == null) {
				return null;
			}
			if (np.mode.equals("easyRedirect")) {
				String s = main.getInternalFileContent("easy_redirect_result.html");
				String url = "http://" + CPMain.HOST + "/" + np.prefix + "/" + np.publicKey;
				s = s.replace("{PUBLNK}", url).replace("{PUBLIC}", np.publicKey).replace("{SECRET}", np.privateKey);
				result = NanoHTTPD.newFixedLengthResponse(s);
			}
			if (np.mode.equals("gpsGet")) {
				String s = main.getInternalFileContent("gps_get_result.html");
				String url = "http://" + CPMain.HOST + "/" + np.prefix + "/" + np.publicKey;
				s = s.replace("{PUBLNK}", url).replace("{PUBLIC}", np.publicKey).replace("{SECRET}", np.privateKey);
				result = NanoHTTPD.newFixedLengthResponse(s);
			}
		}
		return result;
	}

	public String createKey(boolean isPublic) {
		StringBuilder sb = new StringBuilder(10);
		for (int i = 0; i < 10; i++) {
			sb.append(RANDOM_CHARS.charAt(Math.abs(sr.nextInt()) % RANDOM_CHARS.length()));
		}
		if (checkDuplication(sb.toString(), isPublic)) {
			return createKey(isPublic);
		}
		return sb.toString();
	}

	public boolean checkDuplication(String key, boolean isPublic) {
		for (File f : filesDir.listFiles()) {
			try {
				File chain = new File(f, "chain.json");
				String s = new String(Files.readAllBytes(chain.toPath()), StandardCharsets.UTF_8);
				NodeParent np = gson.fromJson(s, NodeParent.class);
				if (isPublic) {
					if (key.equals(np.publicKey)) {
						return true;
					}
				} else {
					if (key.equals(np.privateKey)) {
						return true;
					}
				}
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック

			}
		}
		return false;
	}

	public NodeParent findChain(String key, boolean isPublic) {
		for (File f : filesDir.listFiles()) {
			try {
				File chain = new File(f, "chain.json");
				String s = new String(Files.readAllBytes(chain.toPath()), StandardCharsets.UTF_8);
				NodeParent np = gson.fromJson(s, NodeParent.class);
				if (isPublic) {
					if (key.equals(np.publicKey)) {
						return np;
					}
				} else {
					if (key.equals(np.privateKey)) {
						return np;
					}
				}
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック

			}
		}
		return null;
	}

	public static class NodeParent {
		public String publicKey, privateKey;
		public String mode, prefix;
	}

	public static class EasyRedirectOptions {
		public String address;
	}

	public static class EasyRedirectSession {
		public String ip;
		public long currentMillis;
	}

	public static class GPSGetOptions {
		public String address, title, message;
		public boolean close;
	}

	public static class GPSGetSession {
		public String ip;
		public long currentMillis;
		public double longitude, latitude, altitude;
	}
}
