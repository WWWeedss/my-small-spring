package springframework.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class UrlResource implements Resource{
    private final URL url;

    public UrlResource(URL url) {
        assert url != null : "URL must not be null";
        this.url = url;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        URLConnection con = this.url.openConnection();
        try {
            return con.getInputStream();
        }
        catch (IOException ex) {
            // Close the HTTP connection (if applicable).
            if (con instanceof java.net.HttpURLConnection) {
                ((java.net.HttpURLConnection) con).disconnect();
            }
            throw ex;
        }
    }
}
