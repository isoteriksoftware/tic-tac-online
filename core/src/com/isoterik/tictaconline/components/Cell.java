package com.isoterik.tictaconline.components;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.isoterik.mgdx.Component;
import com.isoterik.mgdx.GameObject;
import com.isoterik.mgdx.Transform;

public class Cell extends Component {
    private boolean hasJelly;

    public boolean hasJelly() {
        return hasJelly;
    }

    public void putJelly(TextureRegion jellyRegion) {
        if (hasJelly)
            return;

        GameObject jelly = scene.newSpriteObject(jellyRegion);
        Transform transform = jelly.transform;
        transform.size.scl(0.7f);

        float width = gameObject.transform.size.x;
        float height = gameObject.transform.size.y;
        float jellyWidth = transform.size.x;
        float jellyHeight = transform.size.y;

        transform.position.x = gameObject.transform.position.x + ((width - jellyWidth) / 2f);
        transform.position.y = gameObject.transform.position.y + ((height - jellyHeight) / 2f);
        scene.addGameObject(jelly);

        hasJelly = true;
    }
}


























