/*
 * *****************************************************************************
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
package com.crowni.gdx.spinwheel.Test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntArray;
import com.crowni.gdx.spinwheel.spin.SpinWheel;
import com.crowni.gdx.spinwheel.utils.BaseScreen;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

/**
 * Created by Crowni on 9/14/2017.
 **/
public class TestScreen extends BaseScreen {
    private static final String TAG = TestScreen.class.getSimpleName();

    // These attributes according with your UI designing.
    private static final float WHEEL_DIAMETER = 750F;
    private static final int NUMBER_OF_PEGS = 12;

    // only Box2D structure (without UI designing) as a Model.
    private SpinWheel spinWheel;

    // images for SpinWheel UI.
    private Image wheelImage;
    private Image needleImage;

    @Override
    public void show() {
        super.show();

        final float width = stage.getWidth();
        final float height = stage.getHeight();

        // initialize SpinWheel.
        spinWheel = new SpinWheel(width, height, WHEEL_DIAMETER, width / 2, height / 2, NUMBER_OF_PEGS);

        /* *************** SpinWheel UI ************** */
        // load atlas file with all SpinWheel UI elements.
        final TextureAtlas atlas = new TextureAtlas("spin_wheel_ui.atlas");

        // create wheel image
        spinWheel.getWheelBody().setUserData(wheelImage = new Image(atlas.findRegion("spin_wheel")));
        updateCoordinates(spinWheel.getWheelBody(), wheelImage, 0, 0);
        wheelImage.setOrigin(Align.center);
        stage.addActor(wheelImage);

        // create spin image button.
        final Image btnSpin = new Image(atlas.findRegion("spin_button"));
        btnSpin.setOrigin(Align.center);
        btnSpin.setPosition(width / 2, height / 2, Align.center);
        stage.addActor(btnSpin);

        // add listener to spin button.
        btnSpin.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                btnSpin.addAction(sequence(scaleTo(1.25F, 1.25F, 0.10F), scaleTo(1F, 1F, 0.10F)));
                spinWheel.spin(MathUtils.random(5F, 30F));

                Gdx.app.debug(TAG, "Spinning.");
            }
        });

        // create needle image
        spinWheel.getNeedleBody().setUserData(needleImage = new Image(new Sprite(atlas.findRegion("needle"))));
        updateCoordinates(spinWheel.getNeedleBody(), needleImage, 0, -25F);
        needleImage.setOrigin(spinWheel.getNeedleCenterX(needleImage.getWidth()), spinWheel.getNeedleCenterY(needleImage.getHeight()));
        stage.addActor(needleImage);

        setElementData();
    }

    private void updateCoordinates(Body body, Image image, float incX, float incY) {
        image.setPosition((body.getPosition().x * SpinWheel.PPM) + incX, (body.getPosition().y * SpinWheel.PPM) + incY, Align.center);
        image.setRotation(body.getAngle() * MathUtils.radiansToDegrees);
    }


    private void setElementData() {
        spinWheel.addElementData(Color.valueOf("e966ac"), getData(1, 2));
        spinWheel.addElementData(Color.valueOf("b868ad"), getData(2, 3));
        spinWheel.addElementData(Color.valueOf("8869ad"), getData(3, 4));
        spinWheel.addElementData(Color.valueOf("3276b5"), getData(4, 5));
        spinWheel.addElementData(Color.valueOf("33a7d8"), getData(5, 6));
        spinWheel.addElementData(Color.valueOf("33b8a5"), getData(6, 7));
        spinWheel.addElementData(Color.valueOf("a3fd39"), getData(7, 8));
        spinWheel.addElementData(Color.valueOf("fff533"), getData(8, 9));
        spinWheel.addElementData(Color.valueOf("fece3e"), getData(9, 10));
        spinWheel.addElementData(Color.valueOf("f9a54b"), getData(10, 11));
        spinWheel.addElementData(Color.valueOf("f04950"), getData(12, 1));
    }

    private IntArray getData(int peg_1, int peg_2) {
        IntArray array = new IntArray(2);
        array.addAll(peg_1, peg_2);
        return array;
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        spinWheel.render(false);

        if (!spinWheel.spinningStopped()) {
            // update wheel
            updateCoordinates(spinWheel.getWheelBody(), wheelImage, 0, 0);

            // update needle
            updateCoordinates(spinWheel.getNeedleBody(), needleImage, 0, -25F);
        } else {
            System.out.println("lucky element is: " + spinWheel.getLuckyWinElement());

            Color color = (Color) spinWheel.getLuckyWinElement();
            Gdx.gl.glClearColor(color.r, color.g, color.b, color.a);
        }

        Color color = (Color) spinWheel.getLuckyWinElement();
        if (color != null)
            Gdx.gl.glClearColor(color.r, color.g, color.b, color.a);

        if (Gdx.input.isKeyPressed(Input.Keys.ENTER))
            spinWheel.spin(0.2F);
    }

    @Override
    public void dispose() {
        super.dispose();
        spinWheel.dispose();
    }
}
