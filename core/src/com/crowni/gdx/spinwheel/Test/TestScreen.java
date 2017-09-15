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
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.crowni.gdx.spinwheel.spin.SpinWheel;
import com.crowni.gdx.spinwheel.utils.BaseScreen;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence;

/**
 * Created by Crowni on 9/14/2017.
 **/
public class TestScreen extends BaseScreen {
    private static final String TAG = TestScreen.class.getSimpleName();
    private static final float WHEEL_DIAMETER = 750F;
    private static final int NUMBER_OF_PEGS = 8;

    private SpinWheel spinWheel;

    @Override
    public void show() {
        super.show();

        final float width = stage.getWidth();
        final float height = stage.getHeight();

        final Image btnSpin = new Image();
        btnSpin.setSize(350F, 150F);
        btnSpin.setOrigin(Align.center);
        btnSpin.setPosition(width / 2, 350F, Align.center);
        btnSpin.setDebug(true);
        stage.addActor(btnSpin);

        btnSpin.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                btnSpin.addAction(sequence(scaleTo(1.25F, 1.25F, 0.10F), scaleTo(1F, 1F, 0.10F)));
                spinWheel.spin(MathUtils.random(15, 45));

                Gdx.app.debug(TAG, "Spin.");
            }
        });

        spinWheel = new SpinWheel(width, height, WHEEL_DIAMETER, width / 2, height / 2 + 140F, NUMBER_OF_PEGS);
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        spinWheel.render();
    }

    @Override
    public void pause() {
        super.pause();
        spinWheel.pause();
    }

    @Override
    public void resume() {
        super.resume();
        spinWheel.resume();
    }


    @Override
    public void dispose() {
        super.dispose();
        spinWheel.dispose();
    }
}
