package cz.sam.voicechat;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {

    public static class Client_Reader implements Runnable {

        private DataInputStream dataInputStream;

        public Client_Reader(Socket socket) throws IOException {
            this.dataInputStream = new DataInputStream(socket.getInputStream());
        }

        @Override
        public void run() {
            try {
                final AudioFormat af = new AudioFormat(8000.0f, 16, 1, true, true);
                SourceDataLine sourceDataline = AudioSystem.getSourceDataLine(af);

                sourceDataline.open(af, 1600);
                sourceDataline.start();

                byte[] buffer = new byte[1600];

                while(true) {
                    if(this.dataInputStream.read(buffer, 0, buffer.length) > 0) {
                        sourceDataline.write(buffer, 0, buffer.length);
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void start() {
            new Thread(this).start();
        }
    }

    public static class Client_Writer implements Runnable {

        private DataOutputStream dataOutputStream;

        public Client_Writer(Socket socket) throws IOException {
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void run() {
            try {
                AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
                TargetDataLine line = AudioSystem.getTargetDataLine(format);

                line.open(format);

                line.start();

                int numBytesRead;
                byte[] data = new byte[line.getBufferSize() / 5];

                while(true) {
                    numBytesRead =  line.read(data, 0, data.length);

                    if(numBytesRead > 0) {
                        int rmsLevel = calculateRMSLevel(data);

                        this.dataOutputStream.write(data, 0, data.length);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void start() {
            new Thread(this).start();
        }
    }


    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("176.9.193.7", 25566);

        new Client_Reader(socket).start();
        new Client_Writer(socket).start();
    }














    public static void old_main(String[] args) throws Exception{
        final AudioFormat af = new AudioFormat(8000.0f, 16, 1, true, true);
        SourceDataLine sourceDataline = AudioSystem.getSourceDataLine(af);


        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
        TargetDataLine line = AudioSystem.getTargetDataLine(format);

        line.open(format);

        line.start();

        int numBytesRead;
        byte[] data = new byte[line.getBufferSize() / 5];

        sourceDataline.open(af, line.getBufferSize() / 5);
        sourceDataline.start();

        System.out.println(data.length);

        while(true) {
            numBytesRead =  line.read(data, 0, data.length);

            if(numBytesRead > 0) {
                int rmsLevel = calculateRMSLevel(data);

                sourceDataline.write(data, 0, numBytesRead);
            }
        }

        //line.close();
    }

    public static int calculateRMSLevel(byte[] audioData)
    {
        long lSum = 0;
        for(int i=0; i < audioData.length; i++)
            lSum = lSum + audioData[i];

        double dAvg = lSum / audioData.length;
        double sumMeanSquare = 0d;

        for(int j=0; j < audioData.length; j++)
            sumMeanSquare += Math.pow(audioData[j] - dAvg, 2d);

        double averageMeanSquare = sumMeanSquare / audioData.length;

        return (int)(Math.pow(averageMeanSquare,0.5d) + 0.5);
    }

}
