package com.odin.multimedia.service;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

@WebServlet("/video")
public class VideoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String VIDEO_PATH = "path/to/your/video.mp4";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String range = request.getHeader("Range");
        try (RandomAccessFile file = new RandomAccessFile(VIDEO_PATH, "r")) {
            long fileLength = file.length();
            long start = 0;
            long end = fileLength - 1;

            if (range != null) {
                String[] ranges = range.replace("bytes=", "").split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1) {
                    end = Long.parseLong(ranges[1]);
                }
            }

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            response.setContentType("video/mp4");
            response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", String.valueOf(end - start + 1));

            try (FileInputStream inputStream = new FileInputStream(VIDEO_PATH);
                 OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                long bytesRead;
                inputStream.skip(start);
                while ((bytesRead = inputStream.read(buffer)) != -1 && start < end) {
                    outputStream.write(buffer, 0, (int) Math.min(bytesRead, end - start + 1));
                    start += bytesRead;
                }
            }
        }
    }
}
