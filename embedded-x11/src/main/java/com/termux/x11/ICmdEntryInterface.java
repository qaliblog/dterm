package com.termux.x11;

import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import java.io.IOException;

/**
 * Binder interface exposed by the dedicated Android X11 service process to the viewer. The
 * viewer (running in the main application process) binds to the :x11 service and receives this
 * interface through Binder, so it can present frames to the native Xorg server without a TCP
 * connection or an ACTION_START broadcast handshake.
 */
public interface ICmdEntryInterface extends android.os.IInterface {

    /**
     * Returns the X11 connection (a {@link ParcelFileDescriptor} wrapping the Xorg Unix socket)
     * for the viewer to use as its display.
     */
    ParcelFileDescriptor getXConnection() throws RemoteException;

    /** Returns the logcat output of the X11 service process, or {@code null} when unavailable. */
    String getLogcatOutput() throws RemoteException;

    /** Stub used by the service to publish this interface. */
    abstract class Stub extends android.os.Binder implements ICmdEntryInterface {

        private static final String DESCRIPTOR = "com.termux.x11.ICmdEntryInterface";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static ICmdEntryInterface asInterface(IBinder binder) {
            if (binder == null) return null;
            android.os.IInterface iin = binder.queryLocalInterface(DESCRIPTOR);
            if (iin != null) {
                if (iin instanceof ICmdEntryInterface) {
                    return (ICmdEntryInterface) iin;
                }
            }
            return new Proxy(binder);
        }

        @Override
        public String getInterfaceDescriptor() {
            return DESCRIPTOR;
        }
    }

    /** Proxy used for cross-process calls. */
    class Proxy implements ICmdEntryInterface {
        private final IBinder mRemote;

        Proxy(IBinder remote) {
            this.mRemote = remote;
        }

        @Override
        public IBinder asBinder() {
            return mRemote;
        }

        @Override
        public ParcelFileDescriptor getXConnection() throws RemoteException {
            try {
                return ParcelFileDescriptor.fromFd(0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getLogcatOutput() throws RemoteException {
            return null;
        }
    }
}
