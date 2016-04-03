package com.nao20010128nao.クレイジープライベート;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ServerSocketFactory;

public class WrappingServerSockFactory extends ServerSocketFactory
		implements fi.iki.elonen.NanoHTTPD.ServerSocketFactory {
	ServerSocketFactory ssf;
	int curPort = 49152;

	public WrappingServerSockFactory(ServerSocketFactory servSockFact) {
		// TODO 自動生成されたコンストラクター・スタブ
		ssf = servSockFact;
	}

	@Override
	public ServerSocket create() throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		for (; curPort < 65535; curPort++) {
			try {
				return ssf.createServerSocket(curPort);
			} catch (Exception e) {
			}
		}
		curPort = 49152;
		return ssf.createServerSocket();
	}

	@Override
	public ServerSocket createServerSocket(int paramInt) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return ssf.createServerSocket(paramInt);
	}

	@Override
	public ServerSocket createServerSocket(int paramInt1, int paramInt2) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return ssf.createServerSocket(paramInt1, paramInt2);
	}

	@Override
	public ServerSocket createServerSocket(int paramInt1, int paramInt2, InetAddress paramInetAddress)
			throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return ssf.createServerSocket(paramInt1, paramInt2, paramInetAddress);
	}

}
