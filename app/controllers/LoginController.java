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
@Singleton
public class LoginController extends Controller {
    private Database db;
    private String indexText = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"" +
        "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" +
        "<!--\n" +
        "Design by TEMPLATED\n" +
        "http://templated.co\n" +
        "Released for free under the Creative Commons Attribution License\n" +
        "Name       : Black / White\n" +
        "Description: A two-column, fixed-width design with dark color scheme.\n" +
        "Version    : 1.0\n" +
        "Released   : 20111121\n" +
        "-->\n" +
        "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
        "<head>\n" +
        "<meta name=\"keywords\" content=\"\" />\n" +
        "<meta name=\"description\" content=\"\" />\n" +
        "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />\n" +
        "<title>Homepage</title>\n" +
        "<link href='http://fonts.googleapis.com/css?family=Nova+Mono' rel='stylesheet' type='text/css' />\n" +
        "<link href=\"/stylesheets/style.css\" rel=\"stylesheet\" type=\"text/css\" media=\"screen\" />\n" +
        "</head>\n" +
        "<body>\n" +
        "<div id=\"wrapper\">\n" +
        "	<div id=\"header-wrapper\">\n" +
        "		<div id=\"header\">\n" +
        "			<div id=\"logo\">\n" +
        "				<h1><a href=\"#\">Black/White </a></h1>\n" +
        "				<p>Design by <a href=\"http://templated.co\" rel=\"nofollow\">TEMPLATED</a></p>\n" +
        "			</div>\n" +
        "		</div>\n" +
        "	</div>\n" +
        "	<!-- end #header -->\n" +
        "	<div id=\"menu\">\n" +
        "		<ul>\n" +
        "			<li class=\"current_page_item\"><a href=\"#\">Home</a></li>\n" +
        "			<li><a href=\"/select\">Simple Test</a></li>\n" +
        "     <li><a href=\"/insert\">Insert Test</a></li>\n" +
        "     <li><a href=\"/logout\">Logout</a></li>\n" +
        "		</ul>\n" +
        "	</div>\n" +
        "	<!-- end #menu -->\n" +
        "	<div id=\"page\">\n" +
        "		<div id=\"page-bgtop\">\n" +
        "      <div id=\"content\">\n" +
        "        <p> Welcome to our application for viewing IMDB data. This template comes from TEMPLATED. </p>\n" +
        "				<div style=\"clear: both;\">&nbsp;</div>\n" +
        "      </div>\n" +
        "      <div id=\"page-bgbtm\">\n" +
        "			</div>\n" +
        "	<!-- end page -->\n" +
        "</div>\n" +
        "<div id=\"footer\">\n" +
        "	<p>&copy; Untitled. All rights reserved. Design by <a href=\"http://templated.co\" rel=\"nofollow\">TEMPLATED</a>.</p>\n" +
        "</div>\n" +
        "<!-- end #footer -->\n" +
        "</body>\n" +
        "</html>";
    @Inject
    public LoginController(Database db) {
        this.db = db;
    }
    
    public Result login(){
        DynamicForm form = Form.form().bindFromRequest();
        Connection conn = db.getConnection();
        boolean connected = false;
       
        try{
            String query = "SELECT * FROM user WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, form.get("username"));
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
            	  String saltedPass = rs.getString("password");	
         //       String saltedPass = form.get("password") + rs.getString("password");
         //       String hashedPass = hashPassword(saltedPass);
                if(saltedPass == null)
                    return internalServerError("Error hashing password");
                if(rs.getString("password").equals(form.get("password"))){
                    session("role", rs.getString("role"));
                
                if (rs.getString("cid")!=null)
                    session("cid", rs.getString("cid"));
                        connected = true;
                    
                }
                else{
                    String result = "Database pw: " + rs.getString("password") +
                        "\npw: " + saltedPass;
                    return badRequest("No account with that username/password found.");
                }
            }
        }catch(SQLException e){
            System.err.println(e);
        }
        if(!connected)
            return badRequest("No account with that username/password found.");
        if (session("role").equals("C"))
       
        return redirect("/invoiceselect");
        return redirect("/select");
    }
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
    public Result logout(){
        session().clear();
        return ok("You are logged out.");
    }
}