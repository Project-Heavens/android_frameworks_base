/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm;

import static com.android.server.wm.WindowManagerDebugConfig.DEBUG_DRAG;
import static com.android.server.wm.WindowManagerDebugConfig.SHOW_LIGHT_TRANSACTIONS;
import static com.android.server.wm.WindowManagerDebugConfig.SHOW_TRANSACTIONS;
import static com.android.server.wm.WindowManagerDebugConfig.TAG_WM;

import android.content.ClipData;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import android.view.Display;
import android.view.IWindow;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import android.view.View;

/**
 * Managing drag and drop operations initiated by View#startDragAndDrop.
 */
class DragDropController {
    private static final float DRAG_SHADOW_ALPHA_TRANSPARENT = .7071f;
    private static final long DRAG_TIMEOUT_MS = 5000;

    // Messages for Handler.
    private static final int MSG_DRAG_START_TIMEOUT = 0;
    static final int MSG_DRAG_END_TIMEOUT = 1;
    static final int MSG_TEAR_DOWN_DRAG_AND_DROP_INPUT = 2;
    static final int MSG_ANIMATION_END = 3;

    DragState mDragState;

    private WindowManagerService mService;
    private final Handler mHandler;

    boolean dragDropActiveLocked() {
        return mDragState != null;
    }

    DragDropController(WindowManagerService service, Looper looper) {
        mService = service;
        mHandler = new DragHandler(service, looper);
    }

    IBinder prepareDrag(SurfaceSession session, int callerPid,
            int callerUid, IWindow window, int flags, int width, int height, Surface outSurface) {
        if (DEBUG_DRAG) {
            Slog.d(TAG_WM, "prepare drag surface: w=" + width + " h=" + height
                    + " flags=" + Integer.toHexString(flags) + " win=" + window
                    + " asbinder=" + window.asBinder());
        }

        IBinder token = null;

        synchronized (mService.mWindowMap) {
            if (dragDropActiveLocked()) {
                Slog.w(TAG_WM, "Drag already in progress");
                return null;
            }

            // TODO(multi-display): support other displays
            final DisplayContent displayContent =
                    mService.getDefaultDisplayContentLocked();
            final Display display = displayContent.getDisplay();

            final SurfaceControl surface = new SurfaceControl.Builder(session)
                    .setName("drag surface")
                    .setSize(width, height)
                    .setFormat(PixelFormat.TRANSLUCENT)
                    .build();
            surface.setLayerStack(display.getLayerStack());
            float alpha = 1;
            if ((flags & View.DRAG_FLAG_OPAQUE) == 0) {
                alpha = DRAG_SHADOW_ALPHA_TRANSPARENT;
            }
            surface.setAlpha(alpha);

            if (SHOW_TRANSACTIONS) Slog.i(TAG_WM, "  DRAG " + surface + ": CREATE");
            outSurface.copyFrom(surface);
            final IBinder winBinder = window.asBinder();
            token = new Binder();
            mDragState = new DragState(mService, token, surface, flags, winBinder);
            mDragState.mPid = callerPid;
            mDragState.mUid = callerUid;
            mDragState.mOriginalAlpha = alpha;
            token = mDragState.mToken = new Binder();

            // 5 second timeout for this window to actually begin the drag
            sendTimeoutMessage(MSG_DRAG_START_TIMEOUT, winBinder);
        }

        return token;
    }

    boolean performDrag(IWindow window, IBinder dragToken,
            int touchSource, float touchX, float touchY, float thumbCenterX, float thumbCenterY,
            ClipData data) {
        if (DEBUG_DRAG) {
            Slog.d(TAG_WM, "perform drag: win=" + window + " data=" + data);
        }

        synchronized (mService.mWindowMap) {
            if (mDragState == null) {
                Slog.w(TAG_WM, "No drag prepared");
                throw new IllegalStateException("performDrag() without prepareDrag()");
            }

            if (dragToken != mDragState.mToken) {
                Slog.w(TAG_WM, "Performing mismatched drag");
                throw new IllegalStateException("performDrag() does not match prepareDrag()");
            }

            final WindowState callingWin = mService.windowForClientLocked(null, window, false);
            if (callingWin == null) {
                Slog.w(TAG_WM, "Bad requesting window " + window);
                return false;  // !!! TODO: throw here?
            }

            // !!! TODO: if input is not still focused on the initiating window, fail
            // the drag initiation (e.g. an alarm window popped up just as the application
            // called performDrag()

            mHandler.removeMessages(MSG_DRAG_START_TIMEOUT, window.asBinder());

            // !!! TODO: extract the current touch (x, y) in screen coordinates.  That
            // will let us eliminate the (touchX,touchY) parameters from the API.

            // !!! FIXME: put all this heavy stuff onto the mHandler looper, as well as
            // the actual drag event dispatch stuff in the dragstate

            final DisplayContent displayContent = callingWin.getDisplayContent();
            if (displayContent == null) {
                return false;
            }
            Display display = displayContent.getDisplay();
            mDragState.register(display);
            if (!mService.mInputManager.transferTouchFocus(callingWin.mInputChannel,
                    mDragState.getInputChannel())) {
                Slog.e(TAG_WM, "Unable to transfer touch focus");
                mDragState.unregister();
                mDragState.reset();
                mDragState = null;
                return false;
            }

            mDragState.mDisplayContent = displayContent;
            mDragState.mData = data;
            mDragState.broadcastDragStartedLocked(touchX, touchY);
            mDragState.overridePointerIconLocked(touchSource);

            // remember the thumb offsets for later
            mDragState.mThumbOffsetX = thumbCenterX;
            mDragState.mThumbOffsetY = thumbCenterY;

            // Make the surface visible at the proper location
            final SurfaceControl surfaceControl = mDragState.mSurfaceControl;
            if (SHOW_LIGHT_TRANSACTIONS) Slog.i(
                    TAG_WM, ">>> OPEN TRANSACTION performDrag");
            mService.openSurfaceTransaction();
            try {
                surfaceControl.setPosition(touchX - thumbCenterX,
                        touchY - thumbCenterY);
                surfaceControl.setLayer(mDragState.getDragLayerLocked());
                surfaceControl.setLayerStack(display.getLayerStack());
                surfaceControl.show();
            } finally {
                mService.closeSurfaceTransaction("performDrag");
                if (SHOW_LIGHT_TRANSACTIONS) Slog.i(
                        TAG_WM, "<<< CLOSE TRANSACTION performDrag");
            }

            mDragState.notifyLocationLocked(touchX, touchY);
        }

        return true;    // success!
    }

    void reportDropResult(IWindow window, boolean consumed) {
        IBinder token = window.asBinder();
        if (DEBUG_DRAG) {
            Slog.d(TAG_WM, "Drop result=" + consumed + " reported by " + token);
        }

        synchronized (mService.mWindowMap) {
            if (mDragState == null) {
                // Most likely the drop recipient ANRed and we ended the drag
                // out from under it.  Log the issue and move on.
                Slog.w(TAG_WM, "Drop result given but no drag in progress");
                return;
            }

            if (mDragState.mToken != token) {
                // We're in a drag, but the wrong window has responded.
                Slog.w(TAG_WM, "Invalid drop-result claim by " + window);
                throw new IllegalStateException("reportDropResult() by non-recipient");
            }

            // The right window has responded, even if it's no longer around,
            // so be sure to halt the timeout even if the later WindowState
            // lookup fails.
            mHandler.removeMessages(MSG_DRAG_END_TIMEOUT, window.asBinder());
            WindowState callingWin = mService.windowForClientLocked(null, window, false);
            if (callingWin == null) {
                Slog.w(TAG_WM, "Bad result-reporting window " + window);
                return;  // !!! TODO: throw here?
            }

            mDragState.mDragResult = consumed;
            mDragState.endDragLocked();
        }
    }

    void cancelDragAndDrop(IBinder dragToken) {
        if (DEBUG_DRAG) {
            Slog.d(TAG_WM, "cancelDragAndDrop");
        }

        synchronized (mService.mWindowMap) {
            if (mDragState == null) {
                Slog.w(TAG_WM, "cancelDragAndDrop() without prepareDrag()");
                throw new IllegalStateException("cancelDragAndDrop() without prepareDrag()");
            }

            if (mDragState.mToken != dragToken) {
                Slog.w(TAG_WM,
                        "cancelDragAndDrop() does not match prepareDrag()");
                throw new IllegalStateException(
                        "cancelDragAndDrop() does not match prepareDrag()");
            }

            mDragState.mDragResult = false;
            mDragState.cancelDragLocked();
        }
    }

    void dragRecipientEntered(IWindow window) {
        if (DEBUG_DRAG) {
            Slog.d(TAG_WM, "Drag into new candidate view @ " + window.asBinder());
        }
    }

    void dragRecipientExited(IWindow window) {
        if (DEBUG_DRAG) {
            Slog.d(TAG_WM, "Drag from old candidate view @ " + window.asBinder());
        }
    }

    /**
     * Sends a message to the Handler managed by DragDropController.
     */
    void sendHandlerMessage(int what, Object arg) {
        mHandler.obtainMessage(what, arg).sendToTarget();
    }

    /**
     * Sends a timeout message to the Handler managed by DragDropController.
     */
    void sendTimeoutMessage(int what, Object arg) {
        mHandler.removeMessages(what, arg);
        final Message msg = mHandler.obtainMessage(what, arg);
        mHandler.sendMessageDelayed(msg, DRAG_TIMEOUT_MS);
    }

    private class DragHandler extends Handler {
        /**
         * Lock for window manager.
         */
        private final WindowManagerService mService;

        DragHandler(WindowManagerService service, Looper looper) {
            super(looper);
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DRAG_START_TIMEOUT: {
                    IBinder win = (IBinder) msg.obj;
                    if (DEBUG_DRAG) {
                        Slog.w(TAG_WM, "Timeout starting drag by win " + win);
                    }
                    synchronized (mService.mWindowMap) {
                        // !!! TODO: ANR the app that has failed to start the drag in time
                        if (mDragState != null) {
                            mDragState.unregister();
                            mDragState.reset();
                            mDragState = null;
                        }
                    }
                    break;
                }

                case MSG_DRAG_END_TIMEOUT: {
                    IBinder win = (IBinder) msg.obj;
                    if (DEBUG_DRAG) {
                        Slog.w(TAG_WM, "Timeout ending drag to win " + win);
                    }
                    synchronized (mService.mWindowMap) {
                        // !!! TODO: ANR the drag-receiving app
                        if (mDragState != null) {
                            mDragState.mDragResult = false;
                            mDragState.endDragLocked();
                        }
                    }
                    break;
                }

                case MSG_TEAR_DOWN_DRAG_AND_DROP_INPUT: {
                    if (DEBUG_DRAG)
                        Slog.d(TAG_WM, "Drag ending; tearing down input channel");
                    DragState.InputInterceptor interceptor = (DragState.InputInterceptor) msg.obj;
                    if (interceptor != null) {
                        synchronized (mService.mWindowMap) {
                            interceptor.tearDown();
                        }
                    }
                    break;
                }

                case MSG_ANIMATION_END: {
                    synchronized (mService.mWindowMap) {
                        if (mDragState == null) {
                            Slog.wtf(TAG_WM, "mDragState unexpectedly became null while " +
                                    "plyaing animation");
                            return;
                        }
                        mDragState.onAnimationEndLocked();
                    }
                    break;
                }
            }
        }
    }
}
