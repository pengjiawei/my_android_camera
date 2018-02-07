/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android.view.camera;

import com.google.common.base.Preconditions;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.util.Log;

import org.jboss.netty.buffer.ByteBufferBackedChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.nio.ByteBuffer;

import sensor_msgs.Image;

/**
 * Publishes preview frames.
 * 
 * @author damonkohler@google.com (Damon Kohler)
 */
class CompressedImagePublisher implements RawImageListener {
  private final String TAG = "CompressedImagePublisher";
  private final ConnectedNode connectedNode;
  private final Publisher<sensor_msgs.CompressedImage> imagePublisher;
  private final Publisher<sensor_msgs.CameraInfo> cameraInfoPublisher;
  //add by pengjiawei
  private final Publisher<sensor_msgs.Image> rawImagePublisher;

  private byte[] rawImageBuffer;
  private Size rawImageSize;
  private YuvImage yuvImage;
  private Rect rect;
  private ChannelBufferOutputStream stream;

  public CompressedImagePublisher(ConnectedNode connectedNode) {
    this.connectedNode = connectedNode;
    NameResolver resolver = connectedNode.getResolver().newChild("camera");
    imagePublisher =
        connectedNode.newPublisher(resolver.resolve("image/compressed"),
            sensor_msgs.CompressedImage._TYPE);
    cameraInfoPublisher =
        connectedNode.newPublisher(resolver.resolve("camera_info"), sensor_msgs.CameraInfo._TYPE);
    //add by pengjiawei
    rawImagePublisher = connectedNode.newPublisher(resolver.resolve("image/raw"), Image._TYPE);

    stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
  }

  @SuppressLint("LongLogTag")
  @Override
  public void onNewRawImage(byte[] data, Size size) {
    Preconditions.checkNotNull(data);
    Preconditions.checkNotNull(size);
    if (data != rawImageBuffer || !size.equals(rawImageSize)) {
      rawImageBuffer = data;
      rawImageSize = size;
      yuvImage = new YuvImage(rawImageBuffer, ImageFormat.NV21, size.width, size.height, null);
      rect = new Rect(0, 0, size.width, size.height);
    }

    Time currentTime = connectedNode.getCurrentTime();
    String frameId = "camera";

    sensor_msgs.CompressedImage image = imagePublisher.newMessage();
    image.setFormat("jpeg");
    image.getHeader().setStamp(currentTime);
    image.getHeader().setFrameId(frameId);

    Preconditions.checkState(yuvImage.compressToJpeg(rect, 20, stream));
    image.setData(stream.buffer().copy());
//    stream.buffer().clear();

    imagePublisher.publish(image);

    sensor_msgs.CameraInfo cameraInfo = cameraInfoPublisher.newMessage();
    cameraInfo.getHeader().setStamp(currentTime);
    cameraInfo.getHeader().setFrameId(frameId);

    cameraInfo.setWidth(size.width);
    cameraInfo.setHeight(size.height);
//    cameraInfoPublisher.publish(cameraInfo);


    //add by pengjiawei
    Log.d(TAG, "onNewRawImage: start construct raw image");
    sensor_msgs.Image rawImage = rawImagePublisher.newMessage();
    rawImage.getHeader().setStamp(currentTime);
    rawImage.getHeader().setFrameId(frameId);
    rawImage.setEncoding("rgba8");
    rawImage.setWidth(size.width);
    rawImage.setHeight(size.height);
    rawImage.setStep(640);


//    Log.i(TAG,"Raw image 2");
//    ByteBuffer bb;
//    bmp.copyPixelsToBuffer(bb);
//
//
//    Log.i(TAG,"Raw image 3");
//
//    stream.buffer().writeBytes(bb.array());
//    bb.clear();


//    Log.i(TAG,"Raw image 4");

//    stream.buffer().writeBytes(rawImageBuffer);
    rawImage.setData(stream.buffer().copy());
    stream.buffer().clear();

//    Log.i(TAG,"Raw image 5");
    Log.d(TAG, "onNewRawImage: publish rawImage");
    cameraInfoPublisher.publish(cameraInfo);
    rawImagePublisher.publish(rawImage);

//    Log.i(TAG,"Raw image 6");
  }
}