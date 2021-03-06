package com.milkmaid.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/**
 * Created by maximus_prime on 27/9/15.
 * Painter class handles all the rendering
 */

public class Painter implements Screen {

    private Model.GameState PaintingMode = Model.GameState.NORMAL;

    private final String TAG = "PAINTER";

    protected OrthographicCamera camera;
    protected ShapeRenderer debugRenderer = new ShapeRenderer();
    protected final int Width,Height;
    protected TextureRegion Regions[][];
    protected Sprite BackgroundSprites[] = new Sprite[4];
    private final Sprite BackgroundGlow;

    protected SpriteBatch batch;
    protected BitmapFont Score;
    protected GameSuperviser Superviser;
    private final Player crazyFrog;

    Painter(GameSuperviser superviser,Player crazyF) {

        Superviser = superviser;
        Width = Superviser.getWidth();
        Height = Superviser.getHeight();
        camera = superviser.getDisplayCamera();
        crazyFrog = crazyF;
        Score = new BitmapFont();
        Score.setColor(Color.CYAN);
        Score.getData().setScale(3);

        Texture SpriteSheet = new Texture(Gdx.files.internal("sprites_ext.png"));
        Texture background = new Texture(Gdx.files.internal("backgroundtexture2.png"));
        TextureRegion Backgrounds[][] = TextureRegion.split(background,301,200);

        int width = SpriteSheet.getWidth()/4,height = SpriteSheet.getHeight()/2;
        Regions = TextureRegion.split(SpriteSheet,width,height);

        BackgroundGlow = new Sprite(Regions[1][0]);
        BackgroundGlow.setScale(1.5f);

        BackgroundSprites[0] = new Sprite(Backgrounds[0][0]);
        BackgroundSprites[1] = new Sprite(Backgrounds[0][1]);
        BackgroundSprites[2] = new Sprite(Backgrounds[1][0]);
        BackgroundSprites[3] = new Sprite(Backgrounds[1][1]);

        int x = 0;
        for(Sprite s:BackgroundSprites) {
            s.setScale(2);
            s.setPosition(x, 250);
            Rectangle r = s.getBoundingRectangle();
            x += r.getWidth();
            /*
            Gdx.app.log(TAG, "RegionInfo"+ r.getX() +"|" + r.getY() +"|" +
                        r.getWidth() +"|" + r.getHeight());
            Gdx.app.log(TAG, "SpriteInfo"+ s.getX() +"|" + s.getY() +"|" +
                    s.getWidth() +"|" + s.getHeight());
            */
        }

        batch = new SpriteBatch();

    }

    public void updateBackground(float Velocity) {

        int camera_bottom = (int) (camera.position.x - camera.viewportWidth/2);

        for(Sprite s : BackgroundSprites) {

            s.setPosition(s.getX()+Velocity,s.getY());
            Rectangle r = s.getBoundingRectangle();
            int rtop = (int) (r.getX()+r.getWidth());
            if(rtop < camera_bottom) {
                s.setPosition(s.getX()+r.getWidth()*4,250);
            }
        }
    }

    public void setPaintingMode(Model.GameState g) {
        PaintingMode = g;
    }

    @Override
    public void render(float v) {

        NormalPaint();
    }

    private void NormalPaint() {

//        Vertex.Reset(); //reset the Is explored boolean value

        int camera_top = (int) (camera.position.x + camera.viewportWidth/2); //for optimization

        debugRenderer.setProjectionMatrix(camera.combined);//for working in camera coordinates
        batch.setProjectionMatrix(camera.combined);

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glLineWidth(30);

        batch.begin();
        for(Sprite s: BackgroundSprites) s.draw(batch); //render background
        batch.end();

        //Lines
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        debugRenderer.setColor(new Color(0.0f, 0.5f, 1f, 1));
        for(int i = 0;i<Model.VQueue.getSize();++i) {
            Vertex vertex = Model.VQueue.getVertex(i);

            if(vertex.x > camera_top) break;//since vertex is not visible

            for(Vertex e:vertex.getEdgeList()) {
                    debugRenderer.line(vertex,e);
            }
        }
        debugRenderer.end();

        batch.begin();
        for(int i = 0;i<Model.VQueue.getSize();++i) {

            Vertex vertex = Model.VQueue.getVertex(i);
            if(vertex.x > camera_top ) break;

            switch (vertex.getCurrentState()) {

                case Reachable:
                    //draw holow background then original image
                    BackgroundGlow.setPosition(vertex.x - Regions[1][0].getRegionWidth()/2,
                            vertex.y-Regions[1][0].getRegionHeight()/2);
                    BackgroundGlow.draw(batch);
                case Alive:
                    batch.draw(Regions[1][1], vertex.x - Regions[1][1].getRegionWidth() / 2,
                            vertex.y-Regions[1][1].getRegionHeight()/2);
                    break;
                case Touched:
                    batch.draw(Regions[0][2], vertex.x - Regions[0][2].getRegionWidth()/2,
                            vertex.y-Regions[0][2].getRegionHeight()/2);
                    break;
                case Dead:
                    break;

            }
            switch (vertex.getVertexType()) {

                case Normal: break;
                case Sharper:
                    batch.draw(Regions[0][1], vertex.x - Regions[0][1].getRegionWidth()/2,
                            vertex.y-Regions[0][1].getRegionHeight()/2);
                    break;
                case Stronger:
                    batch.draw(Regions[0][0], vertex.x - Regions[0][0].getRegionWidth()/2,
                            vertex.y-Regions[0][0].getRegionHeight()/2);
                    break;
            }
        }

        batch.end();
        camera.rotate(-90);
        camera.update();
        camera_top = (int) (camera.position.y + camera.viewportWidth/2);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        Score.draw(batch, "" + Superviser.getScore(), camera.position.x-300, camera_top-10);
        batch.end();

        camera.rotate(90);
        camera.update();
    }

    @Override
    public void show() {

    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {

    }
}
