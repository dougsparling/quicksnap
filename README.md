# QuickSnap

A test-bed application for Android intended to answer three questions:

* Can the camera operate from a foreground service?
  * Yes! In fact there's a special permission for it, `FOREGROUND_SERVICE_CAMERA`, although it doesn't seem to be presented any differently to the user.
* Can an API be built to drive the CameraX API uniformly across photo and video use cases, using coroutines, without devolving into callback-hell?
   * Yes! Though the use cases are simple, see `CameraController.CaptureSpec` and `CoroutineScope.launchWithLifecycle` for the interesting bits. There's perhaps a lot of unnecessary setup and teardown between shots, but it does work cleanly on my test device (Pixel 7a).
* Can Android Studio's new Gemini preview help build an app?
  * Sorta? It didn't understand my intention to use coroutines for everything and also often shoved Camera2 junk in all over the place, but it did write a decent amount of usable code.

## Limitations, Caveats and Thoughts

### How to see output if there's no UI?

Maybe one day I'll incorporate the media source API, but for now, the output can be pulled off the device with ADB:

`adb pull /storage/emulated/0/Android/data/dev.cyberdeck.qs/files/Pictures .`

### Why does the app crash on launch?

I was lazy, so it expects you to grant permissions before launching. Just open the app settings, give it the camera and notification permissions, and re-launch.

If it still crashes, well, I built it around the feature set of my one test device. Despite the lovely wrapper CameraX provides around the uneven capabilities of various devices, it's still possible I've written code that will crash on less capable phones.

### Min SDK 35?!

When you're only deploying on one device you don't have to worry about backward compatibility. Android 8 compatibility woes are for my day job, sorry (not sorry).

### What did you learn?

CameraX is well thought out and easy to use, **if** what you want to do is supported, otherwise you may have to use the experimental Camera2 inter-op, or bite the bullet and use Camera2 directly. One example is the slo-mo API, it's simply not possible to record beyond 60 FPS with CameraX even if the hardware supports it.

Another frustrating thing is that cameras have become so complex a lot of useful functionality is tucked away into opaque extensions, which aren't even included with CameraX out of the box. (e.g. HDR support) I feel like this puts app authors at a disadvantage compared to vendors bundling their own camera apps that can tap into these opaque features. In an ideal world the standard camera API would cover all capabilities, but Android being what it is... that'll never happen. 

Also, some of the abstractions are... leaky? Sometimes getting a capability involves setting a flag, and it varies if you set it on the camera or on the use case, other times you have to filter cameras to pick the one that can do what you want. It can be overwhelming to juggle all these concerns, which is why I tried to pack everything into `CameraController#capture`.