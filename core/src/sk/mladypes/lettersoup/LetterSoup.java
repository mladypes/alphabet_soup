package sk.mladypes.lettersoup;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2D;

public class LetterSoup extends Game {
	private SpriteBatch batch;
    public final String screensDirectory = "soup-screens";

	
	@Override
	public void create () {

	    if (!Gdx.files.external(screensDirectory).exists() || !Gdx.files.external(screensDirectory).isDirectory()) {
            Gdx.files.external(screensDirectory).mkdirs();
        }

		batch = new SpriteBatch();

		setScreen(new SoupScreen(this));
        Box2D.init();
	}

    public SpriteBatch getGlobalSpriteBatch() {
        return batch;
    }

	@Override
	public void render () {
		super.render();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}
}
