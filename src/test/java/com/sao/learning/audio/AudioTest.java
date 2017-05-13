package com.sao.learning.audio;

import org.junit.Test;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by saopr on 5/10/2017.
 */
public class AudioTest {

    private static final float SAMPLING_RATE = 16000;
    private static final int SAMPLE_SIZE = 16;
    private static final int MONO = 1;
    private static final int FRAME_SIZE = 2;
    private static final float FRAME_RATE = 16000;
    private static final boolean BIG_ENDIAN = false;

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    @Test
    public void testRecording() {
        TargetDataLine targetDataLine = null;
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLING_RATE, SAMPLE_SIZE, MONO, FRAME_SIZE, FRAME_RATE, BIG_ENDIAN);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Audio system does not support line.");
        }

        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
        } catch (LineUnavailableException e) {
            throw new RuntimeException("Could not get target line from audio system.");
        }

        final TargetDataLine dataLine = targetDataLine;

        targetDataLine.addLineListener(update -> {
            LineEvent.Type type = update.getType();
            Long frame = update.getFramePosition();
            System.out.format("Listener type %s, FramePosition is %s, TargetDataLine is running %s, is active %s%n",
                    type, frame, dataLine.isRunning(), dataLine.isActive());
        });

        final AtomicBoolean stopped = new AtomicBoolean();
        stopped.set(false);

        List<Callable<Integer>> callables = Arrays.asList(
            () -> {
                byte[] rawData = new byte[dataLine.getBufferSize() / 5];
                int bytesRead;

                dataLine.start();

                while (!stopped.get()) {
                    bytesRead = dataLine.read(rawData, 0, rawData.length);
                    byteArrayOutputStream.write(rawData, 0, bytesRead);
                }

                dataLine.stop();
                dataLine.close();

                return 0;
            },
            () -> {
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(1000);
                        System.out.format("Timer tick %s...%n", i);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                stopped.set(true);
                return 0;
            }
        );

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            executorService.invokeAll(callables);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }

        testPlayback();
    }

    public void testPlayback() {
        SourceDataLine sourceDataLine = null;
        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, SAMPLING_RATE, SAMPLE_SIZE, MONO, FRAME_SIZE, FRAME_RATE, BIG_ENDIAN);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Audio system does not support line.");
        }

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

        try {
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(audioFormat);
        } catch (LineUnavailableException e) {
            throw new RuntimeException("Could not get source line from audio system.");
        }

        final SourceDataLine dataLine = sourceDataLine;

        sourceDataLine.addLineListener(update -> {
            LineEvent.Type type = update.getType();
            Long frame = update.getFramePosition();
            System.out.format("Listener type %s, FramePosition is %s, SourceDataLine is running %s, is active %s%n",
                    type, frame, dataLine.isRunning(), dataLine.isActive());
        });

        Runnable runnable = () -> {
            byte[] rawData = new byte[dataLine.getBufferSize() / 5];
            int bytesRead;

            dataLine.start();

            while (-1 != (bytesRead = byteArrayInputStream.read(rawData, 0, rawData.length))) {
                dataLine.write(rawData, 0, bytesRead);
            }

            dataLine.drain();
        };

        try {
            Thread thread = new Thread(runnable);
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        sourceDataLine.stop();
        sourceDataLine.close();
    }
}
