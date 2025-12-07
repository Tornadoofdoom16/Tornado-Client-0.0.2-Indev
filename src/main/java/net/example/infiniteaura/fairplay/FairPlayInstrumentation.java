package net.example.infiniteaura.fairplay;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple instrumentation for FairPlay signals to measure timings.
 *
 * Records emit time, reception time, and activation time per-signal (nonce)
 * and appends CSV rows to `fairplay_rtt.csv` in the working directory.
 */
public class FairPlayInstrumentation {

    private static FairPlayInstrumentation instance;

    private final Map<String,Entry> entries = new HashMap<>();
    private final File outFile;
    private final Object lock = new Object();

    private FairPlayInstrumentation() {
        outFile = new File("fairplay_rtt.csv");
        // Ensure file has header if new
        try {
            boolean create = !outFile.exists();
            try (PrintWriter pw = new PrintWriter(new FileWriter(outFile, true))) {
                if (create) {
                    pw.println("nonce,action,emittedAt,receivedAt,accepted,activatedAt,emit_to_recv_ms,recv_to_act_ms,senderHash");
                }
            }
        } catch (IOException ignored) {}
    }

    public static synchronized FairPlayInstrumentation getInstance() {
        if (instance == null) instance = new FairPlayInstrumentation();
        return instance;
    }

    public void recordEmit(String nonce, String actionId, long emittedAtMs, String senderHash) {
        synchronized (lock) {
            Entry e = entries.getOrDefault(nonce, new Entry(nonce));
            e.action = actionId;
            e.emittedAt = emittedAtMs;
            e.senderHash = senderHash;
            entries.put(nonce, e);
            // try flush if we already have reception/activation
            tryFlush(e);
            pruneOld();
        }
    }

    public void recordReception(String nonce, long receivedAtMs, boolean accepted) {
        synchronized (lock) {
            Entry e = entries.getOrDefault(nonce, new Entry(nonce));
            e.receivedAt = receivedAtMs;
            e.accepted = accepted;
            entries.put(nonce, e);
            // If not accepted, flush now (no activation expected)
            if (!accepted) {
                flushEntry(e);
                entries.remove(nonce);
            } else {
                tryFlush(e);
            }
            pruneOld();
        }
    }

    public void recordActivation(String nonce, long activatedAtMs) {
        synchronized (lock) {
            Entry e = entries.getOrDefault(nonce, new Entry(nonce));
            e.activatedAt = activatedAtMs;
            entries.put(nonce, e);
            // Activation completes the record
            flushEntry(e);
            entries.remove(nonce);
            pruneOld();
        }
    }

    private void tryFlush(Entry e) {
        if (e.emittedAt > 0 && e.receivedAt > 0 && (e.activatedAt > 0 || !e.accepted)) {
            flushEntry(e);
            entries.remove(e.nonce);
        }
    }

    private void flushEntry(Entry e) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(outFile, true))) {
            long emit_to_recv = (e.emittedAt > 0 && e.receivedAt > 0) ? (e.receivedAt - e.emittedAt) : -1;
            long recv_to_act = (e.receivedAt > 0 && e.activatedAt > 0) ? (e.activatedAt - e.receivedAt) : -1;
            pw.print(e.nonce);
            pw.print(','); pw.print(e.action == null ? "" : e.action);
            pw.print(','); pw.print(e.emittedAt);
            pw.print(','); pw.print(e.receivedAt);
            pw.print(','); pw.print(e.accepted);
            pw.print(','); pw.print(e.activatedAt);
            pw.print(','); pw.print(emit_to_recv);
            pw.print(','); pw.print(recv_to_act);
            pw.print(','); pw.print(e.senderHash == null ? "" : e.senderHash);
            pw.println();
        } catch (IOException ignored) {}
    }

    private void pruneOld() {
        long cutoff = System.currentTimeMillis() - 60_000; // keep 60s
        Iterator<Map.Entry<String,Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,Entry> me = it.next();
            Entry e = me.getValue();
            long recent = Math.max(e.emittedAt, Math.max(e.receivedAt, e.activatedAt));
            if (recent > 0 && recent < cutoff) it.remove();
        }
    }

    private static class Entry {
        final String nonce;
        String action = null;
        long emittedAt = 0;
        long receivedAt = 0;
        boolean accepted = false;
        long activatedAt = 0;
        String senderHash = null;

        Entry(String nonce) { this.nonce = nonce; }
    }
}
