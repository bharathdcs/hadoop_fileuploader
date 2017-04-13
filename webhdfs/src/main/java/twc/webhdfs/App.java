package twc.webhdfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
    	Properties prop = new Properties();
    	InputStream input = null;
    	input = new FileInputStream(args[0]);

    		// load a properties file
    	prop.load(input);
    	
    	 String knoxHostPort=prop.getProperty("knoxHostPort");
    	 String username=prop.getProperty("knoxUsername");
    	 String password=prop.getProperty("knoxPassword");
    	 String inputFile=prop.getProperty("dataFile");
    	 String hdfsFile=prop.getProperty("hdfsFileUrl");
    	 String hdfsFilePersmission=prop.getProperty("hdfsFilePermission", "777");
    	 String url="https://"+knoxHostPort+"/gateway/default/webhdfs/v1"+hdfsFile+"?op=CREATE&permission="+hdfsFilePersmission;
    	 SSLContext ctx = SSLContext.getInstance("TLS");
         X509TrustManager tm = new X509TrustManager() {
             public X509Certificate[] getAcceptedIssuers() {
                 return null;
             }

             public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
             }

             public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
             }
         };
         ctx.init(null, new TrustManager[] { (TrustManager) tm }, null);
         SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(ctx,
             SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
         Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory> create()
             .register("https", ssf).build();
         BasicCredentialsProvider basicProvider=new BasicCredentialsProvider();
         basicProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
				
         HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r);  
         HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(basicProvider).setRedirectStrategy(new DefaultRedirectStrategy() {                
        public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context)  {
            boolean isRedirect=false;
            try {
                isRedirect = super.isRedirected(request, response, context);
            } catch (ProtocolException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (!isRedirect) {
                int responseCode = response.getStatusLine().getStatusCode();
                if (responseCode == 301 || responseCode == 302) {
                    return true;
                }
            }
            return isRedirect;
        }
    }).setConnectionManager(cm).build();
       HttpPut request = new HttpPut(url);
     
        
    	HttpContext context = new BasicHttpContext(); 
    	
    	
    		HttpResponse response = client.execute(request, context); //Capture context for PUT
          
    	    String newUrl=response.getFirstHeader("Location").getValue();
    	    System.out.println(newUrl);
    	    File dataFile=new File(inputFile);
    	    if(dataFile.length()>5242880)
    	    {
    	    	System.out.println("File Size cannot be more than 5 MB");
    	    }
    	    FileEntity entity = new FileEntity(dataFile);
    	
    	    HttpPut newRequest = new HttpPut(newUrl);
    	    
    	
    	    newRequest.setEntity(entity);
    	    response = client.execute(newRequest, context);
    	 
    	    if(response.getStatusLine().getStatusCode()==201)
    	    	System.out.println("File creation successfull");
    	

    }
}
