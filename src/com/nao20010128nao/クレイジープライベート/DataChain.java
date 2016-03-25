package com.nao20010128nao.クレイジープライベート;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
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
			opt.address = queryMap.getOrDefault("address", "");
			opt.title = queryMap.getOrDefault("title", "");
			opt.message = queryMap.getOrDefault("message", "");
			opt.close = "on".equals(queryMap.getOrDefault("close", "off"));
			opt.gps_message = queryMap.getOrDefault("gps_message", "続行するにはあなたの現在地情報が必要です。");
			opt.gps_button = queryMap.getOrDefault("gps_button", "続行");
			json = gson.toJson(opt, GPSGetOptions.class);
			try {
				Files.write(new File(dir, "options.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}
			dir = new File(dir, "sessions");
			dir.mkdirs();
			result = CPMain.newRedirectResponse("http://" + CPMain.HOST + "/yourtrace?private=" + np.privateKey);
		}
		if (path.startsWith("/test/gps_get")) {
			// GPS型(テストページ)
			try {
				final String title = queryMap.getOrDefault("title", "");
				final String message = queryMap.getOrDefault("message", "");
				final String gps_message = queryMap.getOrDefault("gps_message", "続行するにはあなたの現在地情報が必要です。");
				final String gps_button = queryMap.getOrDefault("gps_button", "続行");

				String s = main.getInternalFileContent("gps_get_test.html");
				Document doc = Jsoup.parse(s);
				doc.select("title").get(0).text(title);
				doc.select("h2.title").get(0).text(title);
				doc.select("div>h3").get(0).text(message);
				doc.select("p.reqire_gps").get(0).text(gps_message);
				doc.select("button#gps_get").get(0).text(gps_button);
				s = doc.html();

				result = NanoHTTPD.newFixedLengthResponse(s);
			} catch (Throwable e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
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

	public Response startSession(String path, String query, IHTTPSession session) {
		Map<String, String> queryMap = Utils.getQueryMap(query);
		Response result = null;
		if (path.startsWith("/photo") || path.startsWith("/image") || path.startsWith("/images")
				|| path.startsWith("/video") || path.startsWith("/videos") || path.startsWith("/download")
				|| path.startsWith("/webpage") || path.startsWith("/website") || path.startsWith("/homepage")
				|| path.startsWith("/patch")) {
			String publicKey = path.split("\\/")[2];
			NodeParent np = findChain(publicKey, true);
			if (np == null) {
				return null;
			}
			if (np.mode.equals("easyRedirect")) {
				String json;
				File dir = new File(filesDir, np.publicKey);
				try {
					json = new String(Files.readAllBytes(new File(dir, "options.json").toPath()),
							StandardCharsets.UTF_8);
				} catch (IOException e1) {
					return null;
				}
				EasyRedirectOptions ero = gson.fromJson(json, EasyRedirectOptions.class);

				EasyRedirectSession ers = new EasyRedirectSession();
				ers.currentMillis = System.currentTimeMillis();
				ers.ip = session.getHeaders().getOrDefault("remote-addr", "127.0.0.1");
				dir = new File(dir, "sessions");
				dir = new File(dir, System.currentTimeMillis() + ".json");
				json = gson.toJson(ers);
				try {
					Files.write(dir.toPath(), json.getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
				}
				result = CPMain.newRedirectResponse(ero.address);
			}
			if (np.mode.equals("gpsGet")) {
				String json;
				File dir = new File(filesDir, np.publicKey);
				try {
					json = new String(Files.readAllBytes(new File(dir, "options.json").toPath()),
							StandardCharsets.UTF_8);
				} catch (IOException e1) {
					return null;
				}
				GPSGetOptions ggo = gson.fromJson(json, GPSGetOptions.class);

				GPSGetSession ggs = new GPSGetSession();
				ggs.currentMillis = System.currentTimeMillis();
				ggs.ip = session.getHeaders().getOrDefault("remote-addr", "127.0.0.1");
				dir = new File(dir, "sessions");
				dir = new File(dir, System.currentTimeMillis() + ".json");
				json = gson.toJson(ggs);
				try {
					Files.write(dir.toPath(), json.getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
				}

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
		public String address, title, message, gps_message, gps_button;
		public boolean close;
	}

	public static class GPSGetSession {
		public String ip;
		public long currentMillis;
		public double longitude, latitude, altitude;
	}
}
