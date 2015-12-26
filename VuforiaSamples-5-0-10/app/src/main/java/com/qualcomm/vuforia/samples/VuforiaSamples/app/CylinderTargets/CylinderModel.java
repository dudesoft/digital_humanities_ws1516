/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.qualcomm.vuforia.samples.VuforiaSamples.app.CylinderTargets;

import com.qualcomm.vuforia.samples.SampleApplication.utils.MeshObject;

import java.nio.Buffer;

public class CylinderModel extends MeshObject {

   // 4 triangles per side, so 12 indices per side
   short cylinderIndices[];
   double cylinderNormals[];
   double cylinderTexCoords[];
   double cylinderVertices[];
   int indicesNumber;
   int verticesNumber;
   private int CYLINDER_NB_SIDES = 64;
   private int CYLINDER_NUM_VERTEX = ((CYLINDER_NB_SIDES * 2) + 2);
   private Buffer mIndBuff;
   private Buffer mNormBuff;
   private Buffer mTexCoordBuff;
   private float mTopRadius;
   private Buffer mVertBuff;
   public CylinderModel(float topRadius) {
      cylinderVertices = new double[CYLINDER_NUM_VERTEX * 3];
      cylinderIndices = new short[CYLINDER_NB_SIDES * 12];
      cylinderTexCoords = new double[CYLINDER_NUM_VERTEX * 2];
      cylinderNormals = new double[CYLINDER_NUM_VERTEX * 3];
      mTopRadius = topRadius;
      prepareData();

      mVertBuff = fillBuffer(cylinderVertices);
      mTexCoordBuff = fillBuffer(cylinderTexCoords);
      mNormBuff = fillBuffer(cylinderNormals);
      mIndBuff = fillBuffer(cylinderIndices);
   }

   @Override
   public Buffer getBuffer(BUFFER_TYPE bufferType) {
      Buffer result = null;
      switch (bufferType) {
         case BUFFER_TYPE_VERTEX:
            result = mVertBuff;
            break;
         case BUFFER_TYPE_TEXTURE_COORD:
            result = mTexCoordBuff;
            break;
         case BUFFER_TYPE_NORMALS:
            result = mNormBuff;
            break;
         case BUFFER_TYPE_INDICES:
            result = mIndBuff;
         default:
            break;
      }

      return result;
   }

   @Override
   public int getNumObjectIndex() {
      return indicesNumber;
   }

   @Override
   public int getNumObjectVertex() {
      return verticesNumber;
   }

   void prepareData() {
      double deltaTex = (1.0 / (double) CYLINDER_NB_SIDES);

      // vertices index for the bottom and top vertex
      int ix_vertex_center_bottom = 2 * CYLINDER_NB_SIDES;
      int ix_vertex_center_top = ix_vertex_center_bottom + 1;

      for (int i = 0; i < CYLINDER_NB_SIDES; i++) {
         double angle = 2 * Math.PI * i / CYLINDER_NB_SIDES;

         // bottom circle
         cylinderVertices[(i * 3) + 0] = Math.cos(angle); // x
         cylinderVertices[(i * 3) + 1] = Math.sin(angle); // y
         cylinderVertices[(i * 3) + 2] = 0; // z

         // top circle
         cylinderVertices[(i + CYLINDER_NB_SIDES) * 3 + 0] =
               mTopRadius * cylinderVertices[i * 3 + 0]; // x
         cylinderVertices[(i + CYLINDER_NB_SIDES) * 3 + 1] =
               mTopRadius * cylinderVertices[i * 3 + 1]; // y
         cylinderVertices[(i + CYLINDER_NB_SIDES) * 3 + 2] = 1; // z

         // texture coordinates
         cylinderTexCoords[(i * 2) + 0] = i * deltaTex;
         cylinderTexCoords[(i * 2) + 1] = 1;

         cylinderTexCoords[((i + CYLINDER_NB_SIDES) * 2) + 0] = i * deltaTex;
         cylinderTexCoords[((i + CYLINDER_NB_SIDES) * 2) + 1] = 0;

         // normals

         cylinderNormals[(i * 3) + 0] = cylinderVertices[(i * 3) + 1];
         cylinderNormals[(i * 3) + 1] = -(cylinderVertices[(i * 3) + 0]);
         cylinderNormals[(i * 3) + 2] = 0; // z

         // top circle normals
         cylinderNormals[(i + CYLINDER_NB_SIDES) * 3 + 0] =
               mTopRadius * cylinderVertices[i * 3 + 1];
         cylinderNormals[(i + CYLINDER_NB_SIDES) * 3 + 1] =
               -(mTopRadius * cylinderVertices[i * 3 + 0]);
         cylinderNormals[(i + CYLINDER_NB_SIDES) * 3 + 2] = 0; // z

         // indices
         // triangles are b0 b1 t1 and t1 t0 b0 (bn: vertex #n on the bottom
         // circle, tn: vertex #n on the to circle)
         // i1 is the index of the next vertex - we wrap if we reach the end
         // of the circle
         int i1 = i + 1;
         if (i1 == CYLINDER_NB_SIDES) {
            i1 = 0;
         }
         cylinderIndices[(i * 12) + 0] = (short) i;
         cylinderIndices[(i * 12) + 1] = (short) i1;
         cylinderIndices[(i * 12) + 2] = (short) (i1 + CYLINDER_NB_SIDES);

         cylinderIndices[(i * 12) + 3] = (short) (i1 + CYLINDER_NB_SIDES);
         cylinderIndices[(i * 12) + 4] = (short) (i + CYLINDER_NB_SIDES);
         cylinderIndices[(i * 12) + 5] = (short) i;

         // bottom circle
         cylinderIndices[(i * 12) + 6] = (short) i1;
         cylinderIndices[(i * 12) + 7] = (short) i;
         cylinderIndices[(i * 12) + 8] = (short) ix_vertex_center_bottom;

         // top circle
         cylinderIndices[(i * 12) + 9] = (short) (i + CYLINDER_NB_SIDES);
         cylinderIndices[(i * 12) + 10] = (short) (i1 + CYLINDER_NB_SIDES);
         cylinderIndices[(i * 12) + 11] = (short) ix_vertex_center_top;
      }

      // we are adding 2 extra vertices: the center of each circle
      cylinderVertices[(3 * ix_vertex_center_bottom) + 0] = 0; // x
      cylinderVertices[(3 * ix_vertex_center_bottom) + 1] = 0; // y
      cylinderVertices[(3 * ix_vertex_center_bottom) + 2] = 0; // z

      cylinderVertices[(3 * ix_vertex_center_top) + 0] = 0; // x
      cylinderVertices[(3 * ix_vertex_center_top) + 1] = 0; // y
      cylinderVertices[(3 * ix_vertex_center_top) + 2] = 1; // z

      cylinderTexCoords[(ix_vertex_center_bottom * 2) + 0] = 0.5f;
      cylinderTexCoords[(ix_vertex_center_bottom * 2) + 1] = 0.5f;

      cylinderTexCoords[(ix_vertex_center_top * 2) + 0] = 0.5f;
      cylinderTexCoords[(ix_vertex_center_top * 2) + 1] = 0.5f;

      cylinderNormals[(3 * ix_vertex_center_bottom) + 0] = 0;
      cylinderNormals[(3 * ix_vertex_center_bottom) + 1] = 0;
      cylinderNormals[(3 * ix_vertex_center_bottom) + 2] = -1; // z

      cylinderNormals[(3 * ix_vertex_center_top) + 0] = 0;
      cylinderNormals[(3 * ix_vertex_center_top) + 1] = 0;
      cylinderNormals[(3 * ix_vertex_center_top) + 2] = 1; // z

      verticesNumber = cylinderVertices.length / 3;
      indicesNumber = cylinderIndices.length;
   }
}
