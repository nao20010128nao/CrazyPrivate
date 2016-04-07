package com.nao20010128nao.クレイジープライベート.commands;

import com.nao20010128nao.クレイジープライベート.CPMain;

public abstract class CommandBase {
	public CommandBase(CPMain server) {
	}

	public abstract String getCommandBody();

	public abstract String getCommandDescription();

	public abstract void onCommand(String[] args);
}
