package com.muniv24.volumebar;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * Runs one shell command through the TV's own network debugging (adb on 127.0.0.1:5555), so the
 * app can give itself the "show over other apps" permission without a computer. Only talks to the
 * TV it runs on. Speaks the small part of the adb protocol needed: connect, sign in, run, close.
 */
final class LocalAdb implements Closeable {
    static final int PORT = 5555;

    private static final int CNXN = 0x4e584e43, AUTH = 0x48545541, OPEN = 0x4e45504f;
    private static final int OKAY = 0x59414b4f, WRTE = 0x45545257, CLSE = 0x45534c43;
    private static final int AUTH_TOKEN = 1, AUTH_SIGNATURE = 2, AUTH_PUBLIC_KEY = 3;
    private static final int VERSION = 0x01000000, MAX_PAYLOAD = 4096;
    private static final int LOCAL_ID = 1;
    private static final byte[] NONE = new byte[0];

    private final Socket socket = new Socket();
    private final DataInputStream in;
    private final OutputStream out;

    /** Connects and signs in. The first time, waits up to approveMs for "Allow" on the TV. */
    LocalAdb(Context c, int approveMs) throws IOException, GeneralSecurityException {
        socket.connect(new InetSocketAddress("127.0.0.1", PORT), 3000);
        socket.setSoTimeout(5000);
        in = new DataInputStream(socket.getInputStream());
        out = socket.getOutputStream();
        signIn(AdbKey.load(c), approveMs);
    }

    private void signIn(AdbKey key, int approveMs) throws IOException, GeneralSecurityException {
        send(CNXN, VERSION, MAX_PAYLOAD, "host::\0".getBytes(StandardCharsets.UTF_8));
        boolean signed = false, keySent = false;
        while (true) {
            Packet p = receive();
            if (p.command == CNXN) return;
            if (p.command != AUTH || p.arg0 != AUTH_TOKEN) throw new IOException("Unexpected answer from the TV");
            if (!signed) {
                // Already trusted keys get in with a signature.
                send(AUTH, AUTH_SIGNATURE, 0, key.sign(p.data));
                signed = true;
            } else if (!keySent) {
                // A new key: send it, and the TV asks the user "Allow debugging?".
                send(AUTH, AUTH_PUBLIC_KEY, 0, key.publicKeyForTv());
                keySent = true;
                socket.setSoTimeout(approveMs);
            } else {
                throw new IOException("The TV did not allow debugging");
            }
        }
    }

    /** Runs a shell command and returns what it printed. */
    String shell(String command) throws IOException {
        send(OPEN, LOCAL_ID, 0, ("shell:" + command + "\0").getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        socket.setSoTimeout(15000);
        while (true) {
            Packet p = receive();
            if (p.command == WRTE) {
                printed.write(p.data);
                send(OKAY, LOCAL_ID, p.arg0, NONE);
            } else if (p.command == CLSE) {
                send(CLSE, LOCAL_ID, p.arg0, NONE);
                return new String(printed.toByteArray(), StandardCharsets.UTF_8);
            }
            // OKAY: the TV opened the command; nothing to do.
        }
    }

    @Override public void close() throws IOException {
        socket.close();
    }

    private void send(int command, int arg0, int arg1, byte[] data) throws IOException {
        int sum = 0;
        for (byte d : data) sum += d & 0xff;
        ByteBuffer header = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(command).putInt(arg0).putInt(arg1).putInt(data.length).putInt(sum).putInt(~command);
        out.write(header.array());
        out.write(data);
        out.flush();
    }

    private Packet receive() throws IOException {
        byte[] raw = new byte[24];
        in.readFully(raw);
        ByteBuffer header = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        Packet p = new Packet();
        p.command = header.getInt();
        p.arg0 = header.getInt();
        p.arg1 = header.getInt();
        int length = header.getInt();
        if (length < 0 || length > 1024 * 1024) throw new IOException("Bad answer from the TV");
        p.data = new byte[length];
        in.readFully(p.data);
        return p;
    }

    private static final class Packet {
        int command, arg0, arg1;
        byte[] data;
    }
}
