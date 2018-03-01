package server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Copyright 2018 Kenneth Bonilla,
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * 
 * @author Kenneth Bonilla kfbonill@asu.edu
 * @version February, 2018
 **/

public class Server {
    private int id;
    private int port;

    private  String indexFile = "index.html";
    private  File rootDir = null;

    /**
     * Server 
     * @param rootDir absolute path of htdocs
     * @param port listen to this port number
     */
    public Server(File rootDir, int port) {

        this.rootDir = rootDir;
        this.port = port;
        this.id = 0;

    }

    /**
     * Start the server, instantiates request agents as separate threads
     */
    public void start() {
        ExecutorService pool = Executors.newFixedThreadPool(100);
        System.out.println("Server started on port " + port);
        try (ServerSocket server_ = new ServerSocket(port)) {
            //wait for a client request
            while (true) {
                try {
                    Socket request = server_.accept();
                    System.out.println("Accepted client : " + (++id));
                    Runnable runTask = new RequestAgent(indexFile, request, rootDir);
                    pool.submit(runTask);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    /**
     * Initiates a single thread to act as server, the multithreading will occur as a 
     * Request Agent implementing runnable. Initiate the server with arg port
     * @param args
     * @throws IOException
     */
    public static void main(String args[]) throws IOException {
        //get user directory as root
        String docrootstring = System.getProperty("user.dir") + System.getProperty("file.separator") + "www";
        File docroot = new File(docrootstring);
        System.out.println(docroot.toString());
        int port = 8888;
        //get port from args
        try {
            if (args.length != 1) {
                System.out.println("Usage: java ser321.sockets.ThreadedEchoServer" + " [portNum]");
                System.exit(0);
            }
            port = Integer.parseInt(args[0]);

            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        //start the server
        Server myServer = new Server(docroot, port);
        myServer.start();

    }



}
