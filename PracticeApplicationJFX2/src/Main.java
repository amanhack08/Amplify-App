/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.stage.Stage;
import Client.LoginPage;

/**
 *
 * @author Asus
 */
public class Main extends Application{
    public static void main(String []args){
        
    }
     @Override
    public void start(Stage primaryStage) {
       
        //To change body of generated methods, choose Tools | Templates.
         try {
            Socket s = new Socket("localhost", 5000);
            new LoginPage(s).setVisible(true);
            
        } catch (IOException ex) {
            Logger.getLogger(LoginPage.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
