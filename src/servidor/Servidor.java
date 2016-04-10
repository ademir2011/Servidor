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
import java.util.Date;
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
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        
        usuarioDAO = new UsuarioDAO();
        usuario = new Usuario();
       
        try {
                
                ServerSocket srvSocket = new ServerSocket(PORTA);
                
                while(true){
                    
                    System.out.println("... Esperando ...");
                    Socket socket = srvSocket.accept();
                    
                    new Thread( new Runnable() {
                        @Override
                        public void run() {
                            
                            Socket socketConexao = socket;
                            
                            ObjectInputStream ois = null;
                            
                            try {
                                ois = new ObjectInputStream( socketConexao.getInputStream() );
                            } catch (IOException ex) {
                                Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                            System.out.println("recebido");

                            Request request = null;
                            
                            try {
                                request = (Request) ois.readObject();
                            } catch (IOException ex) {
                                Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (ClassNotFoundException ex) {
                                Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                            System.out.println("Objeto recebido - ");
                            
                            usuario.setLogin(request.getLogin());
                            usuario.setSenha(request.getSenha());

                            if(request.getOperacao().equals("registrar")){

                                try {
                                    if(registraUsuario(socketConexao)){
                                        createDirectory();
                                    } else {
                                        System.out.println("Falha ao criar diretorio");
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                                }

                            } else if (request.getOperacao().equals("logar")) {
                                
                                try {
                                    if(logarUsuario(socketConexao)){
                                        
                                    } else {
                                        socketConexao.close();
                                    }
                                } catch (IOException ex) {
                                    Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                            }
                            
                            try {
                                socketConexao.close();
                                System.out.println("Conexao fechada");
                            } catch (IOException ex) {
                                Logger.getLogger(Servidor.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        }
                    } ).start();
                    
                    
//                    
//                    FileOutputStream fos = new FileOutputStream(dir+request.getNome());
//                    fos.write(request.getConteudo());
//                    fos.close();
//                    
//                    File file = new File("/home/ademir/OneBox/teste.txt");
//                    
//                    byte[] bFile = fileToByteArray(file);
//                    
//                    request = new Request(file.getName(),bFile,"/home/ademir/OneBox/",new Date());
//                    ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
//                    oos.writeObject(request);
                    
                }
                
               
                
        } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
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
    
    public static boolean registraUsuario(Socket socket) throws IOException{
        
        if(usuarioDAO.checkRegister(usuario)){
            usuarioDAO.addUsuario(usuario);
           
            sendObject( new Reply("Registrado com sucesso!"), socket);
            
            return true;

        } else {
            
            sendObject( new Reply("Ja existe dados com esse login e senha!"), socket);

            return false;
        }
        
    }
    
    public static void createDirectory(){
        
        long id = usuarioDAO.getIdByUser(usuario);
        new File(dir+id).mkdirs();
        System.out.println("Diretorio criado com sucesso");
        
    }
    
    public static boolean logarUsuario(Socket socket) throws IOException{
        
        if(usuarioDAO.checkLogin(usuario)){
            
            Reply reply = new Reply("Autenticado !");
            
            long id = usuarioDAO.getIdByUser(usuario);
            
            reply.setUserId(id);
            reply.setLogin(usuario.getLogin());
            reply.setSenha(usuario.getSenha());
            
            sendObject( reply, socket);
            
            return true;
            
        } else {
            sendObject( new Reply("Login ou senha errado !"), socket);
            return false;
        }
        
    }
    
    public static void sendObject(Reply replyRegistro, Socket socket) throws IOException{
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(replyRegistro);
    }
    
}
