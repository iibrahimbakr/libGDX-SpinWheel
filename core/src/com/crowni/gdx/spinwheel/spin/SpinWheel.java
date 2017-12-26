/*
 *******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.crowni.gdx.spinwheel.spin;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Thanks for NauticalMile answer's in stackexchange site
 * <li><a>
 * href="https://gamedev.stackexchange.com/questions/72170/how-simulate-the-return-effect-of-the-wheel-of-fortune-needle">
 * https://gamedev.stackexchange.com/questions/72170/how-simulate-the-return-effect-of-the-wheel-of-fortune-needle
 * </a></li> </ul>
 * <p>
 * You must call dispose(); method if spinning is no longer used.
 * </p>
 *
 * @author Crowni
 */
public class SpinWheel implements Disposable {
    public static final float PPM = 100f;
    private static final float STANDARD_SIZE = 512F;
    private static final short BIT_PEG = 4;
    private static final short BIT_NEEDLE = 8;
    private static final short BIT_B1 = 16;
    private static final short BIT_B2 = 32;

    private final World world;
    private final OrthographicCamera camera;
    private final Box2DDebugRenderer renderer;

    private final BodyDef bodyDef = new BodyDef();                                // general body definition.
    private final FixtureDef fixtureDef = new FixtureDef();                       // general fixture definition.
    private final RevoluteJointDef revJointDef = new RevoluteJointDef();          // keep needle and wheel in the place.
    private final DistanceJointDef disJointDef = new DistanceJointDef();          // join needle with external bodies to constrain and keep it in place.

    private Body wheelCore;     // circle shape which join with all pegs.
    private Body wheelBase;     // any shape to join with core of wheel.
    private Body needle;        // polygon shape.
    private Body B0, B1, B2;    // three bodies to constrain and keep the needle in the place.

    private final float diameter;         // diameter of wheel
    private final float x, y;             // position
    private final int nPegs;              // number of pegs
    private float farNeedle;              // distance of needle from the wheel
    private boolean spinned = false;      // to ensure a spinning is one time only.

    /**
     * All dimensions parameters's dividing on 100 to equivalent Newton's laws according Box2D physics.
     *
     * @param viewportWidth  the viewport width.
     * @param viewportHeight the viewport height.
     * @param diameter       of wheel.
     * @param x              position of wheel according camera viewport.
     * @param y              position of wheel according camera viewport.
     * @param nPegs          number of pegs attached with wheel.
     */
    public SpinWheel(float viewportWidth, float viewportHeight, float diameter, float x, float y, int nPegs) {
        this.diameter = diameter / PPM;
        this.x = x / PPM;
        this.y = y / PPM;
        this.nPegs = nPegs;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, viewportWidth / PPM, viewportHeight / PPM);

        // world with no gravity
        world = new World(new Vector2(0, 0), true);
        world.setContactListener(new SpinWheelWorldContact());

        // for debugging mode
        renderer = new Box2DDebugRenderer();

        createWheel();
        createNeedle();
    }

    /**
     * Just contains on all methods to create a wheel of spin.
     */
    private void createWheel() {
        // 1- Base of Wheel
        base_of_wheel();

        // 2- Core of Wheel
        core_of_wheel();

        // 3- Pegs of Wheel
        pegs_of_wheel();

        // 4- Revolute Joint (Base & Core) of Wheel
        joint_base_core_of_wheel();
    }

    /**
     * Just contains on all methods to create a needle of spin.
     */
    private void createNeedle() {
        // 1- Core Of Needle
        core_of_needle(30F * (diameter / STANDARD_SIZE), 80F * (diameter / STANDARD_SIZE));

        // 2- B0 which is (CENTER) base of CN-needle
        B0_of_needle();

        // 3- joint (B0) base of needle with CN-needle
        joint_B0_CN_Needle();

        // 4- create B1 which is (LEFT) base of constrain joint with UP-needle
        B1_of_needle();

        // 5- create B2 which is (RIGHT) base of constrain joint with UP-needle
        B2_of_needle();

        // 6- joint (B1 & B2) with UP-needle
        joint_B1_B2_with_UP_needle();
    }

    /**
     * The base of wheel is a static body with as a square shape.
     */
    private void base_of_wheel() {
        PolygonShape polygon = new PolygonShape();
        // Define The Base Of Wheel
        bodyDef.type = BodyType.StaticBody;

        // set The Base Position
        bodyDef.position.set(x, y);

        // Create The Base Body
        wheelBase = world.createBody(bodyDef);

        // set The Shape of Base
        polygon.setAsBox(32 / PPM, 32 / PPM);
        fixtureDef.shape = polygon;

        // Create The Base Fixture
        wheelBase.createFixture(fixtureDef);

        // Dispose The Shape
        polygon.dispose();
    }

    /**
     * The core of wheel is a dynamic body as a circle shape
     */
    private void core_of_wheel() {
        CircleShape circle = new CircleShape();
        // Define The Base Of Wheel
        bodyDef.type = BodyType.DynamicBody;

        // To Stop after spinning
        bodyDef.angularDamping = 0.25f;
        bodyDef.position.set(x, y);

        // Create The Core Body
        wheelCore = world.createBody(bodyDef);

        // set The Shape of Base
        circle.setRadius(diameter / 2);
        fixtureDef.shape = circle;

        // set The physics properties of The Shape
        fixtureDef.density = 0.25f;
        fixtureDef.friction = 0.25f;

        // Create The Base Fixture
        wheelCore.createFixture(fixtureDef);

        // Dispose The Shape
        circle.dispose();
    }

    /**
     * The pegs of wheel allowing the needle to collide only with the pegs and not the wheel.
     **/
    private void pegs_of_wheel() {
        if (nPegs == 0)
            return;

        CircleShape circle = new CircleShape();
        // Define The Pegs Of Wheel
        bodyDef.type = BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        // set The physics properties of The Shape
        fixtureDef.density = 0.0f;
        fixtureDef.friction = 0.0f;

        // set categoryBits To allow collide with (needle)
        fixtureDef.filter.categoryBits = BIT_PEG;
        fixtureDef.filter.maskBits = BIT_NEEDLE;

        for (int i = 0; i < nPegs; i++) {
            double theta = Math.toRadians((360 / nPegs) * i);
            float x = (float) Math.cos(theta);
            float y = (float) Math.sin(theta);

            // set The Peg Position
            circle.setPosition(circle.getPosition().set(x * diameter / 2, y * diameter / 2).scl(0.90f));

            // set The Shape of Pegs
            circle.setRadius(12 * (diameter / STANDARD_SIZE) / 2);
            fixtureDef.shape = circle;

            // Create The Base Fixture
            Fixture fixture = wheelCore.createFixture(fixtureDef);

            // set user data as a number of peg to indecator to lucky win element.
            fixture.setUserData((i + 1));
        }

        // the shape is no longer used.
        circle.dispose();
    }

    /**
     * Left static body to constrain and keep needle in the center.
     */
    private void B1_of_needle() {
        CircleShape circle = new CircleShape();
        // set The Shape of B1
        circle.setRadius(4 * (diameter / STANDARD_SIZE));
        fixtureDef.shape = circle;

        // Define The B1 Of Wheel
        bodyDef.type = BodyType.StaticBody;

        // needs to be true; the reason for this is that the pegs can move very quickly
        // when the wheel is spinning fast and sometimes the peg-needle collision will
        // be missed by box2d. If the isBullet flag is set, box2d resolves those cases
        // correctly.
        bodyDef.bullet = true;

        bodyDef.position.set(x - 40F * (diameter / STANDARD_SIZE), y + farNeedle);

        // Create The B1 Body
        B1 = world.createBody(bodyDef);

        // set The physics properties of The Shape
        fixtureDef.density = 1f;
        fixtureDef.restitution = 1f;
        fixtureDef.friction = 1f;

        // set categoryBits To allow collide with (needle)
        fixtureDef.filter.categoryBits = BIT_B1;
        fixtureDef.filter.maskBits = BIT_NEEDLE;

        // Create The B1 Fixture
        B1.createFixture(fixtureDef);

        // Dispose The Shape
        circle.dispose();
    }

    /**
     * Right static body to constrain and keep needle in the center.
     */
    private void B2_of_needle() {
        CircleShape circle = new CircleShape();
        // set The Shape of B2
        circle.setRadius(4 * (diameter / STANDARD_SIZE));
        fixtureDef.shape = circle;

        // Define The B2 Of Wheel
        bodyDef.type = BodyType.StaticBody;
        bodyDef.bullet = true;

        bodyDef.position.set(x + 40F * (diameter / STANDARD_SIZE), y + farNeedle);

        // Create The B2 Body
        B2 = world.createBody(bodyDef);

        // set The physics properties of The Shape
        fixtureDef.density = 1f;
        fixtureDef.restitution = 1f;
        fixtureDef.friction = 1f;

        // set categoryBits To allow collide with (needle)
        fixtureDef.filter.categoryBits = BIT_B2;
        fixtureDef.filter.maskBits = BIT_NEEDLE;

        // Create The B2 Fixture
        B2.createFixture(fixtureDef);

        // Dispose The Shape
        circle.dispose();

        // add B2 body to pegsSelectors
        // bodies.add(B2);
    }

    /**
     * Center static body to join needle with it by joint.
     */
    private void B0_of_needle() {
        CircleShape circle = new CircleShape();
        // set The Shape of Base
        circle.setRadius(4 * (diameter / STANDARD_SIZE));
        fixtureDef.shape = circle;

        // Define The Base Of Wheel
        bodyDef.type = BodyType.StaticBody;

        bodyDef.position.set(x, y + farNeedle + 5F * (diameter / STANDARD_SIZE));

        // Create The B Body
        B0 = world.createBody(bodyDef);

        // set The physics properties of The Shape
        fixtureDef.density = 0.0f;

        // Create The B Fixture
        B0.createFixture(fixtureDef);

        // shape is no longer used.
        circle.dispose();
    }

    /**
     * The needle is a body with a single fixture in the shape of a polygon as a kite shape.
     */
    private void core_of_needle(float needleWidth, float needleHeight) {
        PolygonShape polygon = new PolygonShape();
        // set The Shape of Needle
        float[] vertices = {-needleWidth / 2, 0f, 0f, needleHeight / 4, needleWidth / 2, 0f, 0f, -3 * needleHeight / 4};
        polygon.set(vertices);
        fixtureDef.shape = polygon;

        // Define The Needle
        bodyDef.type = BodyType.DynamicBody;

        farNeedle = diameter / 1.95f;

        bodyDef.position.set(x, y + farNeedle);

        // set The physics properties of The Shape
        bodyDef.bullet = true;
        bodyDef.angularDamping = 0.25f;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.0f;

        // Create The needle Body
        needle = world.createBody(bodyDef);

        // set categoryBits To allow collide with (Peg & B1 & B2)
        fixtureDef.filter.categoryBits = BIT_NEEDLE;
        fixtureDef.filter.maskBits = BIT_PEG | BIT_B1 | BIT_B2;

        // Create The B Fixture
        needle.createFixture(fixtureDef);

        // Dispose The Shape
        polygon.dispose();
    }

    /**
     * attach wheel to its base via a revolute joint. This joint allows the wheel to spin freely about the center.
     */
    private void joint_base_core_of_wheel() {
        revJointDef.bodyA = wheelBase;
        revJointDef.bodyB = wheelCore;
        revJointDef.collideConnected = false;
        world.createJoint(revJointDef);
    }

    /**
     * keep needle in the center position with two distances joint connected by two bodies B1 and B2.
     */
    private void joint_B1_B2_with_UP_needle() {
        disJointDef.bodyA = B1;
        disJointDef.bodyB = needle;
        disJointDef.localAnchorB.set(0, 15 / PPM);
        disJointDef.length = (float) Math.sqrt(Math.pow(40F * (diameter / STANDARD_SIZE), 2) + Math.pow(15 / PPM + 5F * (diameter / STANDARD_SIZE), 2));
        disJointDef.collideConnected = true;
        world.createJoint(disJointDef);

        disJointDef.bodyA = B2;
        disJointDef.bodyB = needle;
        disJointDef.collideConnected = true;
        world.createJoint(disJointDef);
    }

    // The needle base rotate about the revolute joint.
    private void joint_B0_CN_Needle() {
        revJointDef.bodyA = B0;
        revJointDef.bodyB = needle;
        world.createJoint(revJointDef);
    }

    /**
     * @param debug mode.
     */
    public void render(boolean debug) {
        world.step(1 / 60f, 8, 2);

        if (debug)
            renderer.render(world, camera.combined);
    }

    /**
     * This method used one time only.
     *
     * @param omega spin impulse the angular in units of kg*m*m/s. The Maximum value is 30 to avoid needle to slip from joints.
     */
    public void spin(float omega) {
//        if (spinned)
//            return;
        wheelCore.setAngularVelocity(MathUtils.clamp(omega, 0, 30));
        spinned = true;
    }

    /**
     * @return true after calling {@link #spin(float)} method.
     */
    public boolean spinningStopped() {
        return !wheelCore.isAwake();
    }

    public void setWorldContactListener(ContactListener listener) {
        world.setContactListener(listener);
    }

    /**
     * This method is useful to connect this boby with UI data and some properties.
     *
     * @return body of wheel.
     */
    public Body getWheelBody() {
        return wheelCore;
    }

    /**
     * This method is useful to connect this boby with UI data and some properties.
     *
     * @return body of needle.
     */
    public Body getNeedleBody() {
        return needle;
    }

    /**
     * @return center needle rotation value of X (NOT center X of the needle shape.) according given width.
     */
    public float getNeedleCenterX(float needleWidth) {
        return needleWidth / 2;
    }

    /**
     * @return center needle rotation value of Y (NOT center Y of the needle shape.) according given height.
     */
    public float getNeedleCenterY(float needleHeight) {
        return 3 * needleHeight / 4;
    }

    // contains two pegs (as a number which saved in UserData) with the object between them.
    private final IntArray pegsSelectors = new IntArray(2);
    // to connect between data (two pegs numbers) and object which this lucky element.
    private ObjectMap<IntArray, Object> elements = new ObjectMap<IntArray, Object>();

    /**
     * @param elements contains all data with known their objects which have two pegs numbers for each object.
     */
    public void setElements(ObjectMap<IntArray, Object> elements) {
        this.elements = elements;
    }

    /**
     * @param object is between data (two numbers of pegs as known)
     * @param data   two numbers of pegs.
     */
    public void addElementData(Object object, IntArray data) {
        elements.put(data, object);
    }

    /**
     * @return object between that two pegs.
     */
    public Object getLuckyWinElement() {
        if (pegsSelectors.size > 0)
            for (IntArray array : elements.keys())
                if (array.contains(pegsSelectors.get(0)) && array.contains(pegsSelectors.get(1)))
                    return elements.get(array);
        return null;
    }

    @Override
    public void dispose() {
        world.dispose();
        renderer.dispose();
    }

    private final class SpinWheelWorldContact implements ContactListener {
        @Override
        public void beginContact(Contact contact) {
        }

        @Override
        public void endContact(Contact contact) {
            Object data = contact.getFixtureB().getUserData();
            if (data == null) return;

            int nPeg = (Integer) data, peg2;

            if (wheelCore.getAngularVelocity() <= 0) {    // add [before] peg
                int before = nPeg + 1;
                if (before == nPegs)
                    before = 0;
                peg2 = before;
            } else {                                      // add [after] peg
                int after = nPeg - 1;
                if (after == 0)
                    after = nPegs;
                peg2 = after;
            }

            pegsSelectors.clear();
            pegsSelectors.addAll(nPeg, peg2);
        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {
        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {
        }
    }
}
