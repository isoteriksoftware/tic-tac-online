package com.isoterik.tictaconline.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.isoterik.mgdx.Scene;
import com.isoterik.mgdx.utils.WorldUnits;
import com.isoterik.tictaconline.Constants;
import com.isoterik.tictaconline.UIHelper;

public class SplashScene extends Scene {
    private final WorldUnits worldUnits;
    private final UIHelper uiHelper;

    private Label playersLabel, matchesLabel;

    public SplashScene() {
        worldUnits = new WorldUnits(Constants.GUI_WIDTH, Constants.GUI_HEIGHT, 64f);
        setupCanvas(new StretchViewport(worldUnits.getScreenWidth(), worldUnits.getScreenHeight()));
        uiHelper = UIHelper.instance(worldUnits);

        setBackgroundColor(new Color(0.2f, 0.2f, 0.2f, 1f));
        setupUI();
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);

        Color cyan = uiHelper.skin.getColor("cyan");
        Label label = new Label("TIC TAC ONLINE", uiHelper.skin, "big");
        label.setAlignment(Align.center);
        label.setColor(cyan);
        label.setFontScale(0.9f);

        TextButton btnPlay = new TextButton("PLAY", uiHelper.skin);

        playersLabel = new Label("100", uiHelper.skin);
        playersLabel.setColor(cyan);
        matchesLabel = new Label("300", uiHelper.skin);
        matchesLabel.setColor(cyan);

        Table left = new Table();
        left.add(playersLabel);
        left.row();
        left.add(new Label("Players Online", uiHelper.skin));

        Table right = new Table();
        right.add(matchesLabel);
        right.row();
        right.add(new Label("Active Matches", uiHelper.skin));

        root.top().pad(30);
        root.add(label).expandX().fillX().colspan(2);
        root.row();
        root.add(btnPlay).expandY().colspan(2);
        root.row();
        root.add(left).left().expandX();
        root.add(right);

        canvas.addActor(root);
    }
}


































