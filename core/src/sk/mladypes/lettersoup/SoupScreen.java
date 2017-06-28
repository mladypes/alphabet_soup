package sk.mladypes.lettersoup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.utils.*;

import java.util.Date;

/**
 * With <3 by matej on 19/09/16.
 */
public class SoupScreen implements Screen, InputProcessor {

    private final float WORLD_WIDTH = 16;
    private final float WORLD_HEIGHT = 9;
    private final float scaleFactor;

    private final LetterSoup game;
    private TextureAtlas lettersAtlas;
    private SpriteBatch batch;

    private OrthographicCamera camera;

    private Vector3 touchPosition;

    private Array<PhysicalLetter> letters;
    private World world;

    private Box2DDebugRenderer renderer;

    private final int maxTouches;
    private Body plateCircle;
    private Body[] touchBodies;
    private MouseJoint[] mouseJoints;

    private float accumulator;
    private final float step;

    private Pool<WaterCircle> waterCirclePool;
    private Array<WaterCircle> waterCircles;

    private ShapeRenderer waterCircleRenderer;

    private final long waterCircleTimeThreshold = 400;

    private long[] lastWaterCircleTimes;

    private final Color gray;
    private final Color colored;

    private Color interpolated;
    private Color target;
    private Color starting;

    private Interpolation colorInterpolation;
    private final float interpolationTime = 1;
    private long interpolationStartTime;

    private final Vector2 platePosition;

    private Timer timer;


    public SoupScreen(LetterSoup game) {
        scaleFactor = WORLD_WIDTH / Gdx.graphics.getWidth();
        accumulator = 0;
        step = 1.0f / 60;
        maxTouches = 10;

        platePosition = new Vector2(WORLD_WIDTH / 2 + 0.2f, WORLD_HEIGHT / 2 - 0.05f);

        gray = new Color(0.5f, 0.5f, 0.5f, 1.0f);
        colored = new Color(1.0f, 0.5f, 0.5f, 1.0f);

        interpolated = new Color(gray);
        colorInterpolation = Interpolation.fade;
        interpolationStartTime = System.currentTimeMillis();

        target = gray;
        starting = gray;


        lastWaterCircleTimes = new long[maxTouches];

        for (int i = 0; i < maxTouches; ++i) {
            lastWaterCircleTimes[i] = 0;
        }

        touchBodies = new Body[maxTouches];
        mouseJoints = new MouseJoint[maxTouches];


        this.game = game;
        lettersAtlas = new TextureAtlas(Gdx.files.internal("images/letters/letters.atlas"));
        batch = game.getGlobalSpriteBatch();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
        world = new World(new Vector2(0, 0), false);
        plateCircle = createPlateCircle();

        letters = new Array<PhysicalLetter>();
        for (int i = 0; i < 7; ++i) {
            for (char c = 'A'; c <= 'Z'; ++c) {
                Sprite sprite = createLetterSprite(c);

                float radius = WORLD_HEIGHT / 2 * 0.8f;
                Vector2 direction = new Vector2(MathUtils.random(-0.5f, 0.5f), MathUtils.random(-0.5f, 0.5f)).nor();
                letters.add(new PhysicalLetter(world, sprite, direction.scl(radius * MathUtils.random()).add(platePosition)));
            }
        }

        renderer = new Box2DDebugRenderer();
        touchPosition = new Vector3();

        Gdx.input.setInputProcessor(this);

        waterCirclePool = new Pool<WaterCircle>() {
            @Override
            protected WaterCircle newObject() {
                return new WaterCircle();
            }
        };

        waterCircles = new Array<WaterCircle>();
        waterCircleRenderer = new ShapeRenderer();

        timer = new Timer();
        timer.scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);

                Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
                BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
                PixmapIO.writePNG(Gdx.files.external(SoupScreen.this.game.screensDirectory + "/" + new Date().toString() + ".png"), pixmap);
                pixmap.dispose();
            }
        }, 600, 600);
    }

    @Override
    public void render(float delta) {
        float progress = colorInterpolation.apply((System.currentTimeMillis() - interpolationStartTime) / 1000.0f / interpolationTime);
        interpolated.lerp(target, progress);

        Gdx.gl.glClearColor(interpolated.r, interpolated.g, interpolated.b, interpolated.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        waterCircleRenderer.setProjectionMatrix(camera.combined);
        waterCircleRenderer.begin(ShapeRenderer.ShapeType.Line);
        waterCircleRenderer.setColor(1, 0.0f, 0.0f, 0.5f);
        for (WaterCircle c : waterCircles) {
            c.update(delta);
            if (!c.isAlive()) {
                waterCircles.removeValue(c, true);
                waterCirclePool.free(c);
            } else {
                c.draw(waterCircleRenderer);
            }
        }

        waterCircleRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (PhysicalLetter l : letters) {
            l.draw(batch);
        }

        batch.end();
//        renderer.render(world, camera.combined);
        fixedStep(delta);



    }

    private void fixedStep(float delta) {
        accumulator += delta;
        while (accumulator > step) {
            accumulator -= delta;
            world.step(step, 4, 4);
        }
    }

    private Sprite createLetterSprite(char letter) {
        Sprite s = lettersAtlas.createSprite(String.valueOf(letter));

        s.setSize(scaleFactor * s.getRegionWidth(), scaleFactor * s.getRegionHeight());

        return s;
    }

    private Body createPlateCircle() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;

        Body body = world.createBody(bodyDef);

        final float fullCircle = 2 * MathUtils.PI;
        final int circleSegments = 20;
        final float angleStep = fullCircle / circleSegments;
        final Vector2 rotationOrigin = platePosition;
        final Vector2 basePoint = new Vector2(WORLD_WIDTH / 2, (WORLD_HEIGHT / 2) * 0.05f);

        Vector2[] circleVertices = new Vector2[circleSegments + 1];
        int i = 0;
        for (float angle = 0; angle < fullCircle; angle += angleStep) {
            float c = MathUtils.cos(angle);
            float s = MathUtils.sin(angle);

            Vector2 tempPoint = new Vector2(basePoint).sub(rotationOrigin);
            Vector2 rotatedPoint = new Vector2(
                    tempPoint.x * c - tempPoint.y * s,
                    tempPoint.x * s + tempPoint.y * c).add(rotationOrigin);
            circleVertices[i++] = rotatedPoint;
        }

        circleVertices[circleSegments] = basePoint;

        ChainShape chain = new ChainShape();
        chain.createChain(circleVertices);

        body.createFixture(chain, 0.0f);

        chain.dispose();

        return body;
    }

    private void addWaterCricle(float x, float y, int index) {

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWaterCircleTimes[index] < waterCircleTimeThreshold) {
            return;
        }
        lastWaterCircleTimes[index] = currentTime;
        WaterCircle c = waterCirclePool.obtain();
        c.init(x, y, 2, WORLD_HEIGHT / 2);
        waterCircles.add(c);
    }

    private Body createTouchBody(final int index) {

        touchBodies[index] = null;

        float smallNumber = Float.MIN_VALUE;
        world.QueryAABB(new QueryCallback() {
            @Override
            public boolean reportFixture(Fixture fixture) {
                if (touchBodies[index] == null && fixture.getBody() != plateCircle) {
                    touchBodies[index] = fixture.getBody();
                }
                return true;
            }
        }, touchPosition.x - smallNumber, touchPosition.y - smallNumber, touchPosition.x + smallNumber, touchPosition.y + smallNumber);
        if (touchBodies[index] == null) {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.linearDamping = Float.MAX_VALUE;
            bodyDef.fixedRotation = true;
            bodyDef.position.set(touchPosition.x, touchPosition.y);

            touchBodies[index] = world.createBody(bodyDef);

            CircleShape shape = new CircleShape();
            shape.setRadius(0.2f);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.restitution = 0;
            fixtureDef.friction = 1.0f;
            fixtureDef.density = 1.0f;

            touchBodies[index].createFixture(fixtureDef);
            touchBodies[index].setUserData(2108);

            shape.dispose();
        }

        MouseJointDef jointDef = new MouseJointDef();

        jointDef.bodyA = plateCircle;
        jointDef.bodyB = touchBodies[index];


        jointDef.target.set(touchPosition.x, touchPosition.y);
        jointDef.maxForce = Float.MAX_VALUE;
        jointDef.collideConnected = true;

        mouseJoints[index] = (MouseJoint) world.createJoint(jointDef);


        return touchBodies[index];
    }

    private void destroyTouchBody(int index) {
        world.destroyJoint(mouseJoints[index]);
        mouseJoints[index] = null;
        if (touchBodies[index].getUserData() != null && (Integer) touchBodies[index].getUserData() == 2108) {

            Body b = touchBodies[index];
            boolean usedElsewhere = false;
            for (int i = 0; i < touchBodies.length; ++i) {
                if (i != index && touchBodies[i] != null && touchBodies[i] == b) {
                    usedElsewhere = true;
                    break;
                }
            }

            if (!usedElsewhere) {
                world.destroyBody(touchBodies[index]);
            }



        }

        touchBodies[index] = null;
    }

    @Override
    public void show() {

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {
        timer.stop();

    }

    @Override
    public void resume() {
        timer.start();
    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        lettersAtlas.dispose();
        timer.clear();
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {

        if (pointer == 0) {
            starting = gray;
            target = colored;
            interpolationStartTime = System.currentTimeMillis();
        }

        if (button != Input.Buttons.LEFT || pointer > maxTouches - 1) {
            return false;
        } else {
            touchPosition.set(screenX, screenY, 0);
            touchPosition = camera.unproject(touchPosition);
            createTouchBody(pointer);
            //addWaterCricle(touchPosition.x, touchPosition.y, pointer);
            return true;
        }
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {

        if (pointer == 0) {
            starting = colored;
            target = gray;
            interpolationStartTime = System.currentTimeMillis() + 10000;
        }

        if (button != Input.Buttons.LEFT || pointer > maxTouches -1) {
            return false;
        } else {
            destroyTouchBody(pointer);
            return true;
        }
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer > maxTouches - 1) {
            return false;
        } else {
            touchPosition.set(screenX, screenY, 0);
            touchPosition = camera.unproject(touchPosition);

            mouseJoints[pointer].setTarget(new Vector2(touchPosition.x, touchPosition.y));
            //addWaterCricle(touchPosition.x, touchPosition.y, pointer);
            return true;
        }
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
