package com.nao20010128nao.クレイジープライベート;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class CPMain extends NanoHTTPD {

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
		System.out.println("Request: " + dir);
		Response resp = newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "");
		{
			if (dir.equals("/")) {
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

		/////
		resp.addHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
		resp.addHeader("Access-Control-Allow-Origin", "*");
		return resp;
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
