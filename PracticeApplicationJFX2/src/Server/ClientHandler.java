/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Asus
 */
public class ClientHandler extends Thread {
    
    DataInputStream dis;
    DataOutputStream dos;
    Socket s;
    private Connection connect;
    private Statement ststement;
    private PreparedStatement preparedstatement;
    private ResultSet result;
    private String s_userID="";
    // Constructor
    public ClientHandler(Socket s) 
    {
        this.connect = null;
        this.ststement = null;
        this.preparedstatement = null;
        this.result = null;
        this.s = s;
        try {
            this.dis = new DataInputStream(s.getInputStream());
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            this.dos = new DataOutputStream(s.getOutputStream());
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run()
    {
        String received;
        String toreturn;
        
        while(true){
            try {


                
                // receive the answer from client
                received = dis.readUTF();

                if(received.equals("CLOSE_CONNECTION"))
                {
                    System.out.println("Client " + this.s + " sends exit...");
                    System.out.println("Closing this connection.");
                    this.s.close();
                    System.out.println("Connection closed");
                    //close connection with database
                    break;
                }
                else 
                {
                    if(received.equals("LOGIN_REQ")){
                        dos.writeUTF("SEND_USERID");
                        s_userID = dis.readUTF();
                        dos.writeUTF("SEND_PASSWORD");
                        String s_password= dis.readUTF();
                        if(logincheck(s_password))
                            dos.writeUTF("VALID");
                        else dos.writeUTF("INVALID");
                    }
                    else if(received.equals("REQUEST_INFO"))
                    {
                        
                    }
                    else if(received.equals("MOST_PLAYED_SONG")){
                        dos.writeUTF(""+most_played_song());
                    }
                    else if(received.equals("RECENTLY_PLAYED_SONG")){
                        dos.writeUTF(""+recently_played_song());
                    }
                    else if(received.equals("PLAYLIST_CREATE")){
                        dos.writeUTF("SEND_SONGSID");
                        String songlist = "";
                        String song = dis.readUTF();
                        while(!song.equals("CREATE")){
                            songlist+=song+"#";
                            song=dis.readUTF();
                        }
                        dos.writeUTF("SEND_PLAYLIST_NAME");
                        String playlistname=dis.readUTF();
                        dos.writeUTF("SEND_PLAYLIST_DESCRIPTION");
                        String playlistdescription=dis.readUTF();
                        if(songlist.equals("#"))
                            dos.writeUTF("EMPTY_PLAYLIST");
                        else {
                            create_Playlist(songlist,playlistname,playlistdescription);
                            dos.writeUTF("PLAYLIST_SUCESSFULLY_CREATED");
                        }
                    }
                    else if(received.equals("SONG_HISTORY")){
                        dos.writeUTF(""+song_History());
                        // returns a string 
                        // we have to tokenize the string at client side   
                    }
                    else if(received.equals("SONG_LIKE")){
                        dos.writeUTF("SEND_SONGID");
                        like_song(dis.readUTF());
                    }
                    else if(received.equals("SONG_DISLIKE")){
                        dos.writeUTF("SEND_SONGID");
                        dislike_song(dis.readUTF());
                    }
                    else if(received.equals("REFRESH")){
                        refresh();
                    }
                    else if(received.equals("SEARCH")){
                        //
                    }
                    else if(received.equals("CREATE_GROUP")){
                        dos.writeUTF("SEND_GROUPNAME_GROUPDESCRIPTION_PLAYLISTID_PASSKEY");
                        String input = dis.readUTF();
                        String[] group_info= input.split("#");
                        create_group(group_info[0],group_info[1],group_info[2],group_info[3]);
                        dos.writeUTF("GROUP_CREATED");
                    }
                    else if(received.equals("ADD_GROUP_MEMBERS")){
                        dos.writeUTF("SEND_GROUPNAME_GROUPDESCRIPTION_PLAYLISTID_PASSKEY");
                        String input = dis.readUTF();
                        String[] group_info= input.split("#");
                        dos.writeUTF("SEND_GROUP_MEMBERS");
                        String[] group_members = input.split("#");
                        if(add_members_in_group(group_members,group_info[0],group_info[1],group_info[2],group_info[3]).equals("VALID"))
                        dos.writeUTF("GROUP_MEMBERS_ADDED");
                        else dos.writeUTF("FAILURE");
                    }
                    else if(received.equals("PLAYLIST_ADD_SONG")){
                        dos.writeUTF("SEND_PLAYLISTID");
                        String playlistid=dis.readUTF();
                        dos.writeUTF("SEND_SONGID");
                        String songs=dis.readUTF();
                        String[] songid=songs.split("#");
                        playlist_add_song(playlistid,songid);
                        
                    }
                    else if(received.equals("SHARE_PLAYLIST")){
                        dos.writeUTF("SEND_PLAYLISTID");
                        int playlistid=Integer.parseInt(dis.readUTF());
                        dos.writeUTF("SEND_USERID");
                         String users=dis.readUTF();
                        String[] userid=users.split("#");
                        if(share_with(userid,playlistid).equals("VALID"))
                            dos.writeUTF("SUCCESFULL");
                        else dos.writeUTF("FAILURE");
                        
                    }
                    
                }  


                        

                /*  default:
                        dos.writeUTF("Invalid input");
                        break;
                }*/
                
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try
        {
            // closing resources
            this.dis.close();
            this.dos.close();

        }catch(IOException e){
            e.printStackTrace();
        }
    }
    private boolean logincheck(String password){
        //database query
        String query="SELECT userid FROM user WHERE userid=? AND password=?;";
        
        try{
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            connect = DriverManager
                    .getConnection("jdbc:mysql://localhost/amplify?"
                            + "user=user&password=user");

            // Statements allow to issue SQL queries to the database
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setString(1,s_userID);
            preparedstatement.setString(2, password);
            result=preparedstatement.executeQuery();
            return result != null;
        }
        catch(ClassNotFoundException | SQLException e){
            e.printStackTrace();
            return false;
        }

        //if(result==null)
        //return false;
        //else return true;
        
    }

    private String most_played_song() {
        try {
            //database query
            String query="SELECT songid,playcount FROM songplayed WHERE userid=?;";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setString(1,s_userID);
            result=preparedstatement.executeQuery();
            
            int max_count=0;int most_played_song_id=0;
            while(result.next()){
                if(result.getInt("playcount")>max_count){
                    max_count=result.getInt("playcount");
                    most_played_song_id=result.getInt("songid");
                }
            }
            if(max_count==0)
                return "NO_SONG_PLAYED";
            else 
            {
                String query2="SELECT songname from song WHERE songid=?";
                preparedstatement = connect.prepareStatement(query2);
                preparedstatement.setInt(1,most_played_song_id);
                result=preparedstatement.executeQuery();
                return result.getString("songname");
            }
            
            //if(result==null)
            // return "No Song Exists";
            // else return most played song
        } catch (SQLException ex) {
            
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            return "NO_SONG_PLAYED";
        }
    }
    
    private String recently_played_song() {
        try {
            //database query
            //if(result==null)
            //return "No Song Exists";
            
            String query="SELECT songid FROM songplayed WHERE userid=? ORDER BY time DESC;";
        
                preparedstatement=connect.prepareStatement(query);
            
            preparedstatement.setString(1,s_userID);
            result=preparedstatement.executeQuery();
            if(result==null)
                return "NO_SONG_PLAYED";
            else{
                query="SELECT songname FROM song WHER songid=?;";
                preparedstatement=connect.prepareStatement(query);
                preparedstatement.setString(1,result.getString("songid"));
                result=preparedstatement.executeQuery();
                return result.getString("songname");
            }
                    
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "NO_SONG_PLAYED";
         
    }
    
    private void create_Playlist(String songlist,String playlistname,String playlistdescription) {
        try {
            //database query
            //create a playlist using String tokenizers
            StringTokenizer st= new StringTokenizer(songlist,"#");
            int pid=1,count=1;
            //count is for setting priority of each song in a playlist
                String query="SELECT playlistid FROM playlist ORDER BY playlistid DESC;";
                preparedstatement = connect.prepareStatement(query);
                result=preparedstatement.executeQuery();
                if(result.next()){
                    pid=result.getInt("playlistid")+1;
                }
                
            while(st.hasMoreTokens()){
                // incomplete study relationship table again
                query="INSERT INTO playlist (playlistid,playlistname,,userid,playlistdescription,songid,priority) VALUES (?,?,?,?,?,?);";
                preparedstatement = connect.prepareStatement(query);
                preparedstatement.setInt(1,pid);
                preparedstatement.setString(2,playlistname);
                preparedstatement.setString(3,s_userID);
                preparedstatement.setString(4,playlistdescription);
                preparedstatement.setInt(5,Integer.parseInt(st.nextToken()));
                preparedstatement.setInt(6,count);
                count++;
                preparedstatement.executeQuery();
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    private String song_History() {
         
        try {
            String query="SELECT songid FROM song WHERE userid=? ORDER BY time DESC;";
            preparedstatement=connect.prepareStatement(query);
            preparedstatement.setString(1,"s_userid");
            result=preparedstatement.executeQuery();
            if(result==null)
                return "NO_HISTORY_AVAILABLE";
            String songhistory="";
            while(result.next()){
                songhistory+=result.getString("songname")+"#";
                songhistory+=result.getString("time")+"#";
                songhistory+=result.getString("plsycount")+"#";
            }
            // study JSON 
            // implement using JSON
            return songhistory;
            
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "NO_HISTORY_AVAILABLE";
        
    }
    
    private void like_song(String songid) {
        
        try {
            //database query
            //3 database queries 
            //2 table updation
            String query="SELECT like from song WHERE songid=?;";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setString(1,songid);
            result = preparedstatement.executeQuery();
            
            //updating song table
            int initial_likes = result.getInt("like");
            query="UPDATE song SET like =? WHERE songid=?;";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setInt(1,initial_likes+1);
            preparedstatement.setString(2,songid);
            preparedstatement.executeQuery();
            
            //updating songfavourite table
            query="UPDATE songfavourite SET like =? WHERE songid=? AND userid=?;";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setInt(1,initial_likes+1);
            preparedstatement.setString(2,songid);
            preparedstatement.setString(3,s_userID);
            preparedstatement.executeQuery();
    
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void dislike_song(String songid) {    
                
        try {
            //database query
            //3 database queries 
            //2 table updation
            String query="SELECT dislike from song WHERE songid=?;";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setString(1,songid);
            result = preparedstatement.executeQuery();
            
            //updating song table
            int initial_dislikes = result.getInt("dislike");
            query="UPDATE song SET dislike =? WHERE songid=?;";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setInt(1,initial_dislikes+1);
            preparedstatement.setString(2,songid);
            preparedstatement.executeQuery();
            
            //updating songfavourite table
            query="UPDATE songfavourite SET dislike =? WHERE songid=? AND userid=?;";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setInt(1,initial_dislikes+1);
            preparedstatement.setString(2,songid);
            preparedstatement.setString(3,s_userID);
            preparedstatement.executeQuery();
    
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void refresh(){
        //refresh and show new uploaded songs
        //call analyze() algorithm
    } 
    
    private void create_group(String groupname,String groupdescription,String playlistid,String passkey){
        try {
            // function to create a group
            // group memebers are not added in this function
            int gid=1;
            String query="SELECT groupid FROM group ORDER BY groupid DESC;";
            preparedstatement = connect.prepareStatement(query);
            result=preparedstatement.executeQuery();
            if(result.next()){
                gid=result.getInt("playlistid")+1;
            }
            query="INSERT INTO groups (groupid,groupname,groupdescription,playlistid,passkey) VALLUES (?,?,?,?,?);";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setInt(1,gid);
            preparedstatement.setString(2,groupname);
            if(groupdescription.equals("-1"))
            preparedstatement.setString(3,"");
            else preparedstatement.setString(3,groupdescription);
            preparedstatement.setInt(4,Integer.parseInt(playlistid));
            preparedstatement.setString(5,passkey);
            preparedstatement.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private String add_members_in_group(String[] userid,String groupname,String groupdescription,String playlistid,String passkey){
        try {
            //function to add group members in a group
            // a group can have even 0 members
            if(groupdescription.equals("-1"))
                groupdescription="";
            String query="SELECT groupid FROM group WHERE groupname=? AND groupdescription=? AND playlistid=? AND passkey=?;";
            preparedstatement = connect.prepareStatement(query);
            preparedstatement.setString(1,groupname);
            preparedstatement.setString(2,groupdescription);
            preparedstatement.setString(3,playlistid);
            preparedstatement.setString(4,passkey);
            result=preparedstatement.executeQuery();
            if(!result.next())
                return "INVALID_DATA";
            int gid= result.getInt("groupid");
            for (String each_userid : userid) {
                query="INSERT INTO groupmembers (groupid,userid) VALUES (?,?);";
                preparedstatement = connect.prepareStatement(query);
                preparedstatement.setInt(1,gid);
                preparedstatement.setString(2, each_userid);
                preparedstatement.executeQuery();
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "VALID";      
    }
    private String share_with(String[] userid,int playlistid){
        String query="";
        for (String userid1 : userid) {
            try {
                query="INSERT INTO shareplaylist (playlistid,sharedfrom,sharedto) VALUES(?,?,?);";
                preparedstatement=connect.prepareStatement(query);
                preparedstatement.setInt(1,playlistid);
                preparedstatement.setString(2,s_userID);
                preparedstatement.setString(3, userid1);
                preparedstatement.executeQuery();
            }catch (SQLException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
                return "INVALID";
            }
        }
        return "VALID";
    }
    private String playlist_add_song(String playlistid,String[] songid){
        
        if(hasAccess(playlistid,s_userID)){
            String query="";
            try {
                query="SELECT * FROM playlistid WHERE playlistid=? ORDER BY priority DESC;";
                preparedstatement=connect.prepareStatement(query);
                preparedstatement.setInt(1,Integer.parseInt(playlistid));
                result=preparedstatement.executeQuery();
                int count=result.getInt("priority");
                count++;
                String playlistname=result.getString("playlistname");
                String playlistdescription=result.getString("playlistdescription");
                
                for(int i=0;i<songid.length;i++){
                    
                    
                    query="INSERT INTO playlist (playlistid,playlistname,playlistdescription,songid,priority) VALUES (?,?,?,?,?);";
                    preparedstatement=connect.prepareStatement(query);
                    preparedstatement.setInt(1,Integer.parseInt(playlistid));
                    preparedstatement.setString(2,playlistname);
                    preparedstatement.setString(3,playlistdescription);
                    preparedstatement.setInt(4,Integer.parseInt(songid[i]));
                    preparedstatement.setInt(5,count+i);
                    preparedstatement.executeQuery();
                }
                return "VALID";
            } catch (SQLException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return "INVALID";
    }

    private boolean hasAccess(String playlistid, String s_userID1) {
        //method to check if a user has access to a particular playlist or not
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

