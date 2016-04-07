package com.nao20010128nao.クレイジープライベート.commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

import com.google.gson.Gson;
import com.nao20010128nao.クレイジープライベート.CPMain;
import com.nao20010128nao.クレイジープライベート.DataChain;

public class InfoCommand extends CommandBase {
	CPMain server;
	Gson gson = new Gson();

	public InfoCommand(CPMain server) {
		// TODO 自動生成されたコンストラクター・スタブ
		super(server);
		this.server = server;
	}

	@Override
	public String getCommandBody() {
		// TODO 自動生成されたメソッド・スタブ
		return "info";
	}

	@Override
	public String getCommandDescription() {
		// TODO 自動生成されたメソッド・スタブ
		return "Shows informations about a chain or this application";
	}

	@Override
	public void onCommand(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		if (args.length == 0) {
			System.err.println("CrazyPrivate (No version)");
			return;
		}
		switch (args[0].toLowerCase(Locale.ROOT)) {
		case "chain":
			String key = args[1];
			DataChain dc = server.getDataChain();
			DataChain.NodeParent np = dc.findChain(key, false);
			if (np == null) {
				System.err.println("Chain is not found! Is it private key?");
				return;
			}
			System.out.println("Public key: " + np.publicKey);
			System.out.println("Private key: " + np.privateKey);
			System.out.println("Prefix: " + np.prefix);
			System.out.println("Mode: " + np.mode);
			System.out.println("==========");
			switch (np.mode) {
			case "easyRedirect":
				String json;
				File dir = new File(DataChain.FILES_DIR, np.publicKey);
				try {
					json = new String(Files.readAllBytes(new File(dir, "options.json").toPath()),
							StandardCharsets.UTF_8);
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
				DataChain.EasyRedirectOptions ero = gson.fromJson(json, DataChain.EasyRedirectOptions.class);
				System.out.println("Redirect location: " + ero.address);
				break;
			case "gpsGet":
				dir = new File(DataChain.FILES_DIR, np.publicKey);
				try {
					json = new String(Files.readAllBytes(new File(dir, "options.json").toPath()),
							StandardCharsets.UTF_8);
				} catch (IOException e1) {
					e1.printStackTrace();
					return;
				}
				DataChain.GPSGetOptions ggo = gson.fromJson(json, DataChain.GPSGetOptions.class);
				if (ggo.close) {
					System.out.println("Redirect location: (Close the webpage)");
				} else {
					System.out.println("Redirect location: " + ggo.address);
				}
				System.out.println("Page title: " + ggo.title);
				System.out.println("Message1: " + ggo.message);
				System.out.println("Message2: " + ggo.gps_message);
				System.out.println("Button: " + ggo.gps_button);
				break;
			default:
				System.err.println("Broken chain!");
				return;
			}
			break;
		}
	}
}
