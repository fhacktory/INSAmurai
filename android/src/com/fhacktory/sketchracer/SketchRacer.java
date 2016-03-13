package com.fhacktory.sketchracer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;


public class SketchRacer extends ApplicationAdapter {

    private final static float MAX_STEER_ANGLE = (float) (Math.PI/4);
    private final static float STEER_SPEED = 1.5f;
    private final static float HORSEPOWERS = 60;

    private final static Vector2 leftRearWheelPosition = new Vector2(-1.5f,1.9f);
    private final static Vector2 rightRearWheelPosition = new Vector2(1.5f,1.9f);
    private final static Vector2 leftFrontWheelPosition = new Vector2(-1.5f,-1.9f);
    private final static Vector2 rightFrontWheelPosition = new Vector2(1.5f,-1.9f);

    private float engineSpeed = HORSEPOWERS;
    private float steeringAngle = 0;

    private World world;
    private Texture img;
    private Sprite sprite;
    private Body body, leftWheel, rightWheel, leftRearWheel, rightRearWheel;
    private RevoluteJoint leftJoint, rightJoint;
    private Box2DDebugRenderer debugRenderer;
    private Matrix4 debugMatrix;
    private OrthographicCamera camera;
    private SpriteBatch batch;

    private Skin touchpadSkin;
    private Touchpad.TouchpadStyle touchpadStyle;
    private Drawable touchBackground, touchKnob;
    private Touchpad touchpad;

    private Stage stage;

    private float targetAngle = -(float)Math.PI/2;

    private Circuit circuit;
    private int turns;

    private List<Vector2> inside, outside;
    private Vector2 start;

    private int lapIndex = 0;
    private int lapIndexReverse = 0;

    private final int lapFirstIndex;

    Vector2 startingPos;

    long startMillis;

    private Activity act;

    public SketchRacer(Circuit circuit, int turns, Activity act) {
        this.circuit = circuit;
        this.turns = turns;
        this.act = act;

        //find the closest point to the beginning
        int indexClosest = -1;
        double distanceClosest = Double.MAX_VALUE;
        List<Point> inner = circuit.getInside();
        for(int i = 0; i < inner.size(); i++) {
            if(Math.sqrt(Math.pow(circuit.getStart().x - inner.get(i).x, 2)
                        +Math.pow(circuit.getStart().y - inner.get(i).y, 2)) < distanceClosest) {
                distanceClosest = Math.sqrt(Math.pow(circuit.getStart().x - inner.get(i).x, 2)
                        +Math.pow(circuit.getStart().y - inner.get(i).y, 2));

                indexClosest = i;
            }
        }

        Log.d("SketchRacer", "indexClosest is "+indexClosest+", distance is "+distanceClosest);
        lapFirstIndex = indexClosest;
        lapIndex = indexClosest + 1;
        lapIndexReverse = indexClosest - 1;

        startMillis = System.currentTimeMillis();
    }

    @Override
    public void create() {
        batch = new SpriteBatch();

        world = new World(new Vector2(0,0), true);

        //Create a Stage and add TouchPad
        stage = new Stage(new ScreenViewport());
        //Create a touchpad skin
        touchpadSkin = new Skin();
        //Set background image
        touchpadSkin.add("touchBackground", new Texture("data/touchBackground.png"));
        //Set knob image
        touchpadSkin.add("touchKnob", new Texture("data/touchKnob.png"));
        //Create TouchPad Style
        touchpadStyle = new Touchpad.TouchpadStyle();
        //Create Drawable's from TouchPad skin
        touchBackground = touchpadSkin.getDrawable("touchBackground");
        touchKnob = touchpadSkin.getDrawable("touchKnob");
        //Apply the Drawables to the TouchPad Style
        touchpadStyle.background = touchBackground;
        touchpadStyle.knob = touchKnob;
        //Create new TouchPad with the created style
        touchpad = new Touchpad(10, touchpadStyle);
        //setBounds(x,y,width,height)
        touchpad.setBounds(30, 30, 300, 300);
        stage.addActor(touchpad);

        // Add timer label
        /*
        Skin timerSkin = new Skin();
        Label.LabelStyle timerStyle = new Label.LabelStyle();
        timerStyle.font = timerSkin.getFont("data/default.fnt");
        Label timer = new Label("Hello", timerStyle);
        timer.setBounds(Gdx.graphics.getWidth() - 100, 10, Gdx.graphics.getWidth() - 10, 50);
*/
        createCircuit();

        createCar();

        Gdx.input.setInputProcessor(stage);

        // Create a Box2DDebugRenderer, this allows us to see the physics simulation controlling the scene
        debugRenderer = new Box2DDebugRenderer();
        camera = new OrthographicCamera(20f*Gdx.graphics.getWidth()/Gdx.graphics.getHeight(),20f);

        touchpad.addListener(new InputListener() {
            @Override
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button)
            {
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer)
            {
                float xp = touchpad.getKnobPercentX();
                float yp = touchpad.getKnobPercentY();
                if(xp == 0) {
                    targetAngle = yp >= 0 ? (float)Math.PI/2f : -(float)Math.PI/2f;
                } else {
                    targetAngle = (float)Math.atan(yp/xp);
                    if(xp < 0) targetAngle += (float) Math.PI;
                }
            }

            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button)
            {
                targetAngle = body.getAngle() - (float)Math.PI/2f;
            }
        });

    }

    private void createCar() {
        //Create sprite
        img = new Texture("car.png");
        sprite = new Sprite(img);
        sprite.setSize(3f, 5f);
        sprite.setPosition(-sprite.getWidth() / 2, -sprite.getHeight() / 2);

        // Define our body
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.linearDamping = 1;
        bodyDef.angularDamping = 1;
        bodyDef.position.set(startingPos.cpy());

        body = world.createBody(bodyDef);

        BodyDef leftWheelDef = new BodyDef();
        leftWheelDef.type = BodyDef.BodyType.DynamicBody;
        leftWheelDef.position.set(startingPos.cpy());
        leftWheelDef.position.add(leftFrontWheelPosition);
        leftWheel = world.createBody(leftWheelDef);

        BodyDef rightWheelDef = new BodyDef();
        rightWheelDef.type = BodyDef.BodyType.DynamicBody;
        rightWheelDef.position.set(startingPos.cpy());
        rightWheelDef.position.add(rightFrontWheelPosition);
        rightWheel = world.createBody(rightWheelDef);

        BodyDef leftRearWheelDef = new BodyDef();
        leftRearWheelDef.type = BodyDef.BodyType.DynamicBody;
        leftRearWheelDef.position.set(startingPos.cpy());
        leftRearWheelDef.position.add(leftRearWheelPosition);
        leftRearWheel = world.createBody(leftRearWheelDef);

        BodyDef rightRearWheelDef = new BodyDef();
        rightRearWheelDef.type = BodyDef.BodyType.DynamicBody;
        rightRearWheelDef.position.set(startingPos.cpy());
        rightRearWheelDef.position.add(rightRearWheelPosition);
        rightRearWheel = world.createBody(rightRearWheelDef);

        // Define our shapes
        PolygonShape boxDef = new PolygonShape();
        boxDef.setAsBox(1.5f,2.5f);
        FixtureDef boxFixture = new FixtureDef();
        boxFixture.density = 1;
        boxFixture.shape = boxDef;
        boxFixture.restitution = 0.1f;
        body.createFixture(boxFixture);

        // Left wheel shape
        PolygonShape leftWheelShapeDef = new PolygonShape();
        leftWheelShapeDef.setAsBox(0.2f,0.5f);
        FixtureDef leftWheelFixture = new FixtureDef();
        leftWheelFixture.density = 1;
        leftWheelFixture.shape = leftWheelShapeDef;
        leftWheel.createFixture(leftWheelFixture);

        // Right wheel shape
        PolygonShape rightWheelShapeDef = new PolygonShape();
        rightWheelShapeDef.setAsBox(0.2f, 0.5f);
        FixtureDef rightWheelFixture = new FixtureDef();
        rightWheelFixture.density = 1;
        rightWheelFixture.shape = rightWheelShapeDef;
        rightWheel.createFixture(rightWheelFixture);

        // Left rear wheel shape
        PolygonShape leftRearWheelShapeDef = new PolygonShape();
        leftRearWheelShapeDef.setAsBox(0.2f,0.5f);
        FixtureDef leftRearWheelFixture = new FixtureDef();
        leftRearWheelFixture.density = 1;
        leftRearWheelFixture.shape = leftRearWheelShapeDef;
        leftRearWheel.createFixture(leftRearWheelFixture);

        // Right rear wheel shape
        PolygonShape rightRearWheelShapeDef = new PolygonShape();
        rightRearWheelShapeDef.setAsBox(0.2f, 0.5f);
        FixtureDef rightRearWheelFixture = new FixtureDef();
        rightRearWheelFixture.density = 1;
        rightRearWheelFixture.shape = rightRearWheelShapeDef;
        rightRearWheel.createFixture(rightRearWheelFixture);

        RevoluteJointDef leftJointDef = new RevoluteJointDef();
        leftJointDef.initialize(body,leftWheel,leftWheel.getWorldCenter());
        leftJointDef.enableMotor = true;
        leftJointDef.maxMotorTorque = 100;

        RevoluteJointDef rightJointDef = new RevoluteJointDef();
        rightJointDef.initialize(body,rightWheel,rightWheel.getWorldCenter());
        rightJointDef.enableMotor = true;
        rightJointDef.maxMotorTorque = 100;

        leftJoint = (RevoluteJoint)world.createJoint(leftJointDef);
        rightJoint = (RevoluteJoint)world.createJoint(rightJointDef);

        PrismaticJointDef leftRearJointDef = new PrismaticJointDef();
        leftRearJointDef.initialize(body,leftRearWheel,leftRearWheel.getWorldCenter(),new Vector2(1,0));
        leftRearJointDef.enableLimit = true;
        leftRearJointDef.lowerTranslation = 0;
        leftRearJointDef.upperTranslation = 0;

        PrismaticJointDef rightRearJointDef = new PrismaticJointDef();
        rightRearJointDef.initialize(body, rightRearWheel, rightRearWheel.getWorldCenter(), new Vector2(1, 0));
        rightRearJointDef.enableLimit = true;
        rightRearJointDef.lowerTranslation = 0;
        rightRearJointDef.upperTranslation = 0;

        world.createJoint(leftRearJointDef);
        world.createJoint(rightRearJointDef);
    }

    private void createCircuit() {
        // re-scale circuit shape
        List<Point> inside = circuit.getInside();
        List<Point> outside = circuit.getOutside();
        List<Vector2> newInside = new ArrayList<Vector2>(inside.size());
        List<Vector2> newOutside = new ArrayList<Vector2>(outside.size());
        Vector2 start = new Vector2(circuit.getStart().x, circuit.getStart().y);
        int midX = (circuit.getMaxX() - circuit.getMinX())/2;
        int midY = (circuit.getMaxY() - circuit.getMinY())/2;
        for(Point p : inside) {
            newInside.add(new Vector2((p.x - midX)/5, (p.y - midY)/5));
        }
        for(Point p : outside) {
            newOutside.add(new Vector2((p.x - midX)/5, (p.y - midY)/5));
        }
        start.x = (start.x - midX)/5;
        start.y = (start.y - midY)/5;
        startingPos = new Vector2(start.x,start.y);
        // build walls
        BodyDef wallDef;
        Body wall;
        EdgeShape wallShape;
        FixtureDef wallFixtureDef;
        for(int i = 0; i < newInside.size() - 1; i++) {
            wallDef = new BodyDef();
            wallDef.type = BodyDef.BodyType.StaticBody;
            wallDef.position.set(0,0);
            wall = world.createBody(wallDef);
            wallShape = new EdgeShape();
            wallShape.set(newInside.get(i).x,newInside.get(i).y,newInside.get(i+1).x,newInside.get(i+1).y);
            wallFixtureDef = new FixtureDef();
            wallFixtureDef.shape = wallShape;
            wallFixtureDef.density = 1;
            wall.createFixture(wallFixtureDef);
            wallShape.dispose();
        }
        wallDef = new BodyDef();
        wallDef.type = BodyDef.BodyType.StaticBody;
        wallDef.position.set(0,0);
        wall = world.createBody(wallDef);
        wallShape = new EdgeShape();
        wallShape.set(newInside.get(newInside.size() - 1).x,newInside.get(newInside.size() - 1).y,newInside.get(0).x,newInside.get(0).y);
        wallFixtureDef = new FixtureDef();
        wallFixtureDef.shape = wallShape;
        wallFixtureDef.density = 1;
        wall.createFixture(wallFixtureDef);
        wallShape.dispose();
        for(int i = 0; i < newOutside.size() - 1; i++) {
            wallDef = new BodyDef();
            wallDef.type = BodyDef.BodyType.StaticBody;
            wallDef.position.set(0,0);
            wall = world.createBody(wallDef);
            wallShape = new EdgeShape();
            wallShape.set(newOutside.get(i).x,newOutside.get(i).y,newOutside.get(i+1).x,newOutside.get(i+1).y);
            wallFixtureDef = new FixtureDef();
            wallFixtureDef.shape = wallShape;
            wallFixtureDef.density = 1;
            wall.createFixture(wallFixtureDef);
            wallShape.dispose();
        }
        wallDef = new BodyDef();
        wallDef.type = BodyDef.BodyType.StaticBody;
        wallDef.position.set(0, 0);
        wall = world.createBody(wallDef);
        wallShape = new EdgeShape();
        wallShape.set(newOutside.get(newOutside.size() - 1).x,newOutside.get(newOutside.size() - 1).y,newOutside.get(0).x,newOutside.get(0).y);
        wallFixtureDef = new FixtureDef();
        wallFixtureDef.shape = wallShape;
        wallFixtureDef.density = 1;
        wall.createFixture(wallFixtureDef);
        wallShape.dispose();
        this.inside = newInside;
        this.outside = newOutside;
        this.start = start;
    }

    @Override
    public void render() {
        camera.position.set(body.getPosition(), camera.position.z);
        //System.out.println(camera.position.x + ";" + camera.position.y);
        camera.update();
        // Step the physics simulation forward at a rate of 60hz
        world.step(1f/30f, 8, 2);
        killOrthogonalVelocity(leftWheel);
        killOrthogonalVelocity(rightWheel);
        killOrthogonalVelocity(leftRearWheel);
        killOrthogonalVelocity(rightRearWheel);

        //Driving
        Vector2 ldirection = getRcol2(leftWheel);
        ldirection.scl(engineSpeed);
        Vector2 rdirection = getRcol2(rightWheel);
        rdirection.scl(engineSpeed);
        leftWheel.applyForce(ldirection, leftWheel.getPosition(), true);
        rightWheel.applyForce(rdirection, rightWheel.getPosition(), true);

        //Steering
        float currentAngle = body.getAngle() - (float)Math.PI / 2f;
        steeringAngle = (float)((targetAngle - currentAngle + Math.PI) % (2 * Math.PI) - Math.PI);
        if(steeringAngle > MAX_STEER_ANGLE) steeringAngle = MAX_STEER_ANGLE;
        else if(steeringAngle < -MAX_STEER_ANGLE) steeringAngle = -MAX_STEER_ANGLE;

        float mspeed;
        mspeed = steeringAngle - leftJoint.getJointAngle();
        leftJoint.setMotorSpeed(mspeed * STEER_SPEED);
        mspeed = steeringAngle - rightJoint.getJointAngle();
        rightJoint.setMotorSpeed(mspeed * STEER_SPEED);

        // Set the sprite's position from the updated physics carBody location
        sprite.setPosition(body.getPosition().x - sprite.getWidth() / 2f,
                body.getPosition().y - sprite.getHeight() / 2f);
        // Ditto for rotation
        sprite.setRotation((float)Math.toDegrees(body.getAngle()));

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);

        // Scale down the sprite batches projection matrix to box2D size
        debugMatrix = batch.getProjectionMatrix().cpy();

        batch.begin();

        batch.draw(sprite, sprite.getX(), sprite.getY(), sprite.getWidth() / 2f,
                sprite.getHeight() / 2f,
                sprite.getWidth(), sprite.getHeight(), sprite.getScaleX(), sprite.
                        getScaleY(), sprite.getRotation());

        batch.end();
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        // Now render the physics world using our scaled down matrix
        // Note, this is strictly optional and is, as the name suggests, just for debugging purposes
        debugRenderer.render(world, debugMatrix);

        // Check checkpoints
        if(Math.sqrt(Math.pow(inside.get(lapIndex).x - body.getPosition().x, 2)
                +Math.pow(inside.get(lapIndex).y - body.getPosition().y, 2)) < Circuit.WIDTH*2 / 5) {
            Log.d("SketchRacer", "Passed checkpoint "+lapIndex+"!");
            if(lapIndex == lapFirstIndex) {
                turns--;
                Log.i("SketchRacer", turns+" turns left!");

                if(turns == 0) finishLaps();
            }

            lapIndex++;
            if(lapIndex == inside.size()) lapIndex = 0;
        } else if(Math.sqrt(Math.pow(inside.get(lapIndexReverse).x - body.getPosition().x, 2)
                +Math.pow(inside.get(lapIndexReverse).y - body.getPosition().y, 2)) < Circuit.WIDTH*2 / 5) {
            Log.d("SketchRacer", "Passed reverse checkpoint "+lapIndexReverse+"!");
            if(lapIndexReverse == lapFirstIndex) {
                turns--;
                Log.i("SketchRacer", turns+" turns left!");

                if(turns == 0) finishLaps();
            }

            lapIndexReverse--;
            if(lapIndexReverse == -1) lapIndexReverse = inside.size() - 1;
        }
    }

    private String getRunTime() {
        DecimalFormat two = new DecimalFormat("00");
        DecimalFormat three = new DecimalFormat("000");

        long diffMillis = System.currentTimeMillis() - startMillis;
        long mins = diffMillis / 60000;
        long secs = diffMillis / 1000 - mins*60;
        long millis = diffMillis % 1000;

        return two.format(mins)+":"+two.format(secs)+"."+three.format(millis);
    }

    private void finishLaps() {
        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(act)
                        .setTitle(act.getString(R.string.race_end))
                        .setMessage(act.getString(R.string.race_result)+getRunTime())
                        .setPositiveButton(act.getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                act.finish();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        });
    }

    @Override
    public void dispose() {
        img.dispose();
        world.dispose();
    }

    private void killOrthogonalVelocity(Body targetBody) {
        Vector2 localPoint = new Vector2(0,0);
        Vector2 velocity = targetBody.getLinearVelocityFromLocalPoint(localPoint);
        Vector2 sidewaysAxis = getRcol2(targetBody);
        sidewaysAxis.scl(velocity.dot(sidewaysAxis));
        targetBody.setLinearVelocity(sidewaysAxis);//targetBody.GetWorldPoint(localPoint));
    }

    private Vector2 getRcol2(Body targetBody) {
        float angle = targetBody.getTransform().getRotation();
        return new Vector2((float)Math.sin(angle), -(float)Math.cos(angle));
    }

}