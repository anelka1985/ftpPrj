package cho.nico.com.ftpproject;

import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class FtpManager {

    FTPClient ftpClient = null;

    private final String ftpServerIp = "file.njga.gov.cn", ftpUserName = "wbswpic", ftpPwd = "wbsw2018picftp";

    private final int ftpServerPort = 21;

    private boolean isConnect = false;
    private static FtpManager ftpManager;
    ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private LinkedBlockingQueue<Runnable> uploadRunnables = new LinkedBlockingQueue<>();


    private FtpManager() {
        ftpClient = new FTPClient();
    }

    public synchronized static FtpManager getInstance() {
        if (ftpManager == null) {
            ftpManager = new FtpManager();
        }
        return ftpManager;
    }

    // 连接到ftp服务器
    public synchronized boolean connect() throws Exception {
        boolean bol = false;
        if (ftpClient.isConnected()) {//判断是否已登陆
            ftpClient.disconnect();
        }
        ftpClient.setDataTimeout(20000);//设置连接超时时间
        ftpClient.setControlEncoding("utf-8");
        ftpClient.connect(ftpServerIp, ftpServerPort);
        if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            if (ftpClient.login(ftpUserName, ftpPwd)) {
                isConnect = true;
                bol = true;
                startExcute();
//                FtpManager.getInstance().DeleteDirAndFile("/unimas_unpass/111/");
            }
        }
        return bol;
    }

    // 创建文件夹
    public boolean createDirectory(String path) throws Exception {
        boolean bool = false;
        String directory = path.substring(0, path.lastIndexOf("/") + 1);
        int start = 0;
        int end = 0;
        if (directory.startsWith("/")) {
            start = 1;
        }
        end = directory.indexOf("/", start);
        while (true) {
            String subDirectory = directory.substring(start, end);
            if (!ftpClient.changeWorkingDirectory(subDirectory)) {
                ftpClient.makeDirectory(subDirectory);
                ftpClient.changeWorkingDirectory(subDirectory);
                bool = true;
            }
            start = end + 1;
            end = directory.indexOf("/", start);
            if (end == -1) {
                break;
            }
        }
        return bool;
    }

    // 实现上传文件的功能
    public synchronized boolean uploadFile(String localPath, String serverPath)
            throws Exception {
        // 上传文件之前，先判断本地文件是否存在
        File localFile = new File(localPath);
        if (!localFile.exists()) {
            Log.e("test", "本地文件不存在");
            return false;
        }
        Log.e("test", "本地文件存在，名称为：" + localFile.getName());


//        createDirectory(serverPath); // 如果文件夹不存在，创建文件夹
        createDirIfNoExist(serverPath);
        Log.e("test", "服务器文件存放路径：" + serverPath + localFile.getName());
        String fileName = localFile.getName();
        // 如果本地文件存在，服务器文件也在，上传文件，这个方法中也包括了断点上传
        long localSize = localFile.length(); // 本地文件的长度
        FTPFile[] files = ftpClient.listFiles(fileName);
        long serverSize = 0;
        if (files.length == 0) {
            Log.e("test", "服务器文件不存在");
            serverSize = 0;
        } else {
            serverSize = files[0].getSize(); // 服务器文件的长度
        }
        if (localSize <= serverSize) {
            if (ftpClient.deleteFile(fileName)) {
                Log.e("test", "服务器文件存在,删除文件,开始重新上传");
                serverSize = 0;
            }
        }
        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        // 进度
        long step = localSize / 100;
        long process = 0;
        long currentSize = 0;
        // 好了，正式开始上传文件
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setRestartOffset(serverSize);
        raf.seek(serverSize);
        OutputStream output = ftpClient.appendFileStream(fileName);
        byte[] b = new byte[1024];
        int length = 0;
        while ((length = raf.read(b)) != -1) {
            output.write(b, 0, length);
            currentSize = currentSize + length;
            if (currentSize / step != process) {
                process = currentSize / step;
                if (process % 10 == 0) {
                    Log.e("test", "上传进度：" + process);
                }
            }
        }
        output.flush();
        output.close();
        raf.close();
        if (ftpClient.completePendingCommand()) {
            Log.e("test", "文件上传成功");
            return true;
        } else {
            Log.e("test", "文件上传失败");
            return false;
        }
    }


    public boolean isConnect() {
        return isConnect;
    }


    /**
     * 添加任务
     *
     * @param localFile
     */
    public void addTask(String localFile) throws Exception {
        UploadTask uploadTask = new UploadTask(localFile);
        uploadRunnables.add(uploadTask);
        if (!isConnect) {
            connect();
        }

    }

    private void startExcute() throws Exception {

        new Thread(new Runnable() {
            @Override
            public void run() {
                UploadTask uploadTask;
                Log.e("test", "====>startExcute " + "uploadRunnables.size == " + uploadRunnables.size());
                Log.e("test", "====>isConnect " + isConnect);
                while (isConnect) {
                    Log.e("test", "while (isConnect)" );
                    try {
                        uploadTask = (UploadTask) uploadRunnables.take();
                        if (uploadTask != null) {
                            cachedThreadPool.execute(uploadTask);
                        }
                        else
                        {
                            Log.e("test", "uploadTask == null" );
                        }
                    } catch (InterruptedException e) {

                        Log.e("test", "InterruptedException  "+e.getMessage() );
                        e.printStackTrace();
                    }


                }
            }
        }).start();

    }


    public class UploadTask implements Runnable {
        String localPath = "";

        public UploadTask(String path) {
            this.localPath = path;
        }

        @Override
        public void run() {
            try {
                FtpManager.getInstance().uploadFile(localPath, "/unimas_unpass/wifiunion/");
            } catch (Exception e) {
                Log.e("test", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void disConnect() throws Exception {
        ftpClient.disconnect();
        isConnect = false;
    }




    private void createDirIfNoExist(String name) throws IOException {

        if(!ftpClient.changeWorkingDirectory(name))
        {
            ftpClient.makeDirectory(name);
            ftpClient.changeWorkingDirectory(name);
        }

    }


    String name;

    private void DeleteDirAndFile(String name) throws IOException {
        Log.e("test", "当前 目录名称 " + name);
        ftpClient.changeWorkingDirectory(name);
        FTPFile[] files = ftpClient.listFiles(name);

        if (files == null || files.length == 0) {
            Log.e("test", "删除 目录名称 " + name);

            ftpClient.removeDirectory(name);


            ftpClient.changeToParentDirectory();
            String ff = ftpClient.printWorkingDirectory();
            Log.e("test", "删除 目录名称 后父级目录  " + ff);
            if (!ff.equals("/unimas_unpass/111/")) {
                DeleteDirAndFile(ff);
            }

//
//            Log.e("test", "父级 目录名称 " + ff);
//            if (TextUtils.equals(name, ff)) {
//                return;
//            } else {
//                delete(ff);
//            }
        } else {

            for (FTPFile ftpfile : files) {
                if (ftpfile.isFile()) {
//                    ftpClient.deleteFile(ftpfile.getName());
                } else {

//                    ftpfile.
                    Log.e("test", "当前  ftpfile.getName()  " + ftpfile.getName());

                    String currentName = name + File.separator + ftpfile.getName();
                    Log.e("test", "当前  ftpfile.getName()  " + currentName);
                    ftpClient.changeWorkingDirectory(currentName);
//                    ftpfile.getLink();
                    DeleteDirAndFile(currentName);
                }
            }
        }
    }
}
