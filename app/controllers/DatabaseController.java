package controllers;
import play.mvc.*;
import play.db.*;
import java.sql.*;
import javax.sql.DataSource;
import javax.inject.*;
import play.*;
import play.data.Form;
import play.data.DynamicForm;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.util.Random;
@Singleton
public class DatabaseController extends Controller {
    private Database db;
    private String role;
    
    private String cid;
    
    private String responseString1 = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>" +
                "<title> Database Select </title>\n" +
                "<link href=\"/stylesheets/style.css\" rel=\"stylesheet\" type=\"text/css\" media=\"screen\" />\n" +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"wrapper\">\n" +
                "<div id=\"header-wrapper\">\n" +
                "<div id=\"header\">\n" +
                "<div id=\"logo\">\n" +
                "<h1><a href=\"#\">Black/White </a></h1>\n" +
                "<p>Design by <a href=\"http://templated.co\" rel=\"nofollow\">TEMPLATED</a></p>\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "<div id=\"menu\">\n" +
        "<ul>\n";
        String responseString2 = "</div>\n" +
                "<div id=\"page\">\n" +
                "<div id=\"page-bgtop\">\n" +
                "<div id=\"page-bgbtm\">\n" +
        "<div id=\"content\">\n";
    private String responseString3 = "</div>\n" +
                "<div style=\"clear: both;\">&nbsp;</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "</div>\n" +
                "<div id=\"footer\">\n" +
                "<p>&copy; Untitled. All rights reserved. Design by <a href=\"http://templated.co\" rel=\"nofollow\">TEMPLATED</a>.</p>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>\n";
    @Inject
    public DatabaseController(Database db) {
        this.db = db;
    }
    public Result submitInsert(){
        int mid = 0;
        DynamicForm form = Form.form().bindFromRequest();
        Connection conn = db.getConnection();
        try{
            conn.setAutoCommit(false);
        }catch(SQLException e){
            System.err.println(e);
            return internalServerError(e.toString());
        }
        role = session("role");
        if(role == null || !role.equals("M"))
            return unauthorized("Your account does not have this privelege or you are not signed in.");
        Savepoint save1 = null;
        try{
            save1 = conn.setSavepoint();
            String insertManagerQuery = "INSERT INTO manager" +
                "(fname, lname) VALUES " +
                "(?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertManagerQuery);
            insertStmt.setString(1, form.get("firstname"));
            insertStmt.setString(2, form.get("lastname"));
            insertStmt.executeUpdate();
            String getMIDQuery = "SELECT mid FROM manager " +
                "WHERE fname = ? AND lname = ?";
            PreparedStatement midStmt = conn.prepareStatement(getMIDQuery);
            midStmt.setString(1, form.get("firstname"));
            midStmt.setString(2, form.get("lastname"));
            ResultSet rs = midStmt.executeQuery();
            String salt = generateSalt();
            String username = form.get("username");
            String password = form.get("password");
            while(rs.next()){
                mid = rs.getInt("mid");
                createUserFromManager(mid, username, password, salt);
            }
        }catch(SQLException e){
            System.err.println(e);
            try{
                conn.rollback(save1);
                conn.commit();
                conn.setAutoCommit(true);
            }catch(SQLException f){
                System.err.println(f);
                return internalServerError(f.toString());
            }
            return internalServerError(e.toString());
        }
        try{
            conn.commit();
            conn.setAutoCommit(true);
        }catch(SQLException e){
            System.err.println(e);
            return internalServerError(e.toString());
        }
        return ok("Data received. mid: " + mid);
    }
    //Take mid, see if a user already has it. If not, create a user with specified username and password
    public void createUserFromManager(int mid, String username, String password, String salt){
        try{
            Connection conn = db.getConnection();
            String selectUserQuery = "SELECT mid FROM user WHERE mid = ?";
            PreparedStatement selectUserStmt = conn.prepareStatement(selectUserQuery);
            selectUserStmt.setInt(1, mid);
            ResultSet users = selectUserStmt.executeQuery();
            //If the mid corresponds to a current user, move on
            if(users.isBeforeFirst()){
                System.out.println("User with mid found");
                return;
            }
            String insertUserQuery = "INSERT INTO user" +
                "(username, password, mid, salt, original_password)" +
                "VALUES(?,?,?,?,?)";
            PreparedStatement insertUserStmt = conn.prepareStatement(insertUserQuery);
            insertUserStmt.setString(1, username);
            insertUserStmt.setString(2, hashPassword(password + salt));
            insertUserStmt.setInt(3, mid);
            insertUserStmt.setString(4, salt);
            insertUserStmt.setString(5, password);
            insertUserStmt.executeUpdate();
        }catch(SQLException e){
            System.err.println(e);
            return;
        }
    }
    //Hash salted password with SHA-256 encoding
    public String hashPassword(String saltedPass){
        MessageDigest digest = null;
        try{
            digest = MessageDigest.getInstance("SHA-256");
            digest.update(saltedPass.getBytes("UTF-8"));
        }catch(NoSuchAlgorithmException e){
            System.err.println(e);
            return null;
        }catch(UnsupportedEncodingException e){
            System.err.println(e);
            return null;
        }
        byte[] data = digest.digest();
        String hashedPass = DatatypeConverter.printHexBinary(data);
        return hashedPass.toLowerCase();
    }
    //Generate random string with length between 6 and 12
    public String generateSalt(){
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        int saltLength = rand.nextInt(7) + 6;
        for(int i = 0; i < saltLength; i++){
            sb.append((char)(rand.nextInt(95) + 32));
        }
        return sb.toString();
    }
    public Result select(){
        if(session("role") == null || (!session("role").equals("M") && !session("role").equals("C")))
            return unauthorized("Your account does not have this privelege or you are not signed in.");
        DynamicForm form = Form.form().bindFromRequest();
        Connection conn = db.getConnection();
        String newResponseString2 = "<li><a href=\"/\">Home</a></li>\n" +
            "<li class = \"current_page_item\"><a href=\"/select\">Simple Test</a></li>\n" +
            "<li><a href = \"/insert\"> Insert Test</a></li>\n" +
            "</ul>\n" + responseString2;
        String outString = "Manager with first name of " + form.get("firstname") + ": ";
            try {
                String query = "";
                PreparedStatement stmt;
                if(form.get("lastname") == null || form.get("lastname").equals("")){
                    query = "SELECT `fname`, `lname` FROM manager " +
                        "WHERE fname = ?";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, form.get("firstname"));
                }
                else{
                    query = "SELECT `fname`, `lname` FROM manager " +
                        "WHERE fname = ? AND lname = ?";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, form.get("firstname"));
                    stmt.setString(2, form.get("lastname"));
                }
                PreparedStatement selectStmt = conn.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    outString += "<br>\nfirst_name: " + rs.getString("fname") + "<br>\n";
                    outString += "last_name: " + rs.getString("lname") + "<br>\n";
                }
            } catch(SQLException e){
                System.err.println(e);
            }
            return ok(responseString1 + newResponseString2 + outString + responseString3).as("text/html");
    }
    public Result selectInvoice(){
    if(session("role") == null || (!session("role").equals("M") && !session("role").equals("C")))
        return unauthorized("Your account does not have this privelege or you are not signed in.");
    DynamicForm form = Form.form().bindFromRequest();
    Connection conn = db.getConnection();
    String newResponseString2 = "<li><a href=\"/\">Home</a></li>\n" +
        "<li class = \"current_page_item\"><a href=\"/select\">Simple Test</a></li>\n" +
        "<li><a href = \"/insert\"> Insert Test</a></li>\n" +
        "</ul>\n" + responseString2;
    String outString = "Invoice with pid \"" + form.get("pid") + "\": ";
        try {
            String query = "";

        //    if(form.get("pid") == null || form.get("lastname").equals("")){
        //        query = "SELECT `fname`, `lname` FROM manager " +
        //            "WHERE fname = ?";
        //        stmt = conn.prepareStatement(query);
        //        stmt.setString(1, form.get("firstname"));
        //    }
        //    else{
                query = "SELECT * FROM invoice " +
                    "WHERE pid = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, form.get("pid"));
            
         //   }
         //   PreparedStatement selectStmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                outString += "<br>\namount: " + rs.getString("amount") + "<br>\n";
             
            }
        } catch(SQLException e){
            System.err.println(e);
        }
        return ok(responseString1 + newResponseString2 + outString + responseString3).as("text/html");
}

	public Result searchManagerName(){
		
		role= session("role");
		if (session("cid")!=null)
		cid= session("cid");
        String newResponseString2 = "<li><a href=\"/\">Home</a></li>\n" +
                "<li class = \"current_page_item\"><a href=\"/select\">Simple Test</a></li>\n" +
                "<li><a href = \"/insert\"> Insert Test</a></li>\n" +
                "</ul>\n" + responseString2;
		if (role==null || !role.equals("C")){
			return unauthorized("Customer Only Service.");
		}
		String outString= "Your manager is ";
		Connection conn = db.getConnection();
		 try{
	            conn.setAutoCommit(false);
	        }catch(SQLException e){
	            System.err.println(e);
	            return internalServerError(e.toString());
	        }
		

		
        try{      
		PreparedStatement stmt = conn.prepareStatement("Select fname,lname from manager m1, manages m2 where m1.mid=m2.mid AND m2.cid=?");
		stmt.setString(1,cid);
		ResultSet rs =stmt.executeQuery();
		while(rs.next()){
			   outString += "<br>\nfirst_name: " + rs.getString("fname") + "<br>\n";
               outString += "last_name: " + rs.getString("lname") + "<br>\n";
		}
		}catch (SQLException e){
		System.err.println(e);}
        return ok(responseString1 + newResponseString2 + outString + responseString3).as("text/html");
	}
	
	}
