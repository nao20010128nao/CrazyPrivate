package com.nao20010128nao.クレイジープライベート;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class CPMain extends NanoHTTPD {
	public static final String HOST = "localhost:8080";
	DataChain dc = new DataChain(this);

	public CPMain(int port) throws IOException {
		super(port);
		// TODO 自動生成されたコンストラクター・スタブ
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
		String dir = session.getUri().replace("//", "/");
		String query = session.getQueryParameterString();
		System.out.println("Request: " + dir + (Utils.isNullString(query) ? "" : "?" + query));
		/*
		 * if ("".equals(session.getHeaders().getOrDefault("User-Agent", ""))) {
		 * return newFixedLengthResponse(Status.FORBIDDEN, "text/plain",
		 * "Attach your user-agent on the header."); }
		 */
		Response resp = unknownResponse();
		{
			if (dir.equals("/")) {
				// Top Page
				resp = getTopPage();
			}
			if (dir.equals("/robots.txt")) {
				// Top Page
				resp = getTopPage();
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

		/////
		resp.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
		resp.addHeader("Access-Control-Allow-Origin", "*");
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
			BufferedReader br = new BufferedReader(new InputStreamReader(
					getClass().getClassLoader().getResourceAsStream("com/nao20010128nao/クレイジープライベート/" + name)));
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

	Response getBTestScript() {
		Entry ent = getInternalFile("worker.js");
		return entryToResponse(ent, "text/javascript");
	}

	Entry getInternalFile(String name) {
		try {
			Entry ent = new Entry();
			InputStream is = getClass().getClassLoader().getResourceAsStream("com/nao20010128nao/クレイジープライベート/" + name);
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
}
