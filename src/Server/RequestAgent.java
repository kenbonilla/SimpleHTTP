package server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;


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

public class RequestAgent implements Runnable {

    protected File root;
    protected String page = "index.html";
    protected Socket conn;
    protected final String header_ok = "HTTP/1.0 200 OK\n ";
    protected final String header_401 = "HTTP/1.0 200 OK\n ";
    protected final String header_501 = "HTTP/1.0 200 OK\n ";
    //incorrect header strings used to force display of error messages
    //protected final String header_401 = "HTTP/1.0 404 File Not Found text/html; charset=utf-8";
    //protected final String header_501 = "HTTP/1.0 501 Not Implemented text/html; charset=utf-8";
    protected final String header_html = "text/html; charset=utf-8\n";
    private byte[] head = null;
    private byte[] data = null;
    private byte[] send = null;
    
    public RequestAgent(String page_, Socket conn_, File root_) {
        if (root_.isFile()) {
            throw new IllegalArgumentException("Not a directory");
        }
        try {
            root_=root_.getCanonicalFile();
        }catch (IOException ex) {
            System.out.println("Unable to fetch canonical filename");
        }
        root = root_;
        if(page_ != null) page = page_;
        conn = conn_;
        
    }
    @Override
    public void run() {
        try {
            //create the input and output streams
            OutputStream os = new BufferedOutputStream(conn.getOutputStream());
            Writer out = new OutputStreamWriter(os);
            Reader in = new InputStreamReader(new BufferedInputStream(conn.getInputStream()),StandardCharsets.UTF_8);
            StringBuilder request = new StringBuilder();
            //build the request
            //should return something like GET /index.html HTTP/1.1
            while(true) {
                char ch = (char) in.read();
                if(ch == '\r' || ch == '\n') break; //if return or newline, break the read
                request.append(ch);
            }
            
            String getPage = request.toString();
            //System.out.println("\n\ngetPage string = " + getPage);
            
            //tokenize the request
            String[] tokens = getPage.split("\\s+"); //tokenize by whitespace
            String reqType = tokens[0]; //example GET
            String version = ""; 
            if(reqType.equals("GET")) {
                //System.out.println("request type GET");
                String pathAndFile = tokens[1]; //relative path and filename
                if (pathAndFile.endsWith("/")) pathAndFile += "index.html"; //if path only, go to index
                String contentType = URLConnection.getFileNameMap().getContentTypeFor(pathAndFile); //gets the content type
                if(tokens.length > 2) version = tokens[2];
                
                //Time to read the file
                //chop off the initial '/' in pathAndFile
                File file = new File(root, pathAndFile.substring(1, pathAndFile.length()));
                //System.out.println(file.toString());
                //System.out.println("opened file");
                if(file.canRead()) {
                    //System.out.println("reading the file");
                    data = Files.readAllBytes(file.toPath());
                    if(version.startsWith("HTTP/")) {
                        //System.out.println("sending header");
                        head = craftHeader(header_ok, contentType, data.length).getBytes(StandardCharsets.UTF_8);
                        send = concat(head,data);
                        os.write(send);
                        out.flush();
                        System.out.println("page sent");
//                        System.out.println("sending header");
//                        out.write(craftHeader(header_ok, contentType, data.length));
//                        out.flush();
                    }
                    
//                    os.write(data);//out goes the html
//                    os.flush();
                    
                }else {
                    System.out.println("file not found");
                    //file not found
                    File file_401 = new File(root, "401.html");
                    data = Files.readAllBytes(file_401.toPath());
                    head = craftHeader(header_401, contentType, data.length).getBytes(StandardCharsets.UTF_8);
                    send = concat(head,data);
                    os.write(send);//out goes the html
                    os.flush();
                }
            }else {
              //Not supported because this server only understands GET
                File file_501 = new File(root, "501.html");
                data = Files.readAllBytes(file_501.toPath());
                head = craftHeader(header_501, header_html, data.length).getBytes(StandardCharsets.UTF_8);
                send = concat(head,data);
                os.write(send);//out goes the html
                os.flush();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Builds the header as a string from multiple properties
     * @param code_ http code like 200 OK
     * @param type_ mime type, not currently used
     * @param length_ length of byte array
     * @return string of header
     */
    private String craftHeader(String code_, String type_, int length_) {
        //String result = code_ + " \nContent-Type: " + type_ + " \n Content-length: " + length_ + " \n\n";
        //server only works with html anyway, so making type fixed, also corrects charset
        String result = code_ + header_html + " \n Content-length: " + length_ + " \n\n";

        return result;
        
    }
    
    /**
     * Concats the header and data byte arrays
     * @param a 1st array
     * @param b 2nd array
     * @return concat [] a+b
     * @throws IOException
     */
    public byte[] concat(byte[] a, byte[] b) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write( a );
        outputStream.write( b );
        byte[] result = outputStream.toByteArray();
        return result;
    }
}
