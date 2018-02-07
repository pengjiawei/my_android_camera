# my_android_camera
publish topic camera/image to ros from android camera

the most of my code is from [github rosjava/android_core](https://github.com/rosjava/android_core.git)
but if you simply follow the android_tutorial_camera from that,you will find that you can't add your own publisher in RosCameraPreview.java
it only contains CompressedImagePublisher

you can still subscribe the topic published by this app and display it in rviz,but you only get the compressedImage and cameraInfo

so I add the android_10 as a module into my app that I can change code in it .

I wanna add a publisher that can publish the image/raw,but it's not finished yet
