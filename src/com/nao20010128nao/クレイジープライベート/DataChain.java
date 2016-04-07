package com.nao20010128nao.クレイジープライベート;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.gson.Gson;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class DataChain {
	static final List<String> GPS_NULL_VALUES = Arrays.asList("undefined", "NaN", "", "null", "0", null);
	static final File FILES_DIR = new File(CPMain.CURRENT_DIRECTORY, "files");
	static final String ALPHABET_SMALL = "abcdefghijklmnopqrstuvwxyz_-";
	static final String RANDOM_CHARS = ALPHABET_SMALL + ALPHABET_SMALL.toUpperCase() + ALPHABET_SMALL
			+ ALPHABET_SMALL.toUpperCase();

	CPMain main;
	SecureRandom sr = new SecureRandom();
	Gson gson = new Gson();

	public DataChain(CPMain server) {
		// TODO 自動生成されたコンストラクター・スタブ
		main = server;
		if (!FILES_DIR.exists()) {
			FILES_DIR.mkdirs();
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
			File dir = new File(FILES_DIR, np.publicKey);
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
			result = CPMain.newRedirectResponse("http://" + main.HOST + "/yourtrace?private=" + np.privateKey);
		}
		if (path.startsWith("/new/gps_get")) {
			// GPS型
			NodeParent np = new NodeParent();
			np.publicKey = createKey(true);
			np.privateKey = createKey(false);
			np.mode = "gpsGet";
			np.prefix = queryMap.get("path");
			File dir = new File(FILES_DIR, np.publicKey);
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
			opt.gps_message = queryMap.getOrDefault("gps_message", main.text.get("gps_get.gps_message"));
			opt.gps_button = queryMap.getOrDefault("gps_button", main.text.get("gps_get.gps_button"));
			json = gson.toJson(opt, GPSGetOptions.class);
			try {
				Files.write(new File(dir, "options.json").toPath(), json.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				return null;
			}
			dir = new File(dir, "sessions");
			dir.mkdirs();
			result = CPMain.newRedirectResponse("http://" + main.HOST + "/yourtrace?private=" + np.privateKey);
		}
		if (path.startsWith("/test/gps_get")) {
			// GPS型(テストページ)
			final String title = queryMap.getOrDefault("title", "");
			final String message = queryMap.getOrDefault("message", "");
			final String gps_message = queryMap.getOrDefault("gps_message", main.text.get("gps_get.gps_message"));
			final String gps_button = queryMap.getOrDefault("gps_button", main.text.get("gps_get.gps_button"));

			String s = main.getInternalFileContent("gps_get_test.html");
			Document doc = Jsoup.parse(s);
			doc.select("title").get(0).text(title);
			doc.select("h2.title").get(0).text(title);
			doc.select("div>h3").get(0).text(message);
			doc.select("p.reqire_gps").get(0).text(gps_message);
			doc.select("button#gps_get").get(0).text(gps_button);
			s = doc.html();

			result = NanoHTTPD.newFixedLengthResponse(s);
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
				String url = "http://" + main.HOST + "/" + np.prefix + "/" + np.publicKey;
				s = s.replace("{PUBLNK}", url).replace("{PUBLIC}", np.publicKey).replace("{SECRET}", np.privateKey);
				result = NanoHTTPD.newFixedLengthResponse(s);
			}
			if (np.mode.equals("gpsGet")) {
				String s = main.getInternalFileContent("gps_get_result.html");
				String url = "http://" + main.HOST + "/" + np.prefix + "/" + np.publicKey;
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
			String urlPrefix = path.split("\\/")[1];
			if (!np.prefix.equals(urlPrefix)) {
				return null;
			}
			if (np.mode.equals("easyRedirect")) {
				String json;
				File dir = new File(FILES_DIR, np.publicKey);
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
				dir = new File(dir, ers.currentMillis + ".json");
				json = gson.toJson(ers);
				try {
					Files.write(dir.toPath(), json.getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
				}
				result = CPMain.newRedirectResponse(ero.address);
			}
			if (np.mode.equals("gpsGet")) {
				String json;
				File dir = new File(FILES_DIR, np.publicKey);
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
				ggs.done = false;
				dir = new File(dir, "sessions");
				dir = new File(dir, ggs.currentMillis + ".json");
				json = gson.toJson(ggs);
				try {
					Files.write(dir.toPath(), json.getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
				}

				String s = main.getInternalFileContent("gps_get_trappage.html");
				Document doc = Jsoup.parse(s);
				doc.select("title").get(0).text(ggo.title);
				doc.select("h2").get(0).text(ggo.title);
				doc.select("div>h3").get(0).text(ggo.message);
				doc.select("p.reqire_gps").get(0).text(ggo.gps_message);
				doc.select("button#gps_get").get(0).text(ggo.gps_button);
				doc.select("form").get(0).attr("action", doc.select("form").get(0).attr("action")
						.replace("{TIME}", ggs.currentMillis + "").replace("{PUBLIC}", np.publicKey));
				s = doc.html();

				result = NanoHTTPD.newFixedLengthResponse(s);
			}
		}
		return result;
	}

	public Response secondarySession(String path, String query, IHTTPSession session) {
		Map<String, String> queryMap = Utils.getQueryMap(query);
		Response result = null;
		if (path.startsWith("/submit/")) {
			String[] splitted = path.split("\\/");
			String publicKey = splitted[2];
			String sessionID = splitted[3];
			File traceDir = new File(FILES_DIR, publicKey);
			File sessionFile = new File(new File(traceDir, "sessions"), sessionID + ".json");
			if (sessionFile.exists()) {
				String json, json2;
				try {
					json = new String(Files.readAllBytes(sessionFile.toPath()), StandardCharsets.UTF_8);
					json2 = new String(Files.readAllBytes(new File(traceDir, "options.json").toPath()),
							StandardCharsets.UTF_8);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
				GPSGetOptions ggo = gson.fromJson(json2, GPSGetOptions.class);
				GPSGetSession ggs = gson.fromJson(json, GPSGetSession.class);
				if (!ggs.done) {
					{
						DoubleValue dv = new DoubleValue();
						String tmp = queryMap.getOrDefault("latitude", "NaN");
						if (GPS_NULL_VALUES.contains(tmp)) {
							dv.value = 0;
							dv.NaN = true;
						} else {
							dv.value = new Double(tmp);
							dv.NaN = false;
						}
						ggs.latitude = dv;
					}
					{
						DoubleValue dv = new DoubleValue();
						String tmp = queryMap.getOrDefault("longitude", "NaN");
						if (GPS_NULL_VALUES.contains(tmp)) {
							dv.value = 0;
							dv.NaN = true;
						} else {
							dv.value = new Double(tmp);
							dv.NaN = false;
						}
						ggs.longitude = dv;
					}
					{
						DoubleValue dv = new DoubleValue();
						String tmp = queryMap.getOrDefault("altitude", "NaN");
						if (GPS_NULL_VALUES.contains(tmp)) {
							dv.value = 0;
							dv.NaN = true;
						} else {
							dv.value = new Double(tmp);
							dv.NaN = false;
						}
						ggs.altitude = dv;
					}
					{
						DoubleValue dv = new DoubleValue();
						String tmp = queryMap.getOrDefault("accuracy", "NaN");
						if (GPS_NULL_VALUES.contains(tmp)) {
							dv.value = 0;
							dv.NaN = true;
						} else {
							dv.value = new Double(tmp);
							dv.NaN = false;
						}
						ggs.accuracy = dv;
					}
					{
						DoubleValue dv = new DoubleValue();
						String tmp = queryMap.getOrDefault("altitudeAccuracy", "NaN");
						if (GPS_NULL_VALUES.contains(tmp)) {
							dv.value = 0;
							dv.NaN = true;
						} else {
							dv.value = new Double(tmp);
							dv.NaN = false;
						}
						ggs.altitudeAccuracy = dv;
					}
					{
						DoubleValue dv = new DoubleValue();
						String tmp = queryMap.getOrDefault("heading", "NaN");
						if (GPS_NULL_VALUES.contains(tmp)) {
							dv.value = 0;
							dv.NaN = true;
						} else {
							dv.value = new Double(tmp);
							dv.NaN = false;
						}
						ggs.heading = dv;
					}
					{
						DoubleValue dv = new DoubleValue();
						String tmp = queryMap.getOrDefault("speed", "NaN");
						if (GPS_NULL_VALUES.contains(tmp)) {
							dv.value = 0;
							dv.NaN = true;
						} else {
							dv.value = new Double(tmp);
							dv.NaN = false;
						}
						ggs.speed = dv;
					}
					if (ggs.latitude.NaN & ggs.longitude.NaN & ggs.altitude.NaN & ggs.accuracy.NaN
							& ggs.altitudeAccuracy.NaN & ggs.heading.NaN & ggs.speed.NaN) {
						String joined = queryMap.getOrDefault("joined", "$$$$$$");
						String[] data = joined.split("\\$");
						{
							DoubleValue dv = new DoubleValue();
							String tmp = data[0];
							if (GPS_NULL_VALUES.contains(tmp)) {
								dv.value = 0;
								dv.NaN = true;
							} else {
								dv.value = new Double(tmp);
								dv.NaN = false;
							}
							ggs.latitude = dv;
						}
						{
							DoubleValue dv = new DoubleValue();
							String tmp = data[1];
							if (GPS_NULL_VALUES.contains(tmp)) {
								dv.value = 0;
								dv.NaN = true;
							} else {
								dv.value = new Double(tmp);
								dv.NaN = false;
							}
							ggs.longitude = dv;
						}
						{
							DoubleValue dv = new DoubleValue();
							String tmp = data[2];
							if (GPS_NULL_VALUES.contains(tmp)) {
								dv.value = 0;
								dv.NaN = true;
							} else {
								dv.value = new Double(tmp);
								dv.NaN = false;
							}
							ggs.altitude = dv;
						}
						{
							DoubleValue dv = new DoubleValue();
							String tmp = data[3];
							if (GPS_NULL_VALUES.contains(tmp)) {
								dv.value = 0;
								dv.NaN = true;
							} else {
								dv.value = new Double(tmp);
								dv.NaN = false;
							}
							ggs.accuracy = dv;
						}
						{
							DoubleValue dv = new DoubleValue();
							String tmp = data[4];
							if (GPS_NULL_VALUES.contains(tmp)) {
								dv.value = 0;
								dv.NaN = true;
							} else {
								dv.value = new Double(tmp);
								dv.NaN = false;
							}
							ggs.altitudeAccuracy = dv;
						}
						{
							DoubleValue dv = new DoubleValue();
							String tmp = data[5];
							if (GPS_NULL_VALUES.contains(tmp)) {
								dv.value = 0;
								dv.NaN = true;
							} else {
								dv.value = new Double(tmp);
								dv.NaN = false;
							}
							ggs.heading = dv;
						}
						{
							DoubleValue dv = new DoubleValue();
							String tmp = data[6];
							if (GPS_NULL_VALUES.contains(tmp)) {
								dv.value = 0;
								dv.NaN = true;
							} else {
								dv.value = new Double(tmp);
								dv.NaN = false;
							}
							ggs.speed = dv;
						}
					}
					ggs.done = true;
					json2 = gson.toJson(ggs);
					try {
						Files.write(sessionFile.toPath(), json2.getBytes());
					} catch (IOException e) {
					}
				}
				if (ggo.close) {
					result = NanoHTTPD.newFixedLengthResponse("CLOSE_WEBPAGE");
				} else {
					result = NanoHTTPD.newFixedLengthResponse(ggo.address);
				}
			} else {
				System.err.println("File does not exist");
			}
		}
		return result;
	}

	public Response manageConsole(String path, String query) {
		Map<String, String> queryMap = Utils.getQueryMap(query);
		Response result = null;
		if (path.startsWith("/console/home")) {
			boolean edited = new Predicate<String>() {
				@Override
				public boolean test(String t) {
					// TODO 自動生成されたメソッド・スタブ
					if (t == null) {
						return false;
					}
					if ("true".equals(t)) {
						return true;
					}
					return false;
				}
			}.test(queryMap.get("edited"));
			NodeParent np = findChain(queryMap.get("secret"), false);
			if (np == null) {
				result = NanoHTTPD.newFixedLengthResponse(main.getInternalFileContent("manage_error_notfound.html"));
			} else {
				String publnk = "http://" + main.HOST + "/" + np.prefix + "/" + np.publicKey;
				Document doc = Jsoup
						.parse(main.getInternalFileContent("manage_home" + (edited ? "_edited" : "") + ".html"));
				doc.select("form.frame>div>div>input#secret").get(0).attr("value", np.privateKey);
				doc.select("div>div.frame>div>form#edit_form>input[name=\"secret\"]").get(0).attr("value",
						np.privateKey);
				doc.select("div>div.frame>div>form#delete_form>input[name=\"secret\"]").get(0).attr("value",
						np.privateKey);
				doc.select("form.frame>div>div>input#public").get(0).attr("value", np.publicKey);
				doc.select("form.frame>div>div>input#publnk").get(0).attr("value", publnk);
				{
					File sessionsDir = new File(new File(FILES_DIR, np.publicKey), "sessions");
					Element history = doc.select("div#history").get(0);
					for (File session : sessionsDir.listFiles()) {
						Element section = null;
						try {
							if (np.mode.equals("easyRedirect")) {
								EasyRedirectSession ers;
								try {
									ers = gson.fromJson(
											new String(Files.readAllBytes(session.toPath()), StandardCharsets.UTF_8),
											EasyRedirectSession.class);
								} catch (Throwable e) {
									// TODO 自動生成された catch ブロック
									continue;
								}
								section = Jsoup.parseBodyFragment(
										main.getInternalFileContent("fragment_easy_redirect_session.html"));
								Element date = section.select("div>p#date").get(0);
								Element ip = section.select("div>p#ip").get(0);
								{
									Calendar calendar = Calendar.getInstance();
									calendar.setTimeInMillis(ers.currentMillis);
									LocalDateTime ldt = LocalDateTime.of(calendar.get(Calendar.YEAR),
											calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
											calendar.get(Calendar.HOUR)
													+ (calendar.get(Calendar.AM_PM) == Calendar.PM ? 12 : 0),
											calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
									DateTimeFormatter dtf = DateTimeFormatter
											.ofPattern(main.text.get("console.edit.date_format"));
									date.text(date.text().replace("{DATE}", ldt.format(dtf)));
								}
								ip.text(ip.text().replace("{ADDR}", ers.ip));
							}
							if (np.mode.equals("gpsGet")) {
								GPSGetSession ggs;
								try {
									ggs = gson.fromJson(
											new String(Files.readAllBytes(session.toPath()), StandardCharsets.UTF_8),
											GPSGetSession.class);
								} catch (Throwable e) {
									// TODO 自動生成された catch ブロック
									continue;
								}
								section = Jsoup.parseBodyFragment(
										main.getInternalFileContent("fragment_get_gps_session.html"));
								Element date = section.select("div>p#date").get(0);
								Element ip = section.select("div>p#ip").get(0);

								Element latitude = section.select("div>p#latitude").get(0);
								Element longitude = section.select("div>p#longitude").get(0);
								Element altitude = section.select("div>p#altitude").get(0);
								Element accuracy = section.select("div>p#accuracy").get(0);
								Element altitudeAccuracy = section.select("div>p#altitudeAccuracy").get(0);
								Element heading = section.select("div>p#heading").get(0);
								Element speed = section.select("div>p#speed").get(0);
								{
									Calendar calendar = Calendar.getInstance();
									calendar.setTimeInMillis(ggs.currentMillis);
									LocalDateTime ldt = LocalDateTime.of(calendar.get(Calendar.YEAR),
											calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
											calendar.get(Calendar.HOUR)
													+ (calendar.get(Calendar.AM_PM) == Calendar.PM ? 12 : 0),
											calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
									DateTimeFormatter dtf = DateTimeFormatter
											.ofPattern(main.text.get("console.edit.date_format"));
									date.text(date.text().replace("{VALUE}", ldt.format(dtf)));
								}
								ip.text(ip.text().replace("{VALUE}", ggs.ip));

								latitude.text(latitude.text().replace("{VALUE}", ggs.latitude.toString()));
								longitude.text(longitude.text().replace("{VALUE}", ggs.longitude.toString()));
								altitude.text(altitude.text().replace("{VALUE}", ggs.altitude.toString()));
								accuracy.text(accuracy.text().replace("{VALUE}", ggs.accuracy.toString()));
								altitudeAccuracy.text(
										altitudeAccuracy.text().replace("{VALUE}", ggs.altitudeAccuracy.toString()));
								heading.text(heading.text().replace("{VALUE}", ggs.heading.toString()));
								speed.text(speed.text().replace("{VALUE}", ggs.speed.toString()));
							}
						} catch (Throwable e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}

						if (section != null) {
							history.appendChild(section.select("div").get(0));
						}
					}
				}
				result = NanoHTTPD.newFixedLengthResponse(doc.html());
			}
		}
		if (path.startsWith("/console/edit")) {
			NodeParent np = findChain(queryMap.get("secret"), false);
			if (np == null) {
				result = NanoHTTPD.newFixedLengthResponse(main.getInternalFileContent("manage_error_notfound.html"));
			} else {
				File chainDir = new File(FILES_DIR, np.publicKey);
				if (np.mode.equals("easyRedirect")) {
					EasyRedirectOptions ero;
					try {
						ero = gson.fromJson(new String(Files.readAllBytes(new File(chainDir, "options.json").toPath())),
								EasyRedirectOptions.class);
					} catch (Throwable e) {
						return null;
					}
					Document editDoc = Jsoup.parse(main.getInternalFileContent("manage_edit_easy_redirect.html"));
					editDoc.select("form.content>input[name=\"address\"]").get(0).attr("value", ero.address);

					result = NanoHTTPD.newFixedLengthResponse(editDoc.html());
				}
				if (np.mode.equals("gpsGet")) {
					GPSGetOptions ggo;
					try {
						ggo = gson.fromJson(new String(Files.readAllBytes(new File(chainDir, "options.json").toPath())),
								GPSGetOptions.class);
					} catch (Throwable e) {
						return null;
					}
					Document editDoc = Jsoup.parse(main.getInternalFileContent("manage_edit_gps_get.html"));
					editDoc.select("form.content>input[name=\"address\"]").get(0).attr("value", ggo.address);
					editDoc.select("form.content>div>input[name=\"close\"]").get(0).attr("checked", ggo.close);
					editDoc.select("form.content>select[name=\"path\"]>option[value=\"" + np.prefix + "\"]").get(0)
							.attr("selected", true);
					editDoc.select("form.content>input[name=\"title\"]").get(0).attr("value", ggo.title);
					editDoc.select("form.content>textarea[name=\"message\"]").get(0).text(ggo.message);
					editDoc.select("form.content>textarea[name=\"gps_message\"]").get(0).text(ggo.gps_message);
					editDoc.select("form.content>input[name=\"gps_button\"]").get(0).attr("value", ggo.gps_button);

					editDoc.select("form.content>input[name=\"secret\"]").get(0).attr("value", np.privateKey);

					result = NanoHTTPD.newFixedLengthResponse(editDoc.html());
				}
			}
		}
		if (path.startsWith("/console/apply")) {
			NodeParent np = findChain(queryMap.get("secret"), false);
			if (np == null) {
				result = NanoHTTPD.newFixedLengthResponse(main.getInternalFileContent("manage_error_notfound.html"));
			} else {
				File chainDir = new File(FILES_DIR, np.publicKey);
				if (np.mode.equals("easyRedirect")) {
					EasyRedirectOptions ero;
					try {
						ero = gson.fromJson(new String(Files.readAllBytes(new File(chainDir, "options.json").toPath())),
								EasyRedirectOptions.class);
					} catch (Throwable e) {
						return null;
					}
					np.prefix = queryMap.getOrDefault("path", np.prefix);
					ero.address = queryMap.getOrDefault("address", ero.address);

					try {
						Files.write(new File(chainDir, "options.json").toPath(),
								gson.toJson(ero).getBytes(StandardCharsets.UTF_8));
					} catch (IOException e) {
						return null;
					}

					result = CPMain.newRedirectResponse(
							"http://" + main.HOST + "/console/home?secret=" + np.privateKey + "&edited=true");
				}
				if (np.mode.equals("gpsGet")) {
					GPSGetOptions ggo;
					try {
						ggo = gson.fromJson(new String(Files.readAllBytes(new File(chainDir, "options.json").toPath())),
								GPSGetOptions.class);
					} catch (Throwable e) {
						return null;
					}
					np.prefix = queryMap.getOrDefault("path", np.prefix);
					ggo.address = queryMap.getOrDefault("address", ggo.address);
					ggo.title = queryMap.getOrDefault("title", ggo.title);
					ggo.message = queryMap.getOrDefault("message", ggo.message);
					ggo.close = "on".equals(queryMap.getOrDefault("close", "off"));
					ggo.gps_message = queryMap.getOrDefault("gps_message", ggo.gps_message);
					ggo.gps_button = queryMap.getOrDefault("gps_button", ggo.gps_button);

					try {
						Files.write(new File(chainDir, "options.json").toPath(),
								gson.toJson(ggo).getBytes(StandardCharsets.UTF_8));
					} catch (IOException e) {
						return null;
					}

					result = CPMain.newRedirectResponse(
							"http://" + main.HOST + "/console/home?secret=" + np.privateKey + "&edited=true");
				}
			}
		}
		if (path.startsWith("/console/delete")) {
			NodeParent np = findChain(queryMap.get("secret"), false);
			if (np == null) {
				result = NanoHTTPD.newFixedLengthResponse(main.getInternalFileContent("manage_error_notfound.html"));
			} else {
				File chainDir = new File(FILES_DIR, np.publicKey);

				new File(chainDir, "chain.json").delete();
				new File(chainDir, "options.json").delete();

				File sessionsDir = new File(chainDir, "sessions");
				for (File session : sessionsDir.listFiles()) {
					session.delete();
				}

				sessionsDir.delete();
				chainDir.delete();

				result = NanoHTTPD.newFixedLengthResponse(main.getInternalFileContent("manage_delete.html"));
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
		for (File f : FILES_DIR.listFiles()) {
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
		for (File f : FILES_DIR.listFiles()) {
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
		public DoubleValue latitude, longitude, altitude, accuracy, altitudeAccuracy, heading, speed;
		public boolean done;
	}

	public static class DoubleValue {
		public double value;
		public boolean NaN = true;

		@Override
		public String toString() {
			// TODO 自動生成されたメソッド・スタブ
			if (NaN)
				return "NaN";
			else
				return value + "";
		}

		@Override
		public boolean equals(Object obj) {
			// TODO 自動生成されたメソッド・スタブ
			if (obj instanceof DoubleValue) {
				if (NaN & ((DoubleValue) obj).NaN)
					return false;// NaN == NaN -> false
				if (!(NaN ^ ((DoubleValue) obj).NaN))
					return false;// NaN == value -> false
				return value == ((DoubleValue) obj).value;
			}
			return false;
		}
	}
}
