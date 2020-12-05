package com.isoterik.tictaconline;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.isoterik.mgdx.MinGdx;
import com.isoterik.mgdx.ui.ActorAnimation;
import com.isoterik.mgdx.utils.WorldUnits;

public final class UIHelper {
    private static UIHelper instance;

    public final Skin skin;
    public ActorAnimation actorAnimation;
    public WorldUnits worldUnits;

    private UIHelper(){
        skin = MinGdx.instance().assets.getSkin("skin/glassy-ui.json");
    }

    public static void init() {
        instance = new UIHelper();
    }

    public static UIHelper instance(WorldUnits worldUnits) {
        instance.worldUnits = worldUnits;
        instance.actorAnimation = ActorAnimation.instance();

        if (worldUnits != null)
            instance.actorAnimation.setup(worldUnits.getScreenWidth(), worldUnits.getScreenHeight());

        return instance;
    }

    public Window showDialog(String title, String message, Stage canvas) {
        final Window window = new Window(title, skin);
        window.setModal(false);
        window.setKeepWithinStage(false);

        Label label = new Label(message, skin, "black");
        label.setAlignment(Align.center);
        label.setWrap(true);

        ScrollPane scrollPane = new ScrollPane(label, skin);
        scrollPane.setScrollingDisabled(true, false);

        TextButton btnClose = new TextButton("Close", skin, "small");
        btnClose.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                actorAnimation.slideOutThenRemove(window, ActorAnimation.LEFT, .5f);
            }
        });

        window.add(scrollPane).expand().fill().pad(10);
        window.row();
        window.add(btnClose).expandX().fillX().height(40).pad(10);

        window.setSize(400, 300);
        window.setPosition((worldUnits.getScreenWidth() - window.getWidth())/2f,
                (worldUnits.getScreenHeight() - window.getHeight())/2f);
        canvas.addActor(window);

        actorAnimation.slideIn(window, ActorAnimation.LEFT, .5f);
        return window;
    }

    public Window showErrorDialog(String message, Stage canvas) {
        return showDialog("Oops!", message, canvas);
    }

    public Window showConfirmDialog(String title, String message, Stage canvas, Runnable onClose, Runnable onConfirm) {
        final Window window = new Window(title, skin);
        window.setModal(true);
        window.setKeepWithinStage(false);

        Label label = new Label(message, skin, "black");
        label.setAlignment(Align.left);
        label.setWrap(true);

        ScrollPane scrollPane = new ScrollPane(label, skin);
        scrollPane.setScrollingDisabled(true, false);

        TextButton btnConfirm = new TextButton("Yes", skin, "small");
        btnConfirm.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                window.remove();
                onConfirm.run();
            }
        });

        TextButton btnClose = new TextButton("No", skin, "small");
        btnClose.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                actorAnimation.slideOutThenRemove(window, ActorAnimation.LEFT, .5f);
                if (onClose != null)
                    onClose.run();
            }
        });

        Table tbl = new Table();
        tbl.left().pad(10);
        tbl.add(btnClose).expandX().height(40).left();
        tbl.add(btnConfirm).height(40);

        window.add(scrollPane).expand().fill().pad(10);
        window.row();
        window.add(tbl).expandX().fillX();

        window.setSize(400, 300);
        window.setPosition((worldUnits.getScreenWidth() - window.getWidth())/2f,
                (worldUnits.getScreenHeight() - window.getHeight())/2f);
        canvas.addActor(window);

        actorAnimation.slideIn(window, ActorAnimation.LEFT, .5f);
        return window;
    }

    public Window showConfirmDialog(String title, String message, Stage canvas, Runnable onConfirm) {
        return showConfirmDialog(title, message, canvas, null, onConfirm);
    }
}
