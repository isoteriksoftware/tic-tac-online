package com.isoterik.tictaconline.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.isoterik.mgdx.MinGdx;
import com.isoterik.mgdx.Scene;
import com.isoterik.mgdx.input.IKeyListener;
import com.isoterik.mgdx.input.KeyCodes;
import com.isoterik.mgdx.input.KeyTrigger;
import com.isoterik.mgdx.m2d.scenes.transition.SceneTransitions;
import com.isoterik.mgdx.m2d.scenes.transition.TransitionDirection;
import com.isoterik.mgdx.utils.WorldUnits;
import com.isoterik.tictaconline.Constants;
import com.isoterik.tictaconline.UIHelper;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class SplashScene extends Scene {
    private final WorldUnits worldUnits;
    private final UIHelper uiHelper;

    private Label playersLabel, matchesLabel;

    private Socket clientConnection;

    private Window activeDialog;

    public SplashScene() {
        worldUnits = new WorldUnits(Constants.GUI_WIDTH, Constants.GUI_HEIGHT, 64f);
        setupCanvas(new StretchViewport(worldUnits.getScreenWidth(), worldUnits.getScreenHeight()));
        uiHelper = UIHelper.instance(worldUnits);

        setBackgroundColor(new Color(0.2f, 0.2f, 0.2f, 1f));
        setupUI();
        setupConnection();

        final String MAPPING_EXIT = "mapping_exit_game";
        inputManager.addMapping(MAPPING_EXIT, KeyTrigger.keyDownTrigger(KeyCodes.BACK),
                KeyTrigger.keyDownTrigger(KeyCodes.ESCAPE),
                KeyTrigger.keyDownTrigger(KeyCodes.END));
        inputManager.mapListener(MAPPING_EXIT, (IKeyListener) (mappingName, evt) -> {
            uiHelper.showConfirmDialog("Confirm Exit", "Do you really want to exit the game?", canvas,
                    () -> {
                        clientConnection.disconnect();
                        Gdx.app.exit();
                    });
        });
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
        btnPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showStartDialog();
            }
        });

        playersLabel = new Label("...", uiHelper.skin);
        playersLabel.setColor(cyan);
        matchesLabel = new Label("...", uiHelper.skin);
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

    private void showStartDialog() {
        Window dialog = new Window("Choose a username", uiHelper.skin);
        dialog.setKeepWithinStage(false);

        Preferences preferences = MinGdx.instance().app.getPreferences("user_data");

        TextField usernameField = new TextField(preferences.getString("username", ""), uiHelper.skin);
        usernameField.setMessageText("Enter a username");

        TextButton btnStart = new TextButton("Go!", uiHelper.skin, "small");
        btnStart.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String username = usernameField.getText().trim();
                if (username.equals("")) {
                    uiHelper.showErrorDialog("Please enter a username!", canvas);
                    return;
                }

                preferences.putString("username", username);
                preferences.flush();

                dialog.remove();

                MinGdx.instance().setScene(new MatchMakingScene(username, clientConnection),
                        SceneTransitions.slide(.8f, TransitionDirection.DOWN, false, Interpolation.pow5Out));
            }
        });

        TextButton btnClose = new TextButton("Cancel", uiHelper.skin, "small");
        btnClose.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                uiHelper.actorAnimation.slideOutThenRemove(dialog, TransitionDirection.DOWN, .5f);
            }
        });

        dialog.top();
        dialog.add(usernameField).expand().fillX().pad(20).top().colspan(2);
        dialog.row();
        dialog.add(btnClose).pad(20).padBottom(5).expandX();
        dialog.add(btnStart).pad(20).padBottom(5);

        dialog.setSize(350, 250);
        dialog.setPosition((worldUnits.getScreenWidth() - dialog.getWidth())/2f,
                (worldUnits.getScreenHeight() - dialog.getHeight())/2f);
        canvas.addActor(dialog);
        uiHelper.actorAnimation.slideIn(dialog, TransitionDirection.UP, .5f);
    }

    private void setupConnection() {
        try {
            clientConnection = IO.socket("https://tictac-api.herokuapp.com/");
            clientConnection.connect();

            activeDialog = uiHelper.showDialog("CONNECTING...", "Connecting to the game server, please hold on...", canvas);

            // Listen to status change events
            clientConnection
                    .once(Socket.EVENT_CONNECT, args -> {
                        if (activeDialog != null)
                            activeDialog.remove();

                        activeDialog = uiHelper.showDialog("SUCESS!", "Successfully connected to the server. Play on!", canvas);
                    })
                    .on("game.status.changed", args -> {
                       try {
                           JSONObject data = (JSONObject)args[0];
                           int players = data.getInt("players");
                           int matches = data.getInt("matches");

                           MinGdx.instance().app.postRunnable(() -> {
                               playersLabel.setText(players);
                               matchesLabel.setText(matches);
                           });
                       } catch (JSONException e) {
                           e.printStackTrace();
                       }
                    })
                    .on(Socket.EVENT_CONNECT_ERROR, args -> {
                        MinGdx.instance().app.postRunnable(() -> {
                            uiHelper.showErrorDialog("Failed to connect to the server. Please make sure you're connected to the internet",
                                    canvas);
                        });
                    })
                    .on(Socket.EVENT_CONNECT_TIMEOUT, args -> {
                        MinGdx.instance().app.postRunnable(() -> {
                            uiHelper.showErrorDialog("Failed to connect to the server. Please make sure you're connected to the internet",
                                    canvas);
                        });
                    });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}