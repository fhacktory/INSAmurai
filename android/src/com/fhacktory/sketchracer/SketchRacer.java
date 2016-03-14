package com.fhacktory.sketchracer;

import android.content.DialogInterface;
import android.graphics.Point;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.SeekBar;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class SketchRacer extends ApplicationAdapter {

    private final static float MAX_STEER_ANGLE = (float) (Math.PI/4);
    private final static float STEER_SPEED = 1.5f;
    public final static float HORSEPOWERS = 60;
    private final static float LINE_WIDTH = 1;

    private final static Vector2 leftRearWheelPosition = new Vector2(-1.5f,1.9f);
    private final static Vector2 rightRearWheelPosition = new Vector2(1.5f,1.9f);
    private final static Vector2 leftFrontWheelPosition = new Vector2(-1.5f,-1.9f);
    private final static Vector2 rightFrontWheelPosition = new Vector2(1.5f,-1.9f);

    private float engineSpeed = 0;
    private float steeringAngle = 0;

    private World world;
    private Texture img;
    private Sprite sprite;
    private Body body, leftWheel, rightWheel, leftRearWheel, rightRearWheel, startLine;
    private RevoluteJoint leftJoint, rightJoint;
    private Box2DDebugRenderer debugRenderer;
    private Matrix4 debugMatrix;
    private OrthographicCamera camera;

    private Skin touchpadSkin;
    private Touchpad.TouchpadStyle touchpadStyle;
    private Drawable touchBackground, touchKnob;
    private Touchpad touchpad;

    private SpriteBatch batch;
    private ShapeRenderer renderer;

    private Stage stage;

    private float targetAngle = -(float)Math.PI/2;

    private Circuit circuit;
    private int turns;
    private final int totalTurns;

    private List<Vector2> inside, outside;
    private Vector2 start;
    private Vector2[] startVertices;

    private int lapIndex = 0;
    private int lapIndexReverse = 0;

    private final int lapFirstIndex;

    Vector2 startingPos;
    Vector2 startingDir;

    long startMillis = 0;

    private AndroidLauncher act;

    public SketchRacer(Circuit circuit, int turns, SeekBar accelerator, AndroidLauncher act) {
        this.circuit = circuit;
        this.turns = turns;
        this.totalTurns = turns;
        this.act = act;

        accelerator.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                engineSpeed = progress - 20;
                if(startMillis == 0) {
                    startMillis = System.currentTimeMillis();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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

        if(lapIndex >= inner.size()) lapIndex = 0;
        if(lapIndexReverse < 0) lapIndexReverse = inner.size() - 1;

        act.setHud1(act.getString(R.string.lap)+(totalTurns - turns + 1)+"/"+totalTurns);
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        renderer = new ShapeRenderer();

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

        createCircuit();

        createCar();

        Gdx.input.setInputProcessor(stage);

        // Create a Box2DDebugRenderer, this allows us to see the physics simulation controlling the scene
        //debugRenderer = new Box2DDebugRenderer();
        camera = new OrthographicCamera(20f*Gdx.graphics.getWidth()/Gdx.graphics.getHeight(),20f);

        touchpad.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                float xp = touchpad.getKnobPercentX();
                float yp = touchpad.getKnobPercentY();
                targetAngle = (float)Math.atan2(yp, xp);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                targetAngle = body.getTransform().getRotation() - (float) Math.PI / 2f;
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
        boxFixture.restitution = 1f;
        boxFixture.filter.groupIndex = -8;
        body.createFixture(boxFixture);

        // Left wheel shape
        PolygonShape leftWheelShapeDef = new PolygonShape();
        leftWheelShapeDef.setAsBox(0.2f,0.5f);
        FixtureDef leftWheelFixture = new FixtureDef();
        leftWheelFixture.density = 1;
        leftWheelFixture.shape = leftWheelShapeDef;
        leftWheelFixture.filter.groupIndex = -8;
        leftWheel.createFixture(leftWheelFixture);

        // Right wheel shape
        PolygonShape rightWheelShapeDef = new PolygonShape();
        rightWheelShapeDef.setAsBox(0.2f, 0.5f);
        FixtureDef rightWheelFixture = new FixtureDef();
        rightWheelFixture.density = 1;
        rightWheelFixture.shape = rightWheelShapeDef;
        rightWheelFixture.filter.groupIndex = -8;
        rightWheel.createFixture(rightWheelFixture);

        // Left rear wheel shape
        PolygonShape leftRearWheelShapeDef = new PolygonShape();
        leftRearWheelShapeDef.setAsBox(0.2f,0.5f);
        FixtureDef leftRearWheelFixture = new FixtureDef();
        leftRearWheelFixture.density = 1;
        leftRearWheelFixture.shape = leftRearWheelShapeDef;
        leftRearWheelFixture.filter.groupIndex = -8;
        leftRearWheel.createFixture(leftRearWheelFixture);

        // Right rear wheel shape
        PolygonShape rightRearWheelShapeDef = new PolygonShape();
        rightRearWheelShapeDef.setAsBox(0.2f, 0.5f);
        FixtureDef rightRearWheelFixture = new FixtureDef();
        rightRearWheelFixture.density = 1;
        rightRearWheelFixture.shape = rightRearWheelShapeDef;
        rightRearWheelFixture.filter.groupIndex = -8;
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

        float angle = (float)(Math.PI/2f + Math.atan2(startingDir.y, startingDir.x));

        body.setTransform(body.getPosition(), angle);
        leftWheel.setTransform(leftWheel.getPosition(), angle);
        rightWheel.setTransform(rightWheel.getPosition(), angle);
        leftRearWheel.setTransform(leftRearWheel.getPosition(), angle);
        rightRearWheel.setTransform(rightRearWheel.getPosition(), angle);

        targetAngle = angle - (float)Math.PI/2f;
        //System.out.println(body.getAngle());
        //System.out.println(angle);
    }

    private void createCircuit() {
        // re-scale circuit shape
        List<Point> inside = circuit.getInside();
        List<Point> outside = circuit.getOutside();
        this.inside = new ArrayList<Vector2>(inside.size());
        this.outside = new ArrayList<Vector2>(outside.size());
        this.start = new Vector2(circuit.getStart().x, circuit.getStart().y);
        int midX = (circuit.getMaxX() - circuit.getMinX())/2;
        int midY = (circuit.getMaxY() - circuit.getMinY())/2;
        for(Point p : inside) {
            this.inside.add(new Vector2((p.x - midX)/5, -(p.y - midY)/5));
        }
        for(Point p : outside) {
            this.outside.add(new Vector2((p.x - midX)/5, -(p.y - midY)/5));
        }
        this.start.x = (this.start.x - midX)/5;
        this.start.y = -(this.start.y - midY)/5;
        startingPos = new Vector2(start.x,start.y);
        // build walls
        BodyDef wallDef;
        Body wall;
        EdgeShape wallShape;
        FixtureDef wallFixtureDef;
        for(int i = 0; i < this.inside.size() - 1; i++) {
            wallDef = new BodyDef();
            wallDef.type = BodyDef.BodyType.StaticBody;
            wallDef.position.set(0,0);
            wall = world.createBody(wallDef);
            wallShape = new EdgeShape();
            wallShape.set(this.inside.get(i).x,this.inside.get(i).y,this.inside.get(i+1).x,this.inside.get(i+1).y);
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
        wallShape.set(this.inside.get(this.inside.size() - 1).x, this.inside.get(this.inside.size() - 1).y, this.inside.get(0).x, this.inside.get(0).y);
        wallFixtureDef = new FixtureDef();
        wallFixtureDef.shape = wallShape;
        wallFixtureDef.density = 1;
        wall.createFixture(wallFixtureDef);
        wallShape.dispose();
        for(int i = 0; i < this.outside.size() - 1; i++) {
            wallDef = new BodyDef();
            wallDef.type = BodyDef.BodyType.StaticBody;
            wallDef.position.set(0,0);
            wall = world.createBody(wallDef);
            wallShape = new EdgeShape();
            wallShape.set(this.outside.get(i).x,this.outside.get(i).y,this.outside.get(i+1).x,this.outside.get(i+1).y);
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
        wallShape.set(this.outside.get(this.outside.size() - 1).x,this.outside.get(this.outside.size() - 1).y,this.outside.get(0).x,this.outside.get(0).y);
        wallFixtureDef = new FixtureDef();
        wallFixtureDef.shape = wallShape;
        wallFixtureDef.density = 1;
        wall.createFixture(wallFixtureDef);
        wallShape.dispose();
        // start/finish line
        BodyDef startLineDef = new BodyDef();
        startLineDef.type = BodyDef.BodyType.StaticBody;
        startLineDef.position.set(0,0);
        Body startLine = world.createBody(startLineDef);
        PolygonShape startLineShape = new PolygonShape();
        startVertices = new Vector2[4];
        startVertices[0] = this.inside.get(lapFirstIndex);
        startVertices[1] = this.outside.get(lapFirstIndex);
        Vector2 norm = new Vector2(-startVertices[0].y + startVertices[1].y,startVertices[0].x - startVertices[1].x);
        norm.scl(2f/norm.len());
        startVertices[2] = startVertices[0].cpy().add(norm);
        startVertices[3] = startVertices[1].cpy().add(norm);
        startLineShape.set(startVertices);
        FixtureDef startLineFixtureDef = new FixtureDef();
        startLineFixtureDef.shape = startLineShape;
        startLineFixtureDef.density = 0;
        startLineFixtureDef.filter.groupIndex = -8;
        startLine.createFixture(startLineFixtureDef);
        startLineShape.dispose();

        startingPos = startVertices[0].cpy().add(startVertices[0].cpy().sub(startVertices[1]).scl(-.5f));
        startingDir = norm;
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
        float currentAngle = body.getAngle() % ((float) Math.PI * 2) - (float)Math.PI / 2f;
        steeringAngle = (float)((targetAngle - currentAngle + 3*Math.PI) % (2 * Math.PI) - Math.PI);
        //trying to correct oscillating problem by taking the existing velocity into account
        steeringAngle -= body.getAngularVelocity() / 2;

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
        sprite.setRotation((float) Math.toDegrees(body.getAngle()));

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderer.setProjectionMatrix(camera.combined);
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(0, 0, 0, 1);
        for(int i=0;i<inside.size()-1;i++) {
            renderer.rectLine(inside.get(i), inside.get(i + 1), LINE_WIDTH);
        }
        renderer.rectLine(inside.get(inside.size() - 1), inside.get(0), LINE_WIDTH);
        for(int i=0;i<outside.size()-1;i++) {
            renderer.rectLine(outside.get(i), outside.get(i+1), LINE_WIDTH);
        }
        renderer.rectLine(outside.get(outside.size() - 1), outside.get(0), LINE_WIDTH);
        renderer.setColor(1,0,0,1);
        renderer.rectLine(startVertices[0],startVertices[3],2);
        renderer.end();

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
        //debugRenderer.render(world, debugMatrix);

        // Check checkpoints
        if(isOnTheLine(lapIndex)) {
            Log.v("SketchRacer", "Passed checkpoint "+lapIndex+"!");
            if(lapIndex == lapFirstIndex) {
                turns--;
                Log.i("SketchRacer", turns+" turns left!");

                if(turns == 0) finishLaps();
                else
                    act.setHud1Blink(act.getString(R.string.lap)+(totalTurns - turns + 1)+"/"+totalTurns);
            }

            lapIndex++;
            if(lapIndex == inside.size()) lapIndex = 0;
        } else if(isOnTheLine(lapIndexReverse)) {
            Log.v("SketchRacer", "Passed reverse checkpoint "+lapIndexReverse+"!");
            if(lapIndexReverse == lapFirstIndex) {
                turns--;
                Log.i("SketchRacer", turns + " turns left!");

                if(turns == 0) finishLaps();
                else
                    act.setHud1Blink(act.getString(R.string.lap)+(totalTurns - turns + 1)+"/"+totalTurns);
            }

            lapIndexReverse--;
            if(lapIndexReverse == -1) lapIndexReverse = inside.size() - 1;
        }

        if(turns == 0) {
            act.setHud1("");
            act.setHud2("");
        } else {
            act.setHud2(getRunTime());
        }
    }

    private boolean isOnTheLine(int lapIndex) {
        int startX = (int) inside.get(lapIndex).x;
        int startY = (int) inside.get(lapIndex).y;
        int endX = (int) outside.get(lapIndex).x;
        int endY = (int) outside.get(lapIndex).y;

        int carX = (int) body.getPosition().x;
        int carY = (int) body.getPosition().y;

        float dist1 = (float) (Math.sqrt((startX-carX) * (startX-carX) + (startY-carY) * (startY-carY)));
        float dist2 = (float) (Math.sqrt((endX-carX) * (endX-carX) + (endY-carY) * (endY-carY)));
        float dist3 = (float) (Math.sqrt((startX-endX) * (startX-endX) + (startY-endY) * (startY-endY)));

        //System.out.println(Math.abs(dist3 - (dist2 + dist1)));
        return Math.abs(dist3 - (dist2 + dist1)) < 1;
    }

    private String getRunTime() {
        DecimalFormat two = new DecimalFormat("00");
        DecimalFormat three = new DecimalFormat("000");
        long diffMillis = 0;
        if(startMillis != 0) {
            diffMillis = System.currentTimeMillis() - startMillis;
        }
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