/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package banco;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import servidor.HibernateUtil;

/**
 *
 * @author ademir
 */
public class UsuarioDAO {
    
    public void addUsuario(Usuario usuario){
        
        Transaction trns = null;    
        SessionFactory sf = HibernateUtil.getSessionFactory();
        Session session = sf.openSession();

         try {
             trns = session.beginTransaction();
             session.save(usuario);
             session.getTransaction().commit();
         } catch (RuntimeException e) {
             if (trns != null) {
                 trns.rollback();
             }
             e.printStackTrace();
         } finally {
             session.flush();
             session.close();
         }
        
    }
    
    public boolean checkRegister(Usuario usuario){
        
        List<Usuario> listUsuario = new ArrayList<Usuario>();
        Transaction trns = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            trns = session.beginTransaction();
            listUsuario = session.createQuery("from Usuario").list();
        
            for(Usuario key : listUsuario){
                if(key.getSenha().equals(usuario.getSenha()) || key.getLogin().equals(usuario.getLogin())){
                    return false;
                }
            }
            
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            session.flush();
            session.close();
        }
        
        return true;
    }
    
    public long getIdByUser(Usuario usuario){
        List<Usuario> listUsuario = new ArrayList<Usuario>();
        Transaction trns = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            trns = session.beginTransaction();
            listUsuario = session.createQuery("from Usuario").list();
        
            for(Usuario key : listUsuario){
                if(key.getSenha().equals(usuario.getSenha()) && key.getLogin().equals(usuario.getLogin())){
                    return key.getId();
                }
            }
            
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            session.flush();
            session.close();
        }
        
        return -1;
    
    }
    
    public boolean checkLogin(Usuario usuario){
        
        List<Usuario> listUsuario = new ArrayList<Usuario>();
        Transaction trns = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            trns = session.beginTransaction();
            listUsuario = session.createQuery("from Usuario").list();
        
            for(Usuario key : listUsuario){
                if(key.getSenha().equals(usuario.getSenha()) && key.getLogin().equals(usuario.getLogin())){
                    return true;
                }
            }
            
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally {
            session.flush();
            session.close();
        }
        return false;
    }
    
}
