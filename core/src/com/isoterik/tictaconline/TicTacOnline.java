package com.isoterik.tictaconline;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.isoterik.mgdx.MinGdxGame;
import com.isoterik.mgdx.Scene;
import com.isoterik.mgdx.m2d.scenes.transition.SceneTransitions;
import com.isoterik.tictaconline.scenes.GameScene;
import com.isoterik.tictaconline.scenes.MatchMakingScene;
import com.isoterik.tictaconline.scenes.SplashScene;

public class TicTacOnline extends MinGdxGame {
	@Override
	protected Scene initGame() {
	    minGdx.app.setLogLevel(Application.LOG_DEBUG);

	    loadAssets();
		UIHelper.init();

		splashTransition = SceneTransitions.fade(1f);
		/*return new SplashScene();*/ return new MatchMakingScene("player" + MathUtils.random(1, 100));
	}

	private void loadAssets() {
		minGdx.assets.enqueueSkin("skin/glassy-ui.json");
	    minGdx.assets.enqueueAsset("bg.png", Texture.class);
        minGdx.assets.enqueueAsset("board.png", Texture.class);
        minGdx.assets.enqueueAtlas("jellies.atlas");

        minGdx.assets.loadAssetsNow();
    }
}