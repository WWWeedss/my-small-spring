package springframework.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileSystemResource implements Resource{

    private final File file;
    public FileSystemResource(File file) {
        this.file = file;
    }

    public FileSystemResource(String path) {
        this.file = new File(path);
    }
    @Override
    public InputStream getInputStream() throws IOException {
        return new FileInputStream(this.file);
    }
}
