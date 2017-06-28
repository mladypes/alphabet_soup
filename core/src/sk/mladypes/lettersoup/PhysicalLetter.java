package sk.mladypes.lettersoup;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * With <3 by matej on 20/09/16.
 */
public class PhysicalLetter {
    private Sprite sprite;
    private Body body;

    public PhysicalLetter(final World world, Sprite sprite, Vector2 position) {
        this.sprite = sprite;

        createBody(world, position);
    }

    public void draw(final SpriteBatch batch) {
        Vector2 bodyPos = body.getPosition();
        sprite.setPosition(bodyPos.x - sprite.getWidth() / 2, bodyPos.y - sprite.getHeight() / 2);
        sprite.setOrigin(sprite.getWidth() / 2, sprite.getHeight() / 2);
        sprite.setRotation(body.getAngle() * MathUtils.radiansToDegrees);

        sprite.draw(batch);
    }

    private void createBody(final World world, Vector2 position) {
        if (sprite == null) {
            throw new RuntimeException("null sprite in letter");
        }

        BodyDef bodyDef = new BodyDef();
        bodyDef.angularDamping = 10;
        bodyDef.linearDamping = 10;
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(position);

        body = world.createBody(bodyDef);
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(sprite.getWidth() / 2, sprite.getHeight() / 2);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0.1f;
        fixtureDef.friction = 1.0f;
        fixtureDef.restitution = .0f;

        body.createFixture(fixtureDef);
        shape.dispose();
    }
}
