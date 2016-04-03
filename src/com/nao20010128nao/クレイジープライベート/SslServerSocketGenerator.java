package com.nao20010128nao.クレイジープライベート;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public class SslServerSocketGenerator {
	public static ServerSocketFactory generate(SSLInfo info) {
		try {
			if (!info.enabled) {
				return ServerSocketFactory.getDefault();
			}
			KeyStore ks = KeyStore.getInstance(info.keyStoreType);
			char[] ksPass = info.keyStorePassword.toCharArray();
			ks.load(new FileInputStream(info.keyStorePath), ksPass);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, ksPass);
			SSLContext sslCtx = SSLContext.getInstance(info.sslType);
			sslCtx.init(kmf.getKeyManagers(), null, null);
			return sslCtx.getServerSocketFactory();
		} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException e) {
			// TODO 自動生成された catch ブロック
			return ServerSocketFactory.getDefault();
		}
	}

	public static class SSLInfo {
		public boolean enabled = false;
		public String keyStoreType = "JKS";
		public String keyStorePassword = "password";
		public String keyStorePath = "filepath";
		public String keyManagerFacType = "SunX509";
		public String sslType = "TLS";
	}
}
