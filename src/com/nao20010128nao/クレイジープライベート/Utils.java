package com.nao20010128nao.クレイジープライベート;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Utils {
	public static Map<String, String> getQueryMap(String query) {
		// Map<String, String> map =
		// Utils.getQueryMap("value=test=valuee&value2=test");
		// map.entrySet().stream().forEach(ent -> {
		// System.out.println(ent.getKey());
		// System.out.println(ent.getValue());
		// });
		if (query == null) {
			return Collections.emptyMap();
		}
		if (query.startsWith("?")) {
			query = query.substring(1);
		}
		String[] splitted = query.split("\\&");
		Map<String, String> result = new HashMap<>(splitted.length);
		for (String splittedValue : splitted) {
			if (splittedValue.indexOf('=') == -1) {
				continue;
			}
			String key = decodePercEnc(splittedValue.substring(0, splittedValue.indexOf('=')));
			String value = decodePercEnc(
					splittedValue.substring(splittedValue.indexOf('=') + 1, splittedValue.length()));
			result.put(key, value);
		}
		return result;
	}

	public static boolean isNullString(String s) {
		return s == null || "".equals(s);
	}

	public static String decodePercEnc(String s) {
		String r;
		try {
			r = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			r = null;
		}
		return r;
	}
}
