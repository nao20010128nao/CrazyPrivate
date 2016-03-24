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
	CPMain main;
	SecureRandom sr = new SecureRandom();
	static File filesDir = new File("files");
	static final String ALPHABET_SMALL = "abcdefghijklmnopqrstuvwxyz_-";
	static final String RANDOM_CHARS = ALPHABET_SMALL + ALPHABET_SMALL.toUpperCase() + ALPHABET_SMALL
			+ ALPHABET_SMALL.toUpperCase();

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
			String json = new Gson().toJson(np, NodeParent.class);
			try {
				Files.write(new File(dir, "chain.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}
			EasyRedirectOptions opt = new EasyRedirectOptions();
			opt.address = queryMap.get("address");
			json = new Gson().toJson(opt, EasyRedirectOptions.class);
			try {
				Files.write(new File(dir, "options.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}
			dir = new File(dir, "sessions");
			dir.mkdirs();
			String s = main.getInternalFileContent("easy_redirect_result.html");
			String url = "http://" + CPMain.HOST + "/" + np.prefix + "/" + np.publicKey;
			s = s.replace("{PUBLNK}", url).replace("{PUBLIC}", np.publicKey).replace("{SECRET}", np.privateKey);
			result = NanoHTTPD.newFixedLengthResponse(s);
		}
		if (path.startsWith("/new/gps_get")) {
			// GPS型
			NodeParent np = new NodeParent();
			np.publicKey = createKey(true);
			np.privateKey = createKey(false);
			np.mode = "gpsGet";
			File dir = new File(filesDir, np.publicKey);
			dir.mkdirs();
			String json = new Gson().toJson(np);
			try {
				Files.write(new File(dir, "chain.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}

			dir = new File(dir, "sessions");
			dir.mkdirs();
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
		return false;
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
}
