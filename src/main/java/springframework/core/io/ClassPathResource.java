package springframework.core.io;

import java.io.IOException;
import java.io.InputStream;

public class ClassPathResource implements Resource{

    private final String path;

    public ClassPathResource(String path) {
        // 委托给另一个构造器
        this.path = path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream(path);
        if (is == null) {
            throw new IOException(path + " cannot be opened");
        }
        return is;
    }
}
