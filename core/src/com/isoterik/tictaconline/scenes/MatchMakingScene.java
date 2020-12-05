package com.isoterik.tictaconline.scenes;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.isoterik.mgdx.MinGdx;
import com.isoterik.mgdx.Scene;
import com.isoterik.mgdx.m2d.scenes.transition.SceneTransitions;
import com.isoterik.mgdx.m2d.scenes.transition.TransitionDirection;
import com.isoterik.mgdx.utils.WorldUnits;
import com.isoterik.tictaconline.Constants;
import com.isoterik.tictaconline.UIHelper;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

public class MatchMakingScene extends Scene {
    private final MinGdx minGdx;
    private UIHelper uiHelper;
    private List<String> playersList;
    private Window activeDialog;
    private TextButton btnStart;

    private final ArrayMap<String, String> availablePlayers;

    private Socket clientConnection;
    private final String username;
    private boolean joinedGame; // Have we been enlisted as an available player?

    public MatchMakingScene(String username) {
        this.username = username;
        availablePlayers = new ArrayMap<>();

        minGdx = MinGdx.instance();

        setBackgroundColor(new Color(0.2f, 0.2f, 0.25f, 1f));
        setupUI();
        setupConnection();
    }

    private void setupUI() {
        WorldUnits worldUnits = new WorldUnits(Constants.GUI_WIDTH, Constants.GUI_HEIGHT, 64f);
        setupCanvas(new StretchViewport(worldUnits.getScreenWidth(), worldUnits.getScreenHeight()));

        uiHelper = UIHelper.instance(worldUnits);

        Table root = new Table();
        root.setFillParent(true);

        playersList = new List<>(uiHelper.skin);
        playersList.setItems(availablePlayers.values().toArray());
        ScrollPane scrollPane = new ScrollPane(playersList, uiHelper.skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setSmoothScrolling(true);

        btnStart = new TextButton("Start Game", uiHelper.skin);

        root.top();
        root.row();
        root.add(new Label("Choose an opponent from online players", uiHelper.skin)).expandX().fillX().pad(20).padTop(30).padBottom(5);
        root.row();
        root.add(scrollPane).height(450).expandX().fillX().pad(20).padTop(0).expandY().fillY();
        root.row();
        root.add(btnStart).expandX().pad(20).padTop(40);

        canvas.addActor(root);

        btnStart.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (clientConnection == null || !joinedGame) {
                    activeDialog = uiHelper.showErrorDialog("A network error occured!\nPlease make sure you're connected to the internet.", canvas);
                    return;
                }

                if (availablePlayers.isEmpty()) {
                    activeDialog = uiHelper.showErrorDialog("No available opponent to play with!", canvas);
                    return;
                }

                String opponentId = availablePlayers.getKey(playersList.getSelected(), false);
                log("Attempting to play with: " + playersList.getSelected() + "("  + opponentId + ")");

                activeDialog = uiHelper.showDialog("Connecting...", "Please wait while we attempt to connect you with the other player...",
                        canvas);
                btnStart.setDisabled(true);

                // Send a match request
                clientConnection.emit("player.match", opponentId);

                // Listen to possible replies from the server
                clientConnection.on("player.match.opponent-not-found", args -> {
                    activeDialog.remove();
                    uiHelper.showErrorDialog("The player is no longer connected!", canvas);
                })
                .on("player.match.opponent-matched", args -> {
                    activeDialog.remove();
                    uiHelper.showErrorDialog("This player is now matched with another player!", canvas);
                });
            }
        });
    }

    private void log(String what) {
        minGdx.app.log(getClass().getSimpleName(), what);
    }

    private void setupConnection() {
        try {
            clientConnection = IO.socket("http://localhost:5000");
            clientConnection.connect();

            clientConnection.on("player.connect", args -> {
                log("Connected!");

                try {
                    JSONObject data = (JSONObject)args[0];

                    JSONArray unmatchedPlayers = data.getJSONArray("unmatched");
                    for (int i = 0; i < unmatchedPlayers.length(); i++) {
                        JSONObject player = unmatchedPlayers.getJSONObject(i);
                        availablePlayers.put(player.getString("id"), player.getString("name"));
                    }

                    // Update the list UI of available players
                    minGdx.app.postRunnable(() -> playersList.setItems(availablePlayers.values().toArray()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Let's attempt to join
                clientConnection.emit("player.join", username);
            })
            .on("player.joined", args -> {
                try {
                    JSONObject data = (JSONObject)args[0];

                    availablePlayers.put(data.getString("id"), data.getString("playerName"));

                    // Update the list UI of available players
                    minGdx.app.postRunnable(() -> playersList.setItems(availablePlayers.values().toArray()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            })
            .on("player.left", args -> {
                availablePlayers.removeKey((String)args[0]);

                // Update the list UI of available players
                minGdx.app.postRunnable(() -> playersList.setItems(availablePlayers.values().toArray()));
            })
            .on("player.matched", args -> {
                availablePlayers.removeKey((String)args[0]);

                // Update the list UI of available players
                minGdx.app.postRunnable(() -> playersList.setItems(availablePlayers.values().toArray()));
            })
            .on("player.welcome", args -> {
                // Other players can see us now
                joinedGame = true;
            })
            .on("player.match.request", args -> {
                try {
                    JSONObject data = (JSONObject)args[0];
                    String playerName = data.getString("playerName");

                    // A player wants to compete with us
                    minGdx.app.postRunnable(() ->
                            activeDialog = uiHelper.showConfirmDialog("Match Request", "'" + playerName + "' wants to play with you!\n\nDo you accept the challenge?",
                                    canvas,
                                    () -> clientConnection.emit("player.match.decline", data),
                                    () -> {
                                        // Accept the challenge
                                        clientConnection.emit("player.match.accept", data);
                                    }));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            })
            .on("player.match.start", args -> {
                try {
                    JSONObject data = (JSONObject)args[0];

                    btnStart.setDisabled(false);

                    MinGdx.instance().app.postRunnable(() ->
                    {
                        try {
                            if (activeDialog != null)
                                activeDialog.remove();

                            MinGdx.instance().setScene(new GameScene(clientConnection, data),
                                    SceneTransitions.slide(1f, TransitionDirection.UP, true,
                                            Interpolation.pow5Out));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })
            .on("player.match.opponent-declined", args -> {
                if (activeDialog != null)
                    activeDialog.remove();

                btnStart.setDisabled(false);

                String playerName = (String)args[0];
                activeDialog = uiHelper.showDialog("DECLINED!", "'" + playerName + "' declined your challenge!", canvas);
            });

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
































