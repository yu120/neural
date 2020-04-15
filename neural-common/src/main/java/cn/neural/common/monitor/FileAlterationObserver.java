package cn.neural.common.monitor;

import lombok.Data;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * FileAlterationObserver represents the state of files below a root directory,
 * checking the filesystem and notifying listeners of create, change or delete events.
 *
 * @author lry
 */
public class FileAlterationObserver implements Serializable {

    private static final long serialVersionUID = 8633513440421797605L;

    private final List<FileAlterationListener> listeners = new CopyOnWriteArrayList<>();
    private final FileEntry rootEntry;
    private final FileFilter fileFilter;
    private Comparator<File> comparator;

    public FileAlterationObserver(String directoryName) {
        this(new File(directoryName));
    }

    public FileAlterationObserver(String directoryName, FileFilter fileFilter) {
        this(new File(directoryName), fileFilter);
    }

    public FileAlterationObserver(String directoryName, FileFilter fileFilter, Boolean caseSensitivity) {
        this(new File(directoryName), fileFilter, caseSensitivity);
    }

    public FileAlterationObserver(File directory) {
        this(directory, null);
    }

    public FileAlterationObserver(File directory, FileFilter fileFilter) {
        this(directory, fileFilter, null);
    }

    public FileAlterationObserver(File directory, FileFilter fileFilter, Boolean caseSensitivity) {
        this(new FileEntry(directory), fileFilter, caseSensitivity);
    }

    /**
     * Construct an observer for the specified directory, file filter and
     * file comparator.
     *
     * @param rootEntry       the root directory to observe
     * @param fileFilter      The file filter or null if none
     * @param caseSensitivity true: the constant for case sensitive regardless of operating system.
     *                        false: the constant for case insensitive regardless of operating system.
     *                        null: Windows is case-insensitive when comparing filenames, Unix is case-sensitive.
     */
    private FileAlterationObserver(FileEntry rootEntry, FileFilter fileFilter, Boolean caseSensitivity) {
        if (rootEntry == null) {
            throw new IllegalArgumentException("Root entry is missing");
        }
        if (rootEntry.getFile() == null) {
            throw new IllegalArgumentException("Root directory is missing");
        }
        this.rootEntry = rootEntry;
        this.fileFilter = fileFilter;
        if (caseSensitivity == null) {
            boolean windows = (File.separatorChar == '\\');
            this.comparator = (file1, file2) -> checkCompareTo(!windows, file1.getName(), file2.getName());
        } else {
            this.comparator = (file1, file2) -> checkCompareTo(caseSensitivity, file1.getName(), file2.getName());
        }
    }

    /**
     * Return the directory being observed.
     *
     * @return the directory being observed
     */
    public File getDirectory() {
        return rootEntry.getFile();
    }

    /**
     * Return the fileFilter.
     *
     * @return the fileFilter
     * @since 2.1
     */
    public FileFilter getFileFilter() {
        return fileFilter;
    }

    /**
     * Add a file system listener.
     *
     * @param listener The file system listener
     */
    public void addListener(FileAlterationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a file system listener.
     *
     * @param listener The file system listener
     */
    public void removeListener(FileAlterationListener listener) {
        if (listener != null) {
            while (listeners.remove(listener)) {
            }
        }
    }

    /**
     * Returns the set of registered file system listeners.
     *
     * @return The file system listeners
     */
    public Iterable<FileAlterationListener> getListeners() {
        return listeners;
    }

    /**
     * Initialize the observer.
     *
     * @throws Exception if an error occurs
     */
    public void initialize() throws Exception {
        rootEntry.refresh(rootEntry.getFile());
        final FileEntry[] children = doListFiles(rootEntry.getFile(), rootEntry);
        rootEntry.setChildren(children);
    }

    /**
     * Final processing.
     *
     * @throws Exception if an error occurs
     */
    public void destroy() throws Exception {
    }

    /**
     * Check whether the file and its children have been created, modified or deleted.
     */
    public void checkAndNotify() {

        /* fire onStart() */
        for (FileAlterationListener listener : listeners) {
            listener.onStart(this);
        }

        /* fire directory/file events */
        final File rootFile = rootEntry.getFile();
        if (rootFile.exists()) {
            checkAndNotify(rootEntry, rootEntry.getChildren(), listFiles(rootFile));
        } else if (rootEntry.isExists()) {
            checkAndNotify(rootEntry, rootEntry.getChildren(), new File[0]);
        } else {
            // Didn't exist and still doesn't
        }

        /* fire onStop() */
        for (FileAlterationListener listener : listeners) {
            listener.onStop(this);
        }
    }

    /**
     * Compare two file lists for files which have been created, modified or deleted.
     *
     * @param parent   The parent entry
     * @param previous The original list of files
     * @param files    The current list of files
     */
    private void checkAndNotify(FileEntry parent, FileEntry[] previous, File[] files) {
        int c = 0;
        FileEntry[] current = files.length > 0 ? new FileEntry[files.length] : FileEntry.EMPTY_ENTRIES;
        for (final FileEntry entry : previous) {
            while (c < files.length && comparator.compare(entry.getFile(), files[c]) > 0) {
                current[c] = createFileEntry(parent, files[c]);
                doCreate(current[c]);
                c++;
            }
            if (c < files.length && comparator.compare(entry.getFile(), files[c]) == 0) {
                doMatch(entry, files[c]);
                checkAndNotify(entry, entry.getChildren(), listFiles(files[c]));
                current[c] = entry;
                c++;
            } else {
                checkAndNotify(entry, entry.getChildren(), new File[0]);
                doDelete(entry);
            }
        }
        for (; c < files.length; c++) {
            current[c] = createFileEntry(parent, files[c]);
            doCreate(current[c]);
        }
        parent.setChildren(current);
    }

    /**
     * Create a new file entry for the specified file.
     *
     * @param parent The parent file entry
     * @param file   The file to create an entry for
     * @return A new file entry
     */
    private FileEntry createFileEntry(FileEntry parent, File file) {
        FileEntry entry = parent.newChildInstance(file);
        entry.refresh(file);
        FileEntry[] children = doListFiles(file, entry);
        entry.setChildren(children);
        return entry;
    }

    /**
     * List the files
     *
     * @param file  The file to list files for
     * @param entry the parent entry
     * @return The child files
     */
    private FileEntry[] doListFiles(File file, FileEntry entry) {
        File[] files = listFiles(file);
        FileEntry[] children = files.length > 0 ? new FileEntry[files.length] : FileEntry.EMPTY_ENTRIES;
        for (int i = 0; i < files.length; i++) {
            children[i] = createFileEntry(entry, files[i]);
        }
        return children;
    }

    /**
     * Fire directory/file created events to the registered listeners.
     *
     * @param entry The file entry
     */
    private void doCreate(FileEntry entry) {
        for (FileAlterationListener listener : listeners) {
            if (entry.isDirectory()) {
                listener.onDirectoryCreate(entry.getFile());
            } else {
                listener.onFileCreate(entry.getFile());
            }
        }
        FileEntry[] children = entry.getChildren();
        for (FileEntry aChildren : children) {
            doCreate(aChildren);
        }
    }

    /**
     * Fire directory/file change events to the registered listeners.
     *
     * @param entry The previous file system entry
     * @param file  The current file
     */
    private void doMatch(FileEntry entry, File file) {
        if (entry.refresh(file)) {
            for (FileAlterationListener listener : listeners) {
                if (entry.isDirectory()) {
                    listener.onDirectoryChange(file);
                } else {
                    listener.onFileChange(file);
                }
            }
        }
    }

    /**
     * Fire directory/file delete events to the registered listeners.
     *
     * @param entry The file entry
     */
    private void doDelete(FileEntry entry) {
        for (FileAlterationListener listener : listeners) {
            if (entry.isDirectory()) {
                listener.onDirectoryDelete(entry.getFile());
            } else {
                listener.onFileDelete(entry.getFile());
            }
        }
    }

    /**
     * List the contents of a directory
     *
     * @param file The file to list the contents of
     * @return the directory contents or a zero length array if
     * the empty or the file is not a directory
     */
    private File[] listFiles(File file) {
        File[] children = null;
        if (file.isDirectory()) {
            children = fileFilter == null ? file.listFiles() : file.listFiles(fileFilter);
        }
        if (children == null) {
            children = new File[0];
        }
        if (comparator != null && children.length > 1) {
            Arrays.sort(children, comparator);
        }
        return children;
    }

    private int checkCompareTo(Boolean sensitive, String str1, String str2) {
        if (str1 == null || str2 == null) {
            throw new NullPointerException("The strings must not be null");
        }

        if (sensitive) {

        }

        return sensitive ? str1.compareTo(str2) : str1.compareToIgnoreCase(str2);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("[file='");
        builder.append(getDirectory().getPath());
        builder.append('\'');
        if (fileFilter != null) {
            builder.append(", ");
            builder.append(fileFilter.toString());
        }
        builder.append(", listeners=");
        builder.append(listeners.size());
        builder.append("]");
        return builder.toString();
    }

    /**
     * The state of a file or directory, capturing the following {@link File} attributes at a point in time.
     *
     * @author lry
     */
    @Data
    public static class FileEntry implements Serializable {

        private static final long serialVersionUID = -5513681067020300908L;
        public static final FileEntry[] EMPTY_ENTRIES = new FileEntry[0];

        private final FileEntry parent;
        private FileEntry[] children;
        private final File file;
        private String name;
        private boolean exists;
        private boolean directory;
        private long lastModified;
        private long length;

        public FileEntry(File file) {
            this(null, file);
        }

        public FileEntry(FileEntry parent, final File file) {
            if (file == null) {
                throw new IllegalArgumentException("File is missing");
            }
            this.file = file;
            this.parent = parent;
            this.name = file.getName();
        }

        /**
         * Refresh the attributes from the {@link File}, indicating
         * whether the file has changed.
         * <p>
         * This implementation refreshes the <code>name</code>, <code>exists</code>,
         * <code>directory</code>, <code>lastModified</code> and <code>length</code>
         * properties.
         * <p>
         * The <code>exists</code>, <code>directory</code>, <code>lastModified</code>
         * and <code>length</code> properties are compared for changes
         *
         * @param file the file instance to compare to
         * @return {@code true} if the file has changed, otherwise {@code false}
         */
        public boolean refresh(File file) {
            // cache original values
            boolean origExists = exists;
            long origLastModified = lastModified;
            boolean origDirectory = directory;
            long origLength = length;

            // refresh the values
            name = file.getName();
            exists = file.exists();
            directory = exists && file.isDirectory();
            lastModified = exists ? file.lastModified() : 0;
            length = exists && !directory ? file.length() : 0;

            // Return if there are changes
            return exists != origExists || lastModified != origLastModified
                    || directory != origDirectory || length != origLength;
        }

        public FileEntry newChildInstance(File file) {
            return new FileEntry(this, file);
        }

        public int getLevel() {
            return parent == null ? 0 : parent.getLevel() + 1;
        }

        public FileEntry[] getChildren() {
            return children != null ? children : EMPTY_ENTRIES;
        }

    }

}
