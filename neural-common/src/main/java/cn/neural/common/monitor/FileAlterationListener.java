package cn.neural.common.monitor;

import java.io.File;

/**
 * A listener that receives events of file system modifications.
 * <p>
 * Register {@link FileAlterationListener}s with a {@link FileAlterationObserver}.
 *
 * @author lry
 */
public interface FileAlterationListener {

    /**
     * File system observer started checking event.
     *
     * @param observer The file system observer
     */
    default void onStart(final FileAlterationObserver observer) {

    }

    /**
     * Directory created Event.
     *
     * @param directory The directory created
     */
    default void onDirectoryCreate(final File directory) {

    }

    /**
     * Directory changed Event.
     *
     * @param directory The directory changed
     */
    default void onDirectoryChange(final File directory) {

    }

    /**
     * Directory deleted Event.
     *
     * @param directory The directory deleted
     */
    default void onDirectoryDelete(final File directory) {

    }

    /**
     * File created Event.
     *
     * @param file The file created
     */
    default void onFileCreate(final File file) {

    }

    /**
     * File changed Event.
     *
     * @param file The file changed
     */
    default void onFileChange(final File file) {

    }

    /**
     * File deleted Event.
     *
     * @param file The file deleted
     */
    default void onFileDelete(final File file) {

    }

    /**
     * File system observer finished checking event.
     *
     * @param observer The file system observer
     */
    default void onStop(final FileAlterationObserver observer) {

    }

}
