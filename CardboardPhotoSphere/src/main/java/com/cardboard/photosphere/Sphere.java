/*
 * Copyright 2014 Google Inc. All Rights Reserved.

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
 */

package com.cardboard.photosphere;

import android.content.Context;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sree Kumar
 *         <p/>
 *         Create a Sphere from set of TRIANGLE_STRIPS and apply the 2D texture inside that.
 */
public class Sphere {


    /**
     * Maximum allowed depth.
     */
    private static final int MAXIMUM_ALLOWED_DEPTH = 5;

    /**
     * Used in vertex strip calculations, related to properties of a icosahedron.
     */
    private static final int VERTEX_MAGIC_NUMBER = 5;

    /**
     * Each vertex is a 2D coordinate.
     */
    private static final int NUM_FLOATS_PER_VERTEX = 3;

    /**
     * Each texture is a 2D coordinate.
     */
    private static final int NUM_FLOATS_PER_TEXTURE = 2;

    /**
     * Each vertex is made up of 3 points, x, y, z.
     */
    private static final int AMOUNT_OF_NUMBERS_PER_VERTEX_POINT = 3;

    /**
     * Each texture point is made up of 2 points, x, y (in reference to the texture being a 2D image).
     */
    private static final int AMOUNT_OF_NUMBERS_PER_TEXTURE_POINT = 2;

    /**
     * Buffer holding the vertices.
     */
    private final List<FloatBuffer> mVertexBuffer = new ArrayList<FloatBuffer>();

    /**
     * The vertices for the sphere.
     */
    private final List<float[]> mVertices = new ArrayList<float[]>();

    /**
     * Buffer holding the texture coordinates.
     */
    private final List<FloatBuffer> mTextureBuffer = new ArrayList<FloatBuffer>();

    /**
     * Mapping texture coordinates for the vertices.
     */
    private final List<float[]> mTexture = new ArrayList<float[]>();


    /**
     * Total number of strips for the given depth.
     */
    private final int mTotalNumStrips;


    // number of coordinates per vertex in this array
    static final int CORDS_PER_VERTEX = 3;


    // Use to access and set the view transformation
    private int mMVPMatrixHandle;


    private int mPositionHandle;
    private int mProgramHandle;
    private int mTextureCoordinateHandle;
    private int mTextureDataHandle0[] = new int[1];
    private final int vertexStride = CORDS_PER_VERTEX * 4; // 4 bytes per vertex


    public Sphere(final Context context, final int depth, final float radius) {

        // Loading the shader from assets
        final String vertexShader = getVertexShader(context);
        final String fragmentShader = getFragmentShader(context);

        // Compiling the shader
        final int vertexShaderHandle = ShaderHelper.compileShader(
                GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(
                GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        // Setting the attributes for the shader, this Step can be ignored
        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle,
                fragmentShaderHandle, new String[]{"a_Position",
                        "a_TexCoordinate"});

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mProgramHandle);


        // Clamp depth to the range 1 to MAXIMUM_ALLOWED_DEPTH;
        final int d = Math.max(1, Math.min(MAXIMUM_ALLOWED_DEPTH, depth));

        // Calculate basic values for the sphere.
        this.mTotalNumStrips = Maths.power(2, d - 1) * VERTEX_MAGIC_NUMBER;
        final int numVerticesPerStrip = Maths.power(2, d) * 3;
        final double altitudeStepAngle = Maths.ONE_TWENTY_DEGREES / Maths.power(2, d);
        final double azimuthStepAngle = Maths.THREE_SIXTY_DEGREES / this.mTotalNumStrips;
        double x, y, z, h, altitude, azimuth;

        for (int stripNum = 0; stripNum < this.mTotalNumStrips; stripNum++) {
            // Setup arrays to hold the points for this strip.
            final float[] vertices = new float[numVerticesPerStrip * NUM_FLOATS_PER_VERTEX]; // NOPMD
            final float[] texturePoints = new float[numVerticesPerStrip * NUM_FLOATS_PER_TEXTURE]; // NOPMD
            int vertexPos = 0;
            int texturePos = 0;

            // Calculate position of the first vertex in this strip.
            altitude = Maths.NINETY_DEGREES;
            azimuth = stripNum * azimuthStepAngle;

            // Draw the rest of this strip.
            for (int vertexNum = 0; vertexNum < numVerticesPerStrip; vertexNum += 2) {
                // First point - Vertex.
                y = radius * Math.sin(altitude);
                h = radius * Math.cos(altitude);
                z = h * Math.sin(azimuth);
                x = h * Math.cos(azimuth);
                vertices[vertexPos++] = (float) x;
                vertices[vertexPos++] = (float) y;
                vertices[vertexPos++] = (float) z;

                // First point - Texture.
                texturePoints[texturePos++] = (float) (1 - azimuth / Maths.THREE_SIXTY_DEGREES);
                texturePoints[texturePos++] = (float) (1 - (altitude + Maths.NINETY_DEGREES) / Maths.ONE_EIGHTY_DEGREES);

                // Second point - Vertex.
                altitude -= altitudeStepAngle;
                azimuth -= azimuthStepAngle / 2.0;
                y = radius * Math.sin(altitude);
                h = radius * Math.cos(altitude);
                z = h * Math.sin(azimuth);
                x = h * Math.cos(azimuth);
                vertices[vertexPos++] = (float) x;
                vertices[vertexPos++] = (float) y;
                vertices[vertexPos++] = (float) z;

                // Second point - Texture.
                texturePoints[texturePos++] = (float) (1 - azimuth / Maths.THREE_SIXTY_DEGREES);
                texturePoints[texturePos++] = (float) (1 - (altitude + Maths.NINETY_DEGREES) / Maths.ONE_EIGHTY_DEGREES);

                azimuth += azimuthStepAngle;
            }

            this.mVertices.add(vertices);
            this.mTexture.add(texturePoints);

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(numVerticesPerStrip * NUM_FLOATS_PER_VERTEX * Float.SIZE);
            byteBuffer.order(ByteOrder.nativeOrder());
            FloatBuffer fb = byteBuffer.asFloatBuffer();
            fb.put(this.mVertices.get(stripNum));
            fb.position(0);
            this.mVertexBuffer.add(fb);

            // Setup texture.
            byteBuffer = ByteBuffer.allocateDirect(numVerticesPerStrip * NUM_FLOATS_PER_TEXTURE * Float.SIZE);
            byteBuffer.order(ByteOrder.nativeOrder());
            fb = byteBuffer.asFloatBuffer();
            fb.put(this.mTexture.get(stripNum));
            fb.position(0);
            this.mTextureBuffer.add(fb);
        }
    }


    public void loadTexture(Context context, int resourceId) {
        // Load the static 2D texture
        mTextureDataHandle0 = TextureHelper.loadTexture(context,
                resourceId);

    }

    public void deleteCurrentTexture() {

        GLES20.glDeleteTextures(mTextureDataHandle0.length, mTextureDataHandle0, 0);
    }


    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL ES environment

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle,
                "u_MVPMatrix");

        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle,
                "a_Position");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle,
                "a_TexCoordinate");

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_FRONT);
        GLES20.glFrontFace(GLES20.GL_CW);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle0[0]);
        GLES20.glUniform1i(mTextureCoordinateHandle, 0);


        for (int i = 0; i < this.mTotalNumStrips; i++) {


            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(mPositionHandle, CORDS_PER_VERTEX,
                    GLES20.GL_FLOAT, false,
                    vertexStride, mVertexBuffer.get(i));

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle);


            GLES20.glVertexAttribPointer(mTextureCoordinateHandle,
                    AMOUNT_OF_NUMBERS_PER_TEXTURE_POINT, GLES20.GL_FLOAT, false, 0,
                    mTextureBuffer.get(i));

            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);


            // Pass the projection and view transformation to the shader
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);


            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, this.mVertices.get(i).length / AMOUNT_OF_NUMBERS_PER_VERTEX_POINT);


        }
        // Disable vertex array

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisable(GLES20.GL_CULL_FACE);


    }


    protected String getVertexShader(Context context) {
        return RawResourceReader.readTextFileFromRawResource(context,
                R.raw._vertex_shader);
    }

    protected String getFragmentShader(Context context) {
        return RawResourceReader.readTextFileFromRawResource(context,
                R.raw._fragment_shader);
    }

}