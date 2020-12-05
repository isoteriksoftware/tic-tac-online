package com.isoterik.tictaconline.scenes;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ArrayMap;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.isoterik.mgdx.GameObject;
import com.isoterik.mgdx.MinGdx;
import com.isoterik.mgdx.Scene;
import com.isoterik.mgdx.Transform;
import com.isoterik.mgdx.io.GameAssetsLoader;
import com.isoterik.mgdx.m2d.scenes.transition.SceneTransitions;
import com.isoterik.mgdx.m2d.scenes.transition.TransitionDirection;
import com.isoterik.mgdx.utils.WorldUnits;
import com.isoterik.tictaconline.Constants;
import com.isoterik.tictaconline.UIHelper;
import com.isoterik.tictaconline.components.Cell;
import com.isoterik.tictaconline.components.ClickListener;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

public class GameScene extends Scene {
    private WorldUnits worldUnits;
    private final GameAssetsLoader assetsLoader;
    private Window activeDialog;

    private UIHelper uiHelper;
    private Label turnLabel;

    private final TextureAtlas jellies;
    private ArrayMap<String, GameObject> boardCells;

    private final Socket clientConnection;
    private final String playerId, opponentId, matchId, playerName, opponentName, playerJelly, opponentJelly;
    private String turn, turnId;

    public GameScene(Socket clientConnection, JSONObject initialGameData) throws JSONException {
        this.clientConnection = clientConnection;
        playerId = initialGameData.getString("id");
        matchId = initialGameData.getString("matchId");
        playerName = initialGameData.getString("name");
        playerJelly = initialGameData.getString("jelly");
        opponentName = initialGameData.getJSONObject("opponent").getString("name");
        opponentId = initialGameData.getJSONObject("opponent").getString("id");
        opponentJelly = initialGameData.getJSONObject("opponent").getString("jelly");

        assetsLoader = MinGdx.instance().assets;
        jellies = assetsLoader.getAtlas("jellies.atlas");

        setupCamera();
        setupBackground();
        setupUI(opponentName);
        setupBoardAndConnection();

        turnId = initialGameData.getString("turn");
        setTurn(turnId);
    }

    private void setupCamera() {
        worldUnits = new WorldUnits(Constants.GUI_WIDTH, Constants.GUI_HEIGHT, 64f);
        mainCamera.setup(new ExtendViewport(worldUnits.getWorldWidth(), worldUnits.getWorldHeight(),
                mainCamera.getCamera()), worldUnits);
        setupCanvas(new StretchViewport(worldUnits.getScreenWidth(), worldUnits.getScreenHeight()));
        uiHelper = UIHelper.instance(worldUnits);
    }

    private void setupBackground() {
        GameObject bg = newSpriteObject(assetsLoader.regionForTexture("bg.png", false));
        addGameObject(bg);
    }

    private void setupUI(String opponentName) {
        Table root = new Table();
        root.setFillParent(true);

        turnLabel = new Label(turn, uiHelper.skin, "big");
        turnLabel.setAlignment(Align.center);
        turnLabel.setFontScale(0.5f);

        TextButton btnForfeit = new TextButton("Forfeit", uiHelper.skin, "small");
        btnForfeit.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                activeDialog = uiHelper.showConfirmDialog("FORFEIT???", "Do you really really want to give up now?",
                        canvas, () -> clientConnection.emit("player.match.forfeit", matchId));
            }
        });

        Table top = new Table();
        top.pad(20);
        top.row();
        top.add(new Image(jellies.findRegion(playerJelly))).left().size(30).padRight(10);
        top.add(new Label("You", uiHelper.skin, "black")).left();
        top.add(new Label("VS", uiHelper.skin, "black")).expandX();
        top.add(new Label(opponentName, uiHelper.skin, "black"))
                .right();
        top.add(new Image(jellies.findRegion(opponentJelly))).left().size(30).padLeft(10);

        root.top();
        root.add(top).expandX().fillX();
        root.row();
        root.add(turnLabel).expandX().expandY().top().padTop(0);
        root.row();
        root.add(btnForfeit).expandX().fillX().pad(20);

        canvas.addActor(root);
    }

    private void setupBoardAndConnection() {
        GameObject board = newSpriteObject(assetsLoader.regionForTexture("board.png", false));
        Transform transform = board.transform;
        transform.position.x = (worldUnits.getWorldWidth() - transform.size.x) / 2f;
        transform.position.y = (worldUnits.getWorldHeight() - transform.size.y) / 2f;
        addGameObject(board);

        // When the player clicks a cell, we make a move if possible
        final ClickListener.ClickHandler cellClickHandler = cell -> {
            if (activeDialog != null && activeDialog.getStage() != null)
                return;

            if (!turnId.equals(playerId))
                return;

            if (!cell.getComponent(Cell.class).hasJelly()) {
                try {
                    JSONObject data = new JSONObject();
                    data.put("matchId", matchId);
                    data.put("move", cell.getTag());

                    clientConnection.emit("player.move.make", data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        boardCells = new ArrayMap<>();
        float cellW = worldUnits.toWorldUnit(Constants.BOARD_CELL_WIDTH);
        float cellH = worldUnits.toWorldUnit(Constants.BOARD_CELL_HEIGHT);
        float boardX = transform.position.x;
        float boardY = transform.position.y;

        for (int row = 0; row < 3; row++) {
            float y = boardY + (cellH * row);

            for (int col = 0; col < 3; col++) {
                float x = boardX + (cellW * col);

                String tag = (2 - row) + "|" + col;
                GameObject cell = GameObject.newInstance(tag);
                cell.transform.setSize(cellW, cellH);
                cell.transform.setPosition(x, y);
                cell.addComponent(new Cell());
                cell.addComponent(new ClickListener(cellClickHandler));

                boardCells.put(tag, cell);
                addGameObject(cell);
            }
        }

        // First, clear all events that we may have register before.
        clientConnection.off("player.move.made");
        clientConnection.off("player.match.opponent-disconnected");
        clientConnection.off("player.match.won");
        clientConnection.off("player.match.draw");
        clientConnection.off("player.match.forfeited");

        // Setup listeners for moves made
        clientConnection
                .on("player.move.made", args -> {
                   try {
                       JSONObject data = (JSONObject)args[0];
                       String jelly = data.getString("jelly");
                       turnId = data.getString("turn");
                       String move = data.getString("move");

                       MinGdx.instance().app.postRunnable(() -> {
                           setTurn(turnId);
                           boardCells.get(move).getComponent(Cell.class).putJelly(jellies.findRegion(jelly));
                       });
                   } catch (JSONException e) {
                       e.printStackTrace();
                   }
                })
                // We need to stop the game when the opponent disconnects
                .on("player.match.opponent-disconnected", args -> {
                    MinGdx.instance().app.postRunnable(() -> {
                        activeDialog = uiHelper.showErrorDialog("We lost connection with your opponent!", canvas);
                        transitionToMatchMakingScene();
                    });
                })
                // When a player wins
                .on("player.match.won", args -> {
                    String id = (String)args[0];

                    String message = "YOU WON!";
                    if (id.equals(opponentId))
                        message = "YOU LOST!";

                    String finalMessage = message;
                    MinGdx.instance().app.postRunnable(() -> {
                        activeDialog = uiHelper.showDialog("GAME OVER!", finalMessage, canvas);
                        transitionToMatchMakingScene();
                    });
                })
                // When the game is drawn
                .on("player.match.draw", args -> {
                    MinGdx.instance().app.postRunnable(() -> {
                        activeDialog = uiHelper.showDialog("GAME OVER!", "IT IS A DRAW!", canvas);
                        transitionToMatchMakingScene();
                    });
                })
                // When a player forfeits
                .on("player.match.forfeited", args -> {
                    String id = (String)args[0];
                    String message = "YOU GAVE UP!";
                    if (id.equals(opponentId))
                        message = opponentName + " GAVE UP! YOU WIN";

                    String finalMessage = message;
                    MinGdx.instance().app.postRunnable(() -> {
                        activeDialog = uiHelper.showDialog("GAME OVER!", finalMessage, canvas);
                        transitionToMatchMakingScene();
                    });
                })
                .once(Socket.EVENT_ERROR, args -> {
                    MinGdx.instance().app.postRunnable(() -> {
                        uiHelper.showErrorDialog("A server error occurred!",
                                canvas);
                    });
                })
                .once(Socket.EVENT_RECONNECTING, args -> {
                    MinGdx.instance().app.postRunnable(() -> {
                        uiHelper.showErrorDialog("Lost connection to the server!",
                                canvas);
                    });
                })
                .once(Socket.EVENT_RECONNECT, args -> transitionToMatchMakingScene(true));
    }

    private void transitionToMatchMakingScene(boolean reconnected) {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                MinGdx.instance().sceneManager.revertToPreviousScene(
                        SceneTransitions.slide(1f, TransitionDirection.DOWN, true,
                                Interpolation.pow5Out));
                if (reconnected)
                    clientConnection.emit("player.ready");
            }
        }, 3);
    }

    private void transitionToMatchMakingScene() {
        transitionToMatchMakingScene(false);
    }

    private void setTurn(String turnId) {
        if (turnId.equals(playerId))
            turn = "Your Turn!";
        else
            turn = opponentName + "'s Turn!";

        turnLabel.setText(turn);
    }
}