package com.isoterik.tictaconline.components;

import com.isoterik.mgdx.Component;
import com.isoterik.mgdx.GameObject;
import com.isoterik.mgdx.input.ITouchListener;
import com.isoterik.mgdx.input.TouchTrigger;

public class ClickListener extends Component {
    private final ClickHandler clickHandler;

    public ClickListener(ClickHandler clickHandler) {
        this.clickHandler = clickHandler;
    }

    @Override
    public void start() {
        input.addListener(TouchTrigger.touchDownTrigger(), (ITouchListener) (mapName, evt) -> {
            float touchX = evt.touchX;
            float touchY = evt.touchY;
            float width = gameObject.transform.size.x;
            float height = gameObject.transform.size.y;
            float x = gameObject.transform.position.x;
            float y = gameObject.transform.position.y;

            if ((touchX >= x && touchX <= (x + width)) &&
                (touchY >= y && touchY <= (y + height))) {
                if (clickHandler != null)
                    clickHandler.onClick(gameObject);
            }
        });
    }

    public interface ClickHandler {
        void onClick(GameObject target);
    }
}
