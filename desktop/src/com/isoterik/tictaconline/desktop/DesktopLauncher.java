package com.isoterik.tictaconline.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.isoterik.tictaconline.Constants;
import com.isoterik.tictaconline.TicTacOnline;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		//config.forceExit = true;
		config.title = "Tic Tac Online";
		config.width = Constants.GUI_WIDTH;
		config.height = Constants.GUI_HEIGHT;
		config.resizable = false;

		new LwjglApplication(new TicTacOnline(), config);
	}
}
