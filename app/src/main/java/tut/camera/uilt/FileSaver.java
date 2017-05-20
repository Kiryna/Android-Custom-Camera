package tut.camera.uilt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileSaver {
    public String saveFileToDisk(File dir, byte[] data) {
        String photoPath = "photo_" + System.currentTimeMillis() + ".jpg";
        if (dir == null || data == null) {
            return null;
        }
        File takenPhoto = new File(dir, photoPath);

        try {
            FileOutputStream fos = new FileOutputStream(takenPhoto);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return photoPath;
    }
}
