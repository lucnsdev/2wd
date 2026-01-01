package lucns.robot2wd.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class Annotator {

    public static abstract class ContentHandler {
        public void onContentRead(String content) {
        }

        public void onContentWrite(boolean success) {
        }
    }

    private String fullPath;
    private ContentHandler callback;
    private Handler handler;

    public Annotator() {
        handler = new Handler(Looper.getMainLooper());
        this.fullPath = App.getContext().getExternalFilesDir(null).getPath();
    }

    public Annotator(String name) {
        this();
        this.fullPath += "/" + name;
    }

    public Annotator(String parent, String name) {
        this();
        this.fullPath += "/" + parent + "/" + name;
    }

    public Annotator(String path, ContentHandler contentHandler) {
        fullPath = path;
        callback = contentHandler;
        handler = new Handler(Looper.getMainLooper());
    }

    public boolean move(Annotator annotator) {
        File file = new File(fullPath);
        return file.renameTo(new File(annotator.getPath()));
    }

    public boolean copy(Annotator annotator) {
        return annotator.setContent(getContent());
    }

    public Annotator[] listAll() {
        File[] files = new File(fullPath).listFiles();
        if (files == null || files.length == 0) return new Annotator[0];
        List<Annotator> list = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                Annotator a = new Annotator();
                a.setFullPath(file.getPath());
                list.add(a);
            }
        }
        return list.toArray(new Annotator[0]);
    }

    public void changeFileName(String fileName) {
        fullPath = fullPath.substring(0, fullPath.lastIndexOf("/") + 1) + fileName;
    }

    public boolean renameTo(String name) {
        File file = new File(fullPath);
        changeFileName(name);
        File file2 = new File(fullPath);
        if (file2.exists() && file2.isFile()) file2.delete();
        return file.renameTo(file2);
    }

    public String getFullName() {
        return fullPath.substring(fullPath.lastIndexOf("/") + 1);
    }

    public String getName() {
        String s = getFullName();
        return s.substring(0, s.lastIndexOf("."));
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public void setContentHandler(ContentHandler contentHandler) {
        callback = contentHandler;
        if (handler == null) handler = new Handler(Looper.getMainLooper());
    }

    public void saveInBackground(String content) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                setContent(content);
            }
        }).start();
    }

    public void requestRead(ContentHandler contentHandler) {
        callback = contentHandler;
        handler = new Handler(Looper.getMainLooper());
        requestRead();
    }

    public void requestWrite(String content, ContentHandler contentHandler) {
        callback = contentHandler;
        handler = new Handler(Looper.getMainLooper());
        requestWrite(content);
    }

    public void requestRead() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String content = getContent();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.onContentRead(content);
                    }
                });
            }
        }).start();
    }

    public void requestWrite(final String content) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean write = setContent(content);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.onContentWrite(write);
                    }
                });
            }
        }).start();
    }

    public void requestAddContent(final String content) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String read = getContent();
                final boolean write = setContent(read.isEmpty() ? content : read + "\n" + content);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.onContentWrite(write);
                    }
                });
            }
        }).start();
    }

    public String getPath() {
        return fullPath;
    }

    public boolean exists() {
        File f =  new File(fullPath);
        return f.exists() && f.isFile();
    }

    public boolean delete() {
        File note = new File(fullPath);
        return note.exists() && note.isFile() && note.delete();
    }

    public boolean deleteIfEmpty() {
        File note = new File(fullPath);
        return note.length() == 0 && note.delete();
    }

    public Annotator[] getAnnotators() {
        String[] names = getFileNames();
        Annotator[] a = new Annotator[names.length];
        for (int i = 0; i < a.length; i++) {
            a[i] = new Annotator();
            a[i].setFullPath(fullPath + "/" + names[i]);
        }
        return a;
    }

    public String[] getFileNames() {
        File[] files = new File(fullPath).listFiles();
        int length = files == null ? 0 : files.length;
        String[] names = new String[length];
        for (int i = 0; i < length; i++) names[i] = files[i].getName();
        return names;
    }

    public boolean addLineIfNotExist(String line) {
        String last = getContent();
        if (last.contains(line)) return false;
        setContent(last + "\n" + line);
        return true;
    }

    public boolean addLinesIfNotExist(String[] lines) {
        String read = getContent();
        StringBuilder content = new StringBuilder();
        if (!read.isEmpty()) content.append(read);
        int start = content.length();
        if (content.length() == 0) {
            for (String line : lines) {
                if (content.length() > 0) content.append("\n");
                content.append(line);
            }
        } else {
            String[] lines2 = content.toString().split("\n");
            for (int i = 0; i < lines2.length; i++) {
                boolean equal = false;
                for (String line : lines) if ((equal = line.equals(lines2[i]))) break;
                if (equal) continue;
                content.append("\n");
                content.append(lines[i]);
            }
        }
        return content.length() > 0 && content.length() != start && setContent(content.toString());
    }

    public boolean addLines(String[] lines) {
        String content2 = getContent();
        StringBuilder content = new StringBuilder();
        int start = content.length();
        if (!content2.isEmpty()) content.append(content2);
        for (String line : lines) {
            if (content.length() > 0) content.append("\n");
            content.append(line);
        }
        return content.length() > 0 && content.length() != start && setContent(content.toString());
    }

    public boolean addLine(String line) {
        String read = getContent();
        StringBuilder content = new StringBuilder();
        if (!read.isEmpty()) content.append(read);
        int start = content.length();
        if (content.length() > 0) content.append("\n");
        content.append(line);
        return content.length() > 0 && content.length() != start && setContent(content.toString());
    }

    public boolean removeLine(String line) {
        String[] lines = getLines();
        StringBuilder content = new StringBuilder();
        for (String read : lines) {
            if (read.equals(line)) continue;
            if (content.length() > 0) content.append("\n");
            content.append(read);
        }
        return setContent(content.toString());
    }

    public boolean setLines(String[] lines) {
        StringBuilder content = new StringBuilder();
        for (String line : lines) {
            if (content.length() == 0) {
                content.append(line);
                continue;
            }
            if (content.length() > 0) content.append("\n");
            content.append(line);
        }
        return setContent(content.toString());

    }

    public String[] getLines() {
        String content = getContent();
        return content.isEmpty() ? new String[0] : content.split("\n");
    }

    public String[] getLines(int linesToRead) {
        String content = getContent(linesToRead);
        return content.isEmpty() ? new String[0] : content.split("\n");
    }

    public String getContent() {
        return getContent(Integer.MAX_VALUE);
    }

    public String getContent(int linesToRead) {
        BufferedReader reader = null;
        String line;
        StringBuilder content = new StringBuilder();

        try {
            reader = new BufferedReader(new FileReader(fullPath));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                count++;
                if (content.length() > 0) content.append("\n");
                content.append(line);
                if (count == linesToRead) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return content.toString();
    }

    public boolean setContent(String content) {
        try {
            if (content == null || content.isEmpty()) {
                Log.e(Annotator.class.getName(), content == null ? "Content is null" : "Content is empty");
                return false;
            }
            File note = new File(fullPath);
            File parent = note.getParentFile();
            if ((!parent.exists() || parent.isFile()) && !parent.mkdirs()) return false;
            if ((!note.exists() || note.isDirectory()) && !note.createNewFile()) return false;
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileWriter writer = new FileWriter(fullPath);
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getSize() {
        File note = new File(fullPath);
        if (note.exists() && note.isFile()) {
            return getSizeAndUnity(note.length());
        } else {
            return "0 byte";
        }
    }

    private String getSizeAndUnity(long size) {
        DecimalFormat df = new DecimalFormat("0.00");

        float kilo = 1024.0f;
        float mega = kilo * kilo;
        float giga = kilo * mega;

        if (size <= 1) return size + " byte";
        else if (size < kilo) return size + " bytes";
        else if (size < mega) return df.format(size / kilo) + " Kb";
        else if (size < giga) return df.format(size / mega) + " Mb";
        else return df.format(size / giga) + " Gb";
    }
}
