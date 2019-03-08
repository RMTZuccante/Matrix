package it.rmtz.utils;

import it.rmtz.camera.Camera;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Preloader {
    public static void main(String[] args) {
        try {
            ServerSocket prserver = new ServerSocket(1026, 10, InetAddress.getLocalHost());
            new Thread(() -> {
                for (; ; ) {
                    try {
                        Socket client = prserver.accept();
                        Scanner in = new Scanner(client.getInputStream());
                        while (client.isConnected()) {
                            System.out.println(in.next());
                        }
                    } catch (IOException e) {
                        System.exit(-1);
                    }
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Another preloader is already running, passing arguments to it");
            if (args.length > 0) {
                try {
                    Socket client = new Socket(InetAddress.getLocalHost(), 1026);
                    OutputStreamWriter out = new OutputStreamWriter(client.getOutputStream());
                    for (int i = 0; i < args.length - 1; i++) {
                        out.write(args[i]);
                        out.write(1);
                    }
                    out.write(args[args.length - 1]);
                    out.flush();
                    out.close();
                    client.close();
                } catch (IOException e1) {
                    System.err.println("Cannot connect to preloader");
                    System.exit(-1);
                }
            } else
                System.err.println("No arguments to pass");
            System.exit(0);
        }


        String lp = null;
        String modelpath = "./model/model.dl4j";
        if (args != null && args.length > 0) {
            int i = 0;
            try {
                for (i = 0; i < args.length; i++) {
                    if (args[i].equals("-lp")) {
                        lp = args[++i];
                    } else if (args[i].equals("-mp")) {
                        modelpath = args[++i];
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Missing argument after [" + args[i - 1] + "]l");
                System.exit(-1);
            }
        }

        if (!Camera.loadLib(lp)) System.exit(-1);
        File f = new File(modelpath);
        MultiLayerNetwork model = null;
        if (f.exists() && f.canRead()) {
            try {
                model = ModelSerializer.restoreMultiLayerNetwork(f.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        exec(args);
    }

    public static void exec(String[] args) {
        VideoCapture c = new VideoCapture(0);
        if (!c.open(0)) System.out.println("niente");
        Mat m = new Mat();
        while (c.isOpened() && c.read(m)) {
            HighGui.imshow("Camera", m);
            HighGui.waitKey(1);
        }
    }
}
