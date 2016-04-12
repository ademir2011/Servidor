/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidor;

import Reply.Reply;
import Request.Request;
import banco.Usuario;
import banco.UsuarioDAO;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

/**
 *
 * @author ademir
 */
public class Servidor {

    public static final String dir = "/home/ademir/OneBox/";
    public static final int PORTA = 10300;
    
    private static UsuarioDAO usuarioDAO;
    private static Usuario usuario;
    private static ListFilesUtil listFilesUtil;
    private static List<String> paths;
    private static volatile boolean autenticado = true;
    
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        
        try {
                
            ServerSocket srvSocket = new ServerSocket(PORTA);
            
            

            while(true){
                
                
                System.out.println("... Esperando ...");

                Socket socket = srvSocket.accept();
                
                usuarioDAO      = new UsuarioDAO();
                usuario         = new Usuario();
                listFilesUtil   = new ListFilesUtil();
                paths           = new ArrayList<>();
                
                ois             = new ObjectInputStream( socket.getInputStream() );
                oos             = new ObjectOutputStream( socket.getOutputStream() );

                new Thread(new EchoThread(socket, oos, ois)).start();
                    
            }
                
               
                
        } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        
    }
    
    public static class EchoThread implements Runnable {

        private Socket socket;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        
        public EchoThread(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
            this.socket = socket;
            this.oos = oos;
            this.ois = ois;
        }

        @Override
        public void run() {
            
            while (autenticado) {
                            
                System.out.println("Socket aberto"+socket.getRemoteSocketAddress());

                System.out.println("recebido");

                Request request = null;

                try {
                    request = (Request) ois.readObject();
                } catch (IOException ex) {
                    Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    break;
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    break;  
                }
                

                System.out.println("Objeto request recebido com a operaçao de "+request.getOperacao());

                if(request.getOperacao().equals("registrar")){

                    try {
                        registroNoBanco(request, socket, oos);
                    } catch (IOException ex) {
                        Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                        
                    try {
                        autenticado = fecharConexao(socket, ois, oos);
                    } catch (IOException ex) {
                        Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    

                } else if (request.getOperacao().equals("logar")) {
                    autenticaUsuario(request);
                    try {
                        if(autenticado = logarUsuario(socket, oos)){
                            
                        } else {
                            autenticado = fecharConexao(socket, ois, oos);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    

                } else if ( request.getOperacao().equals("e/r lista de arquivos") ) {

                    try {
                        devolverListaDeArquivos(socket, oos);
                    } catch (IOException ex) {
                        Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else if ( request.getOperacao().equals("upload") ) {
                    
                    try {
                        uploadFile(request);
                    } catch (IOException ex) {
                        Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    try {
                        enviarMsgReply(socket);
                    } catch (IOException ex) {
                        Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else if ( request.getOperacao().equals("download")) {
                    try {
                        downloadFile(socket, request);
                    } catch (IOException ex) {
                        Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                }

            }
            
        }
        
    }
    
    public static byte[] fileToByteArray(File file) throws FileNotFoundException, IOException{
        FileInputStream fis;
        byte[] bFile = new byte[(int) file.length()];
        fis = new FileInputStream(file);
        fis.read(bFile);
        fis.close();
        return bFile;
    }
    
    public static boolean registraUsuario(Socket socket, Usuario usuario, ObjectOutputStream oos) throws IOException{
        
        if(usuarioDAO.checkRegister(usuario)){
            usuarioDAO.addUsuario(usuario);
           
            sendObject( new Reply("Registrado com sucesso!"), socket, oos);
            
            return true;

        } else {
            
            sendObject( new Reply("Ja existe dados com esse login e senha!"), socket, oos);

            return false;
        }
        
    }
    
    public static void createDirectory(){
        
        long id = usuarioDAO.getIdByUser(usuario);
        new File(dir+id).mkdirs();
        System.out.println("Diretorio criado com sucesso");
        
    }
    
    public static boolean logarUsuario(Socket socket, ObjectOutputStream oos) throws IOException{
        
        if(usuarioDAO.checkLogin(usuario)){
            
            Reply reply = new Reply("Autenticado !");
            
            long id = usuarioDAO.getIdByUser(usuario);
            
            reply.setUserId(id);
            reply.setLogin(usuario.getLogin());
            reply.setSenha(usuario.getSenha());
            
            sendObject( reply, socket, oos);
            
            return true;
            
        } else {
            sendObject( new Reply("Login ou senha errado !"), socket, oos);
            return false;
        }
        
    }
    
    public static void sendObject(Reply replyRegistro, Socket socket, ObjectOutputStream oos) throws IOException{
        oos.writeObject(replyRegistro);
    }
    
    public static void receiverAndSendObjectRequestReply(Socket socket, Reply reply) throws IOException, ClassNotFoundException{
        
        ObjectInputStream ois = ois = new ObjectInputStream( socket.getInputStream() );
                          
        Request request = (Request) ois.readObject();
        
    }
    
    public static void autenticaUsuario(Request request){
        usuario.setLogin(request.getLogin());
        usuario.setSenha(request.getSenha());

        long id = usuarioDAO.getIdByUser(usuario);

        usuario.setId(id);
    }
    
    public static void registroNoBanco(Request request, Socket socket, ObjectOutputStream oos) throws IOException{
        
        usuario.setLogin(request.getLogin());
        usuario.setSenha(request.getSenha());
        long id = usuarioDAO.getIdByUser(usuario);
        usuario.setId(id);
        
        if(registraUsuario(socket, usuario, oos)){
            createDirectory();
        } else {
            System.out.println("Falha ao criar diretorio");
        }
        
    }
    
    public static void devolverListaDeArquivos(Socket socket, ObjectOutputStream oos) throws IOException{
        
        Map<String, String> paths = new HashMap<String, String>();
        
        paths = listFilesUtil.listFilesAndFilesSubDirectories(paths, dir+usuario.getId()+"/");
        
        paths = getSplitPaths(paths, dir+usuario.getId()+"/");
        
        Reply reply = new Reply();
        reply.setPaths(paths);
        reply.setObs("Lista de arquivos do servidor atualizadas para o cliente");

        sendObject(reply, socket, oos);
       
    }
    
    
    public static boolean fecharConexao(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) throws IOException{
        socket.close();
        ois.close();
        oos.close();
        System.out.println("Socket fechado");
        return false;
    }
    
    public static Map<String,String> getSplitPaths(Map<String,String> paths, String split){
        
        Map<String, String> tempPaths = new HashMap<String, String>();
        
        for(Map.Entry<String, String> entry : paths.entrySet()) {
            
            String fullPath = entry.getKey();
            String array[] = fullPath.split(split);
            tempPaths.put(array[1],entry.getValue());

        }
        
        return tempPaths;
    }
    
    public static void uploadFile(Request request) throws FileNotFoundException, IOException{
    
        if(request.isDirectory() && !request.getNome().equals("pasta sem nome")){
            new File(dir+usuario.getId()+"/"+request.getNome()).mkdirs();
        } else if (!request.getNome().equals("pasta sem nome")) {
            FileOutputStream fos = new FileOutputStream(dir+usuario.getId()+"/"+request.getNome());
            File file = new File(dir+usuario.getId()+"/"+request.getNome());
            file.setLastModified(request.getLastModified());
            fos.write(request.getConteudo());
            fos.close();
        }
        
        
    }
    
    public static void enviarMsgReply(Socket socket) throws IOException{
        Reply reply = new Reply();
        
        reply.setObs("Arquivo sincronizado!");
        
        sendObject(reply, socket, oos);
    }
    
    public static void downloadFile(Socket socket, Request request) throws IOException{
        
        File file = new File(dir+usuario.getId()+"/"+request.getNome());

        System.out.println(file.getAbsolutePath());
        System.out.println(file.getName());
        
        Reply reply = new Reply();

        if(file.isDirectory()) { 
            reply.setDirectory(true);
            reply.setNome(file.getName());
            reply.setObs("Diretorio enviado");
        } else { 
            byte[] bFile = fileToByteArray(file);
            reply.setNome(request.getNome());
            reply.setDirectory(false); 
            reply.setConteudo( bFile );
            reply.setLastModified(file.lastModified());
            reply.setObs("Arquivo enviado");
        }
        
        sendObject(reply, socket, oos);
        
    }
    
}
