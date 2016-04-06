package com.nao20010128nao.クレイジープライベート;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import com.google.gson.Gson;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class CPMain extends NanoHTTPD {
	public static File CURRENT_DIRECTORY = new File(System.getProperty("user.dir"));
	public static final List<String> ACCEPTED_LANGUAGES = Arrays.asList("ja", "en");
	public final String HOST;
	DataChain dc = new DataChain(this);
	Config cfg;
	Gson gson = new Gson();
	File configDir = new File(new File(CURRENT_DIRECTORY, "files"), "config.json");
	SslServerSocketGenerator.SSLInfo sslInfo;
	public final String lang;
	public final CustomMap text;

	public CPMain(int port) throws IOException {
		super(port);
		// TODO 自動生成されたコンストラクター・スタブ
		if (configDir.exists()) {
			System.out.println("Loading config...");
			try {
				cfg = gson.fromJson(new String(Files.readAllBytes(configDir.toPath()), "UTF-8"), Config.class);
			} catch (Throwable e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
				System.err.println("Error!");
				cfg = new Config();
			}
		} else {
			cfg = new Config();
		}
		try {
			Files.write(configDir.toPath(), gson.toJson(cfg).getBytes(StandardCharsets.UTF_8));
		} catch (Throwable e) {

		}
		lang = cfg.lang;
		HOST = cfg.host;
		sslInfo = cfg.sslInfo;
		if (sslInfo.enabled) {
			setServerSocketFactory(new WrappingServerSockFactory(SslServerSocketGenerator.generate(sslInfo)));
		}
		text = gson.fromJson(getInternalFileContent("defaults.json"), CustomMap.class);
		if (!ACCEPTED_LANGUAGES.contains(lang)) {
			System.err.println("Unsupported language: " + lang);
			System.exit(1);
		}
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
	}

	public static void main(String[] args) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		OptionParser op = new OptionParser();
		op.accepts("port").withRequiredArg();
		OptionSet os = op.parse(args);
		int port = 8080;
		if (os.has("port")) {
			port = new Integer(os.valueOf("port") + "");
		}
		new CPMain(port);
	}

	@Override
	public Response serve(IHTTPSession session) {
		// TODO 自動生成されたメソッド・スタブ
		Response resp;
		try {
			if (InetAddress.getByName(session.getHeaders().get("remote-addr")).getHostAddress().toLowerCase()
					.contains("google")) {
				return NanoHTTPD.newFixedLengthResponse("Connecting from Google is restricted.");
			}
			if (InetAddress.getByName(session.getHeaders().get("remote-addr")).getHostAddress().toLowerCase()
					.contains("tor-exit-node")) {
				return NanoHTTPD.newFixedLengthResponse("Connecting from Tor is restricted.");
			}
			String dir = session.getUri().replace("//", "/");
			String query = session.getQueryParameterString();
			System.out.println("Request: " + dir + (Utils.isNullString(query) ? "" : "?" + query));
			resp = unknownResponse();
			{
				if (dir.equals("/")) {
					// Top Page
					resp = getTopPage();
				}
				if (dir.equals("/robots.txt")) {
					// robots.txt
					resp = getTopPage();
				}
				if (dir.equals("/close")) {
					// robots.txt
					resp = getClosePage();
				}
			}
			{
				if (dir.equals("/blogcopies/browsertest.html")) {
					// Browser Test
					resp = getBTestHome();
				}
				if (dir.equals("/blogcopies/worker.js")) {
					// Browser Test
					resp = getBTestScript();
				}
			}
			{
				if (dir.startsWith("/new/")) {
					resp = dc.newChain(dir, query);
					if (resp == null) {
						resp = unknownResponse();
					}
				}
				if (dir.startsWith("/test/gps_get")) {
					resp = dc.newChain(dir, query);
					if (resp == null) {
						resp = unknownResponse();
					}
				}
				if (dir.startsWith("/yourtrace")) {
					resp = dc.getInfoPage(dir, query);
					if (resp == null) {
						resp = unknownResponse();
					}
				}
				if (dir.startsWith("/submit/")) {
					resp = dc.secondarySession(dir, query, session);
					if (resp == null) {
						resp = unknownResponse();
					}
				}
				if (dir.startsWith("/console/")) {
					resp = dc.manageConsole(dir, query);
					if (resp == null) {
						resp = unknownResponse();
					}
				}

				if (dir.startsWith("/photo") || dir.startsWith("/image") || dir.startsWith("/images")
						|| dir.startsWith("/video") || dir.startsWith("/videos") || dir.startsWith("/download")
						|| dir.startsWith("/webpage") || dir.startsWith("/website") || dir.startsWith("/homepage")
						|| dir.startsWith("/patch")) {
					resp = dc.startSession(dir, query, session);
					if (resp == null) {
						resp = unknownResponse();
					}
				}
			}
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			resp = unknownResponse();
		}

		/////
		resp.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Cache-Control", "no-cache");
		resp.addHeader("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
		return resp;
	}

	public Response unknownResponse() {
		// TODO 自動生成されたメソッド・スタブ
		return newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "");
	}

	public static Response newRedirectResponse(String destination) {
		Response resp = newFixedLengthResponse("");
		resp.setStatus(Status.REDIRECT);
		resp.setMimeType("application/octet-stream");
		resp.addHeader("Location", destination);
		return resp;
	}

	public String getInternalFileContent(String name) {
		try {
			BufferedReader br;
			try {
				br = new BufferedReader(
						new InputStreamReader(
								getClass().getClassLoader().getResourceAsStream(
										"com/nao20010128nao/クレイジープライベート/lang/" + lang + "/" + name),
								StandardCharsets.UTF_8));
			} catch (Throwable e) {
				br = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
						"com/nao20010128nao/クレイジープライベート/lang/common/" + name), StandardCharsets.UTF_8));
			}
			StringWriter sw = new StringWriter();
			char[] bs = new char[100000];
			int r;
			while (true) {
				r = br.read(bs);
				if (r <= 0) {
					break;
				}
				sw.write(bs, 0, r);
			}
			return sw.toString();
		} catch (IOException e) {
			return null;
		}
	}

	Response getRobotsTxt() {
		Entry ent = getInternalFile("robots.txt");
		return entryToResponse(ent);
	}

	Response getTopPage() {
		Entry ent = getInternalFile("toppage.html");
		return entryToResponse(ent);
	}

	Response getBTestHome() {
		Entry ent = getInternalFile("browsertest.html");
		return entryToResponse(ent);
	}

	Response getClosePage() {
		Entry ent = getInternalFile("close.html");
		return entryToResponse(ent);
	}

	Response getBTestScript() {
		Entry ent = getInternalFile("worker.js");
		return entryToResponse(ent, "text/javascript");
	}

	Entry getInternalFile(String name) {
		try {
			Entry ent = new Entry();
			InputStream is;
			try {
				is = getClass().getClassLoader()
						.getResourceAsStream("com/nao20010128nao/クレイジープライベート/lang/" + lang + "/" + name);
				Objects.requireNonNull(is);
			} catch (Throwable e) {
				is = getClass().getClassLoader()
						.getResourceAsStream("com/nao20010128nao/クレイジープライベート/lang/common/" + name);
			}
			ByteArrayOutputStream bais = new ByteArrayOutputStream();
			byte[] bs = new byte[100000];
			int r;
			while (true) {
				r = is.read(bs);
				if (r <= 0) {
					break;
				}
				bais.write(bs, 0, r);
			}
			ent.size = bais.size();
			ent.stream = new ByteArrayInputStream(bais.toByteArray());
			return ent;
		} catch (IOException e) {
			return null;
		}
	}

	Response entryToResponse(Entry ent) {
		return newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, ent.stream, ent.size);
	}

	Response entryToResponse(Entry ent, String mime) {
		return newFixedLengthResponse(Status.OK, mime, ent.stream, ent.size);
	}

	class Entry {
		long size;
		InputStream stream;
	}

	public static class Config {
		public String host = "localhost:8080";
		public String lang = "ja";
		public SslServerSocketGenerator.SSLInfo sslInfo = new SslServerSocketGenerator.SSLInfo();
	}

	public static class CustomMap extends HashMap<String, String> {
	}
}
