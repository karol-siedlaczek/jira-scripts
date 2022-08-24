import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.*
  
def client = new HttpClient();
def method = new GetMethod("https://example.com")
def credentials = new UsernamePasswordCredentials('username', 'password')
client.getParams().setAuthenticationPreemptive(true)
client.getState().setCredentials(AuthScope.ANY, credentials)
client.executeMethod(method)
method.releaseConnection()
return method.getResponseBodyAsString()
