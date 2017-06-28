package sk.mladypes.lettersoup;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;

/**
 * With <3 by matej on 28/09/16.
 */
public class WaterCircle implements Pool.Poolable {

    private float timeToLive;
    private float aliveTime;
    private float fullRadius;
    private Vector2 position;

    private Interpolation interpolation;

    public WaterCircle() {
        timeToLive = 0;
        aliveTime = 0;
        position = new Vector2();
        interpolation = Interpolation.fade;
    }

    public void init(float x, float y, float timeToLive, float fullRadius) {
        this.timeToLive = timeToLive;
        this.position.set(x, y);
        this.fullRadius = fullRadius;
        this.aliveTime = 0;
    }

    public void update(float delta) {
        aliveTime += delta;
    }

    public void draw(ShapeRenderer shapeRenderer) {
        float progress = Math.min(1.0f, aliveTime / timeToLive);
        shapeRenderer.circle(position.x, position.y, interpolation.apply(0, fullRadius, progress), 10);
    }

    boolean isAlive() {
        return aliveTime <= timeToLive;
    }

    @Override
    public void reset() {
        position.set(0, 0);
        timeToLive = 1;
        fullRadius = 1;
        aliveTime = 0;
    }
}
