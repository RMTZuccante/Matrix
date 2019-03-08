package it.rmtz;

import com.fazecast.jSerialComm.SerialPort;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import it.rmtz.camera.Camera;
import it.rmtz.camera.ModelLoader;
import it.rmtz.matrix.Matrix;
import it.rmtz.matrix.SerialConnector;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class Brain {
    private static int cl, cr, thresh, minArea, maxAra, offset, distwall;
    private static float bodytemp;
    private static char[] ref;
    private static double precision;
    private static String libpath;

    private static SerialPort stm;

    private static Thread shutdown = new Thread(() -> {
        System.out.println("Closing serial port: " + (stm.closePort() ? "TRUE" : "FALSE"));
    });

    public static void main(String[] args) {
        JsonObject config = null;
        Camera left = null, right = null;

        String modelpath = "./model/model.dl4j";

        try {
            config = new JsonParser().parse(new JsonReader(new FileReader("config.json"))).getAsJsonObject();
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find config.json");
            System.exit(-1);
        }

        if (getValuesFromJson(config)) {
            if (Camera.loadLib(libpath)) {
                MultiLayerNetwork model = null;
                try {
                    model = ModelLoader.loadModel(modelpath);
                } catch (ModelLoader.ModelLoaderException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }

                if (model != null) {
                    left = new Camera(model, ref, minArea, maxAra, thresh, offset, precision);
                    right = new Camera(model, ref, minArea, maxAra, thresh, offset, precision);

                    if (!left.open(cl)) {
                        System.err.println("Error opening left camera. index: " + cl);
                    }

                    if (!right.open(cr)) {
                        System.err.println("Error opening right camera. index: " + cr);
                    }
                }
            } else {
                System.err.println("Error loading lib, provided path may be wrong");
            }
        } else {
            System.err.println("Error loading config.json");
            System.exit(-1);
        }

        stm = null;
        for (SerialPort p : SerialPort.getCommPorts()) {
            if (p.getDescriptivePortName().equals("Maple")) {
                stm = p;
                break;
            }
        }

        if (stm == null) {
            System.err.println("Cannot find serial port connected to Maple, trying with args");
            if (args.length > 0) {
                stm = SerialPort.getCommPort(args[0]);
            } else {
                System.err.println("No serial port provided");
                System.exit(-1);
            }
        }

        System.out.println("Ready to start");
        SerialConnector c = new SerialConnector(stm, 115200);
        Matrix m = new Matrix(c, left, right, distwall, bodytemp);
        Runtime.getRuntime().addShutdownHook(shutdown);
        m.start();
    }

    private static boolean getValuesFromJson(JsonObject obj) {
        try {
            cl = obj.get("CAMERA_LEFT").getAsInt();
            cr = obj.get("CAMERA_RIGHT").getAsInt();
            JsonArray jsonRef = obj.get("ref").getAsJsonArray();
            ref = new char[jsonRef.size()];
            for (int i = 0; i < ref.length; i++) {
                ref[i] = jsonRef.get(i).getAsCharacter();
            }
            thresh = obj.get("THRESH").getAsInt();
            minArea = obj.get("MIN_AREA").getAsInt();
            maxAra = obj.get("MAX_AREA").getAsInt();
            offset = obj.get("OFFSET").getAsInt();
            precision = obj.get("PRECISION").getAsDouble();
            libpath = obj.get("LIBPATH").getAsString();
            bodytemp = obj.get("BODYTEMP").getAsFloat();
            distwall = obj.get("DISTWALL").getAsInt();
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }
}
