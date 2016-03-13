package com.fhacktory.sketchracer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
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

public class SketchRacer extends ApplicationAdapter {

    private final static float MAX_STEER_ANGLE = (float) (Math.PI/3);
    private final static float STEER_SPEED = 1.5f;
    private final static float SIDEWAYS_FRICTION_FORCE = 10;
    private final static float HORSEPOWERS = 40;
    private final static Vector2 CAR_STARTING_POS = new Vector2(0,0);

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

    public SketchRacer(Circuit circuit, int turns) {
        this.circuit = circuit;
        this.turns = turns;
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

        //Create car
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
                    targetAngle = y >= 0 ? (float)Math.PI/2 : -(float)Math.PI/2;
                } else {
                    targetAngle = (float)Math.atan(yp/xp);
                    if(xp < 0) targetAngle += (float) Math.PI;
                }
            }

            @Override
            public void touchUp (InputEvent event, float x, float y, int pointer, int button)
            {
                targetAngle = body.getAngle() - (float)Math.PI / 2;
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
        bodyDef.position.set(CAR_STARTING_POS.cpy());

        body = world.createBody(bodyDef);

        BodyDef leftWheelDef = new BodyDef();
        leftWheelDef.type = BodyDef.BodyType.DynamicBody;
        leftWheelDef.position.set(CAR_STARTING_POS.cpy());
        leftWheelDef.position.add(leftFrontWheelPosition);
        leftWheel = world.createBody(leftWheelDef);

        BodyDef rightWheelDef = new BodyDef();
        rightWheelDef.type = BodyDef.BodyType.DynamicBody;
        rightWheelDef.position.set(CAR_STARTING_POS.cpy());
        rightWheelDef.position.add(rightFrontWheelPosition);
        rightWheel = world.createBody(rightWheelDef);

        BodyDef leftRearWheelDef = new BodyDef();
        leftRearWheelDef.type = BodyDef.BodyType.DynamicBody;
        leftRearWheelDef.position.set(CAR_STARTING_POS.cpy());
        leftRearWheelDef.position.add(leftRearWheelPosition);
        leftRearWheel = world.createBody(leftRearWheelDef);

        BodyDef rightRearWheelDef = new BodyDef();
        rightRearWheelDef.type = BodyDef.BodyType.DynamicBody;
        rightRearWheelDef.position.set(CAR_STARTING_POS.cpy());
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

    private Body createWall(float x1, float y1, float x2, float y2) {
        BodyDef boxBodyDef = new BodyDef();
        boxBodyDef.type = BodyDef.BodyType.StaticBody;
        boxBodyDef.position.set(0, 0);

        Body boxBody = world.createBody(boxBodyDef);

        PolygonShape boxShape = new PolygonShape();
        Vector2[] vertices = new Vector2[4];
        vertices[0] = new Vector2(x1, y1);
        vertices[1] = new Vector2(x2, y1);
        vertices[2] = new Vector2(x2, y2);
        vertices[3] = new Vector2(x1, y2);
        boxShape.set(vertices);

        FixtureDef boxFixtureDef = new FixtureDef();
        boxFixtureDef.shape = boxShape;
        boxFixtureDef.density = 1;

        boxBody.createFixture(boxFixtureDef);
        boxShape.dispose();
        return boxBody;
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
        float currentAngle = body.getAngle() - (float)Math.PI / 2;
        steeringAngle = (float)((targetAngle - currentAngle + Math.PI) % (2 * Math.PI) - Math.PI);
        if(steeringAngle > MAX_STEER_ANGLE) steeringAngle = MAX_STEER_ANGLE;
        else if(steeringAngle < -MAX_STEER_ANGLE) steeringAngle = -MAX_STEER_ANGLE;

        float mspeed;
        mspeed = steeringAngle - leftJoint.getJointAngle();
        leftJoint.setMotorSpeed(mspeed * STEER_SPEED);
        mspeed = steeringAngle - rightJoint.getJointAngle();
        rightJoint.setMotorSpeed(mspeed * STEER_SPEED);

        // Set the sprite's position from the updated physics carBody location
        sprite.setPosition(body.getPosition().x - sprite.getWidth() / 2,
                body.getPosition().y - sprite.getHeight() / 2);
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
    }

    @Override
    public void dispose() {
        img.dispose();
        world.dispose();
    }

    public boolean touchDragged(int screenX, int screenY, int pointer) {
        //System.out.println(screenX + ";" + screenY);
		/*Vector2 voiture = body.getPosition();
		Vector2 tap = new Vector2(screenX - Gdx.graphics.getWidth()/2f, screenY - Gdx.graphics.getHeight()/2f);
		body.applyForceToCenter(tap.x - voiture.x, voiture.y - tap.y, true);
		System.out.println("Force : "+(tap.x - voiture.x)+";"+(voiture.y - tap.y));
		return true;*/
        return false;
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

    private int lastFinger = -1;
    // On touch we apply force from the direction of the users touch.
    // This could result in the object "spinning"

    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		/*
		lastFinger = pointer;
		engineSpeed = HORSEPOWERS;
		if(screenX < Gdx.graphics.getWidth()/2f) {
			steeringAngle += MAX_STEER_ANGLE;
			if(steeringAngle > MAX_STEER_ANGLE) steeringAngle = MAX_STEER_ANGLE;
		} else {
			steeringAngle -= MAX_STEER_ANGLE;
			if(steeringAngle < -MAX_STEER_ANGLE) steeringAngle = -MAX_STEER_ANGLE;
		}
		*/
        return false;
    }


    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		/*
		if(pointer == lastFinger) {
			engineSpeed = 0;
			steeringAngle = 0;
		}
		*/
        return false;
    }

}