package cn.neural.common;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * The File Lock
 *
 * @author lry
 */
@Slf4j
public class FileLock {

    private File file;

    private FileChannel channel = null;
    private java.nio.channels.FileLock lock = null;

    public FileLock(String filename) {
        this(new File(filename));
    }

    public FileLock(File file) {
        this.file = file;
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("create file[" + file.getAbsolutePath() + "] failed!", e);
            }
        }
    }

    /**
     * The try lock
     *
     * @return true success
     */
    public boolean tryLock() {
        boolean success = false;
        try {
            Path path = Paths.get(file.getPath());
            if (channel != null && channel.isOpen()) {
                return false;
            }
            channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.READ);
            lock = channel.tryLock();
            if (lock != null) {
                success = true;
                return true;
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (!success) {
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        return false;
    }

    /**
     * The release
     */
    public void release() {
        try {
            if (lock != null) {
                lock.release();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    log.error("file channel close failed.", e);
                }
            }
        }
    }

}
