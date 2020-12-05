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
import com.isoterik.mgdx.input.IKeyListener;
import com.isoterik.mgdx.input.KeyCodes;
import com.isoterik.mgdx.input.KeyTrigger;
import com.isoterik.mgdx.m2d.scenes.transition.SceneTransitions;
import com.isoterik.mgdx.m2d.scenes.transition.TransitionDirection;
import com.isoterik.mgdx.utils.WorldUnits;
import com.isoterik.tictaconline.Constants;
import com.isoterik.tictaconline.UIHelper;
import io.socket.client.Socket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MatchMakingScene extends Scene {
    private final MinGdx minGdx;
    private UIHelper uiHelper;
    private List<String> playersList;
    private Window activeDialog;
    private TextButton btnStart;

    private final ArrayMap<String, String> availablePlayers;

    private final Socket clientConnection;
    private final String username;
    private boolean joinedGame; // Have we been enlisted as an available player?

    public MatchMakingScene(String username, Socket clientConnection) {
        this.clientConnection = clientConnection;
        this.username = username;
        availablePlayers = new ArrayMap<>();

        minGdx = MinGdx.instance();

        setBackgroundColor(new Color(0.2f, 0.2f, 0.25f, 1f));
        setupUI();
        setupConnection();

        String MAPPING_EXIT = "mapping_exit_match_making_scene";
        inputManager.addMapping(MAPPING_EXIT, KeyTrigger.keyDownTrigger(KeyCodes.BACK),
                KeyTrigger.keyDownTrigger(KeyCodes.ESCAPE),
                KeyTrigger.keyDownTrigger(KeyCodes.END));
        inputManager.mapListener(MAPPING_EXIT, (IKeyListener) (mappingName, evt) -> {
            clientConnection.emit("player.leave");

            MinGdx.instance().sceneManager.revertToPreviousScene(
                    SceneTransitions.slide(.8f, TransitionDirection.UP, true, Interpolation.pow5Out)
            );
        });
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
                //btnStart.setDisabled(true);

                // Send a match request
                clientConnection.emit("player.match", opponentId);
            }
        });
    }

    private void log(String what) {
        minGdx.app.log(getClass().getSimpleName(), what);
    }

    private void setupConnection() {
        try {
            // First, clear all events that we may have registered before.
            clientConnection.off("player.connect");
            clientConnection.off("player.joined");
            clientConnection.off("player.left");
            clientConnection.off("player.matched");
            clientConnection.off("player.welcome");
            clientConnection.off("player.match.request");
            clientConnection.off("player.match.start");
            clientConnection.off("player.match.opponent-declined");
            clientConnection.off("player.match.opponent-not-found");
            clientConnection.off("player.match.opponent-matched");

            // Let the server know we are ready to connect
            clientConnection.emit("player.ready");

            clientConnection
            .on("player.connect", args -> {
                availablePlayers.clear();

                try {
                    JSONObject data = (JSONObject)args[0];
                    String playerId = data.getString("id");

                    JSONArray unmatchedPlayers = data.getJSONArray("unmatched");
                    for (int i = 0; i < unmatchedPlayers.length(); i++) {
                        JSONObject player = unmatchedPlayers.getJSONObject(i);
                        String id = player.getString("id");

                        if (!id.equals(playerId))
                            availablePlayers.put(id, player.getString("name"));
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

                    log(data.getString("id") + " joined");
                    availablePlayers.put(data.getString("id"), data.getString("playerName"));

                    // Update the list UI of available players
                    minGdx.app.postRunnable(() -> playersList.setItems(availablePlayers.values().toArray()));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            })
            .on("player.left", args -> {
                log("Player left received: " + args[0]);
                log("Has leaver: " + availablePlayers.containsKey((String)args[0]));
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
            })
            .on("player.match.opponent-not-found", args -> {
                // Remove the player from our list
                availablePlayers.removeKey((String)args[0]);
                activeDialog.remove();
                uiHelper.showErrorDialog("The player is no longer connected!", canvas);
            })
            .on("player.match.opponent-matched", args -> {
                // Remove the player from our list
                availablePlayers.removeKey((String)args[0]);

                activeDialog.remove();
                uiHelper.showErrorDialog("This player is currently playing with another player!", canvas);
            })
            .once(Socket.EVENT_ERROR, args -> {
                MinGdx.instance().app.postRunnable(() -> {
                    activeDialog = uiHelper.showErrorDialog("A server error occurred!",
                            canvas);
                });
            })
            .once(Socket.EVENT_RECONNECTING, args -> {
                MinGdx.instance().app.postRunnable(() -> {
                    activeDialog = uiHelper.showErrorDialog("Lost connection to the server!",
                            canvas);
                });
            })
            .once(Socket.EVENT_RECONNECT, args -> {
                // Clear the current list of available players
                availablePlayers.clear();

                clientConnection.emit("player.ready");

                MinGdx.instance().app.postRunnable(() -> {
                    if (activeDialog != null)
                        activeDialog.remove();

                    uiHelper.showDialog("RECONNECTED", "Reconnected to server!", canvas);
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
































